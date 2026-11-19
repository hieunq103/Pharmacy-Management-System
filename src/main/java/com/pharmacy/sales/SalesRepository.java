package com.pharmacy.sales;

import static com.pharmacy.common.SqlRows.*;

import com.pharmacy.common.AppConstants;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.SearchFilter;
import com.pharmacy.employee.Employee;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.infra.db.TransactionGuards;
import com.pharmacy.inventory.LotStats;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.entity.InventoryLot;
import com.pharmacy.inventory.entity.InventoryTransaction;
import com.pharmacy.inventory.entity.Medicine;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.CartTotals;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.sales.entity.SalesOrder;
import com.pharmacy.sales.entity.SalesOrderItem;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SalesRepository {

    private static final Logger log = LoggerFactory.getLogger(SalesRepository.class);

    // Lấy danh sách tên tất cả danh mục thuốc cho màn hình bán hàng
    public List<String> getAllCategories() {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            return session
                .createSelectionQuery("SELECT mc.name FROM MedicineCategory mc ORDER BY mc.id ASC", String.class)
                .list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh mục thuốc cho màn hình bán hàng: ", e);
            return Collections.emptyList();
        }
    }

    // Phân trang danh sách thuốc bán kèm số lượng tồn khả dụng (2-step HQL pagination, tránh N+1)
    public PageResult<MedicineInventoryDTO> getMedicinesPage(int page, int size, String keyword, String categoryName) {
        return getMedicinesPage(page, size, keyword, categoryName, null);
    }

    public PageResult<MedicineInventoryDTO> getMedicinesPage(
        int page,
        int size,
        String keyword,
        String categoryName,
        Long cachedTotal
    ) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.openReadOnlySession()) {
            SearchFilter search = SearchFilter.of(keyword, "kw")
                .prefix("m.code")
                .contains("m.name", "m.activeIngredient");
            boolean hasCategory =
                hasText(categoryName) &&
                !"Tất cả".equalsIgnoreCase(categoryName) &&
                !"Tất cả nhóm thuốc".equalsIgnoreCase(categoryName);

            String where =
                " WHERE m.status = :active " + search.hql() + (hasCategory ? " AND m.category.name = :category " : "");

            long total;
            if (cachedTotal != null && cachedTotal >= 0) {
                total = cachedTotal;
            } else {
                SelectionQuery<Long> countQuery = session
                    .createSelectionQuery("SELECT COUNT(m.id) FROM Medicine m " + where, Long.class)
                    .setParameter("active", Medicine.Status.ACTIVE);
                bindMedicineFilter(countQuery, search, hasCategory ? categoryName : null);
                total = toLong(countQuery.uniqueResult());
            }
            if (total == 0) {
                return PageResult.empty(validPage, validSize);
            }

            // Bước 1: Lấy đúng ID các thuốc của trang hiện tại (thuốc còn hàng còn hạn lên trước).
            // Không dùng ORDER BY CASE WHEN EXISTS(...) (subquery tương quan chạy cho mọi dòng + filesort toàn bảng mỗi lần
            // chuyển trang). Thay bằng 2 nhóm: nhóm có hàng (semi-join theo index lô) rồi tới nhóm còn lại, ghép theo offset.
            LocalDate today = LocalDate.now();
            String sellable =
                " AND m.id IN (SELECT il.medicine.id FROM InventoryLot il " +
                "WHERE il.quantityRemaining > 0 AND il.expiryDate > :today) ";
            String idsOrder = " ORDER BY m.name ASC, m.id ASC";
            int offset = (validPage - 1) * validSize;

            SelectionQuery<Long> sellableCount = session
                .createSelectionQuery("SELECT COUNT(m.id) FROM Medicine m " + where + sellable, Long.class)
                .setParameter("active", Medicine.Status.ACTIVE)
                .setParameter("today", today);
            bindMedicineFilter(sellableCount, search, hasCategory ? categoryName : null);
            long sellableTotal = toLong(sellableCount.uniqueResult());

            List<Integer> pageIds = new ArrayList<>(validSize);
            if (offset < sellableTotal) {
                SelectionQuery<Integer> q = session
                    .createSelectionQuery("SELECT m.id FROM Medicine m " + where + sellable + idsOrder, Integer.class)
                    .setParameter("active", Medicine.Status.ACTIVE)
                    .setParameter("today", today)
                    .setFirstResult(offset)
                    .setMaxResults(validSize);
                bindMedicineFilter(q, search, hasCategory ? categoryName : null);
                pageIds.addAll(q.list());
            }
            int remaining = validSize - pageIds.size();
            if (remaining > 0) {
                SelectionQuery<Integer> q = session
                    .createSelectionQuery(
                        "SELECT m.id FROM Medicine m " +
                            where +
                            " AND NOT EXISTS (SELECT 1 FROM InventoryLot il WHERE il.medicine.id = m.id " +
                            "AND il.quantityRemaining > 0 AND il.expiryDate > :today) " +
                            idsOrder,
                        Integer.class
                    )
                    .setParameter("active", Medicine.Status.ACTIVE)
                    .setParameter("today", today)
                    .setFirstResult((int) Math.max(0, offset - sellableTotal))
                    .setMaxResults(remaining);
                bindMedicineFilter(q, search, hasCategory ? categoryName : null);
                pageIds.addAll(q.list());
            }
            if (pageIds.isEmpty()) {
                return PageResult.empty(validPage, validSize);
            }

            // Bước 2: Lấy thông tin thuốc + thống kê lô chỉ cho đúng pageIds
            String detailHql = """
            SELECT m.id, m.code, m.name, mc.name, m.requiresPrescription, m.unit, m.sellPrice,
                   m.activeIngredient, m.manufacturer
            FROM Medicine m
            LEFT JOIN m.category mc
            WHERE m.id IN (:ids)
            """;
            List<Object[]> rows = session
                .createSelectionQuery(detailHql, Object[].class)
                .setParameter("ids", pageIds)
                .list();
            Map<Integer, Object[]> rowById = new HashMap<>(rows.size() * 2);
            for (Object[] r : rows) {
                rowById.put(toInteger(r[0]), r);
            }
            Map<Integer, LotStats> statsById = LotStats.load(session, pageIds);

            List<MedicineInventoryDTO> list = new ArrayList<>(pageIds.size());
            for (Integer id : pageIds) {
                Object[] r = rowById.get(id);
                if (r != null) {
                    list.add(mapSalesMedicineRow(r, statsById.getOrDefault(id, LotStats.EMPTY)));
                }
            }
            return new PageResult<>(list, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang thuốc cho bán hàng: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    // Gán tham số tìm kiếm từ khóa và danh mục cho truy vấn thuốc bán
    private static void bindMedicineFilter(SelectionQuery<?> query, SearchFilter search, String category) {
        search.bind(query);
        if (category != null) query.setParameter("category", category.trim());
    }

    // Ánh xạ dòng kết quả truy vấn HQL sang DTO thuốc bán
    private static MedicineInventoryDTO mapSalesMedicineRow(Object[] row, LotStats stats) {
        int sellableQty = stats.sellableQty();
        MedicineInventoryDTO.InventoryStatus status = MedicineInventoryDTO.InventoryStatus.NORMAL;
        if (sellableQty == 0) {
            status = MedicineInventoryDTO.InventoryStatus.OUT_OF_STOCK;
        } else if (stats.expiredCount() > 0) {
            status = MedicineInventoryDTO.InventoryStatus.HAS_EXPIRED_LOT;
        } else if (stats.nearExpiredCount() > 0) {
            status = MedicineInventoryDTO.InventoryStatus.HAS_NEAR_EXPIRY_LOT;
        }

        MedicineInventoryDTO dto = new MedicineInventoryDTO(
            toInteger(row[0]),
            toStr(row[1]),
            toStr(row[2]),
            toStr(row[3]),
            toBoolean(row[4]),
            toStr(row[5]),
            toBigDecimal(row[6]),
            sellableQty,
            status
        );
        dto.setActiveIngredient(toStr(row[7]));
        dto.setManufacturer(toStr(row[8]));
        return dto;
    }

    // Sinh mã hóa đơn tiếp theo theo định dạng HDB-CH01-yyMMdd-HHmmss-xx
    public String generateNextOrderCode() {
        String store = com.pharmacy.sales.payment.PaymentConfig.getStoreCode();
        String storeClean = hasText(store) ? store.trim().toUpperCase().replaceAll("[^A-Z0-9]", "") : "CH01";
        if (storeClean.length() > 6) {
            storeClean = storeClean.substring(0, 6);
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmmss"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(10, 100);
        return String.format("HDB-%s-%s-%02d", storeClean, timestamp, randomSuffix);
    }

    // Tạo mới đơn bán hàng, khóa lô bằng HQL PESSIMISTIC_WRITE, phân bổ FEFO và cập nhật tồn kho
    public String createSalesOrder(SalesOrderRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng rỗng, không có sản phẩm nào để thanh toán.");
        }
        if (req.getEmployeeId() == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên lập hóa đơn.");
        }
        String orderCode = hasText(req.getOrderCode()) ? req.getOrderCode().trim() : generateNextOrderCode();

        try {
            HibernateUtil.execute(session -> {
                TransactionGuards.requireActiveEmployee(session, req.getEmployeeId());

                Map<Integer, Integer> qtyByMedicine = new LinkedHashMap<>();
                Map<Integer, CartItem> cartByMedicine = new HashMap<>();
                for (CartItem item : req.getItems()) {
                    if (item.getMedicineId() == null || item.getQuantity() <= 0) {
                        throw new IllegalArgumentException(
                            "Số lượng của thuốc '" + item.getMedicineName() + "' phải lớn hơn 0."
                        );
                    }
                    qtyByMedicine.merge(item.getMedicineId(), item.getQuantity(), Integer::sum);
                    cartByMedicine.putIfAbsent(item.getMedicineId(), item);
                }

                Map<Integer, BigDecimal> prices = loadActivePrices(session, qtyByMedicine.keySet());
                Map<Integer, List<InventoryLot>> lotsByMedicine = lockSellableLots(session, qtyByMedicine.keySet());

                record OrderLinePlan(
                    int medicineId,
                    InventoryLot lot,
                    int quantity,
                    BigDecimal unitPrice,
                    BigDecimal lineTotal
                ) {}
                BigDecimal subtotal = BigDecimal.ZERO;
                List<OrderLinePlan> lines = new ArrayList<>();

                for (Map.Entry<Integer, Integer> entry : qtyByMedicine.entrySet()) {
                    int medicineId = entry.getKey();
                    int needed = entry.getValue();
                    CartItem cart = cartByMedicine.get(medicineId);
                    BigDecimal unitPrice = prices.get(medicineId);
                    if (unitPrice == null) {
                        throw new IllegalStateException(
                            "Thuốc '" + cart.getMedicineName() + "' không tồn tại hoặc đã ngừng kinh doanh."
                        );
                    }
                    if (cart.getSellPrice() != null && cart.getSellPrice().compareTo(unitPrice) != 0) {
                        throw new IllegalStateException(
                            "Giá bán của thuốc '" +
                                cart.getMedicineName() +
                                "' đã thay đổi. Vui lòng tải lại giỏ hàng trước khi thanh toán."
                        );
                    }

                    List<InventoryLot> lots = lotsByMedicine.getOrDefault(medicineId, List.of());
                    long available = lots.stream().mapToLong(InventoryLot::getQuantityRemaining).sum();
                    if (available < needed) {
                        throw new IllegalStateException(
                            "Thuốc '" +
                                cart.getMedicineName() +
                                "' không đủ số lượng tồn còn hạn để bán (Còn " +
                                available +
                                ", yêu cầu " +
                                needed +
                                ")."
                        );
                    }
                    for (InventoryLot lot : lots) {
                        if (needed <= 0) break;
                        int deduct = Math.min(lot.getQuantityRemaining(), needed);
                        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(deduct));
                        lines.add(new OrderLinePlan(medicineId, lot, deduct, unitPrice, lineTotal));
                        subtotal = subtotal.add(lineTotal);
                        needed -= deduct;
                    }
                }

                BigDecimal discount = CartTotals.clampDiscount(req.getDiscount(), subtotal);
                BigDecimal totalAmount = CartTotals.total(subtotal, discount);
                String phone = hasText(req.getCustomerPhone()) ? req.getCustomerPhone().trim() : null;
                Employee empRef = session.getReference(Employee.class, req.getEmployeeId());

                SalesOrder order = new SalesOrder();
                order.setCode(orderCode);
                order.setEmployee(empRef);
                order.setCustomerPhone(phone);
                order.setTotalAmount(totalAmount);
                order.setDiscount(discount);
                order.setPaymentMethod(SalesOrder.PaymentMethod.valueOf(req.getPaymentMethod().trim().toUpperCase()));
                session.persist(order);

                for (OrderLinePlan line : lines) {
                    InventoryLot lot = line.lot();
                    lot.setQuantityRemaining(lot.getQuantityRemaining() - line.quantity());

                    SalesOrderItem orderItem = new SalesOrderItem();
                    orderItem.setSalesOrder(order);
                    orderItem.setMedicine(session.getReference(Medicine.class, line.medicineId()));
                    orderItem.setInventoryLot(lot);
                    orderItem.setQuantity(line.quantity());
                    orderItem.setUnitPrice(line.unitPrice());
                    orderItem.setLineTotal(line.lineTotal());
                    session.persist(orderItem);
                }

                InventoryTransaction tx = new InventoryTransaction();
                tx.setType(InventoryTransaction.Type.EXPORT_SALE);
                tx.setSalesOrder(order);
                tx.setEmployee(empRef);
                tx.setNote("Bán lẻ tại quầy - Mã đơn " + orderCode);
                session.persist(tx);
            });
            return orderCode;
        } catch (Exception e) {
            log.error("Lỗi giao dịch thanh toán đơn hàng: ", e);
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            String msg = root.getMessage() != null ? root.getMessage() : "Lỗi khi xử lý thanh toán đơn hàng.";
            throw new RuntimeException(msg, e);
        }
    }

    // Lấy giá bán hiện tại của các thuốc đang kinh doanh (PESSIMISTIC_WRITE = FOR UPDATE; không dùng LOCK IN SHARE MODE vì TiDB không hỗ trợ)
    private static Map<Integer, BigDecimal> loadActivePrices(Session session, Set<Integer> medicineIds) {
        List<Object[]> rows = session
            .createSelectionQuery(
                """
                SELECT m.id, m.sellPrice
                FROM Medicine m
                WHERE m.status = :active AND m.id IN (:ids)
                """,
                Object[].class
            )
            .setParameter("active", Medicine.Status.ACTIVE)
            .setParameter("ids", medicineIds)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .list();
        Map<Integer, BigDecimal> prices = new HashMap<>(rows.size() * 2);
        for (Object[] r : rows) {
            prices.put(toInteger(r[0]), toBigDecimal(r[1]));
        }
        return prices;
    }

    // Khóa và lấy danh sách lô thuốc còn hạn theo thứ tự ưu tiên FEFO (PESSIMISTIC_WRITE = FOR UPDATE)
    private static Map<Integer, List<InventoryLot>> lockSellableLots(Session session, Set<Integer> medicineIds) {
        List<InventoryLot> lots = session
            .createSelectionQuery(
                """
                SELECT il
                FROM InventoryLot il
                WHERE il.medicine.id IN (:ids) AND il.quantityRemaining > 0 AND il.expiryDate > :today
                ORDER BY il.medicine.id ASC, il.expiryDate ASC, il.id ASC
                """,
                InventoryLot.class
            )
            .setParameter("ids", medicineIds)
            .setParameter("today", LocalDate.now())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .list();
        Map<Integer, List<InventoryLot>> result = new HashMap<>();
        for (InventoryLot lot : lots) {
            Integer medId = lot.getMedicine().getId();
            result.computeIfAbsent(medId, k -> new ArrayList<>()).add(lot);
        }
        return result;
    }

    // Phân trang danh sách hóa đơn bán hàng theo khoảng ngày và từ khóa (2-step HQL pagination)
    public PageResult<SalesOrderDTO> getSalesOrders(
        LocalDate fromDate,
        LocalDate toDate,
        Integer employeeId,
        String keyword,
        int page,
        int size
    ) {
        if (fromDate == null) fromDate = LocalDate.now();
        if (toDate == null || toDate.isBefore(fromDate)) toDate = fromDate;
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);

        try (Session session = HibernateUtil.openReadOnlySession()) {
            // Mỗi token khớp mã đơn / SĐT khách / tên nhân viên lập / tên-mã thuốc trong đơn
            SearchFilter search = SearchFilter.of(keyword, "kw")
                .contains("so.code", "so.customerPhone")
                .custom("so.employee.id IN (SELECT eS.id FROM Employee eS WHERE eS.fullName LIKE {p} ESCAPE '!')")
                .custom(
                    "EXISTS (SELECT 1 FROM SalesOrderItem soiS JOIN soiS.medicine mS WHERE soiS.salesOrder.id = so.id" +
                        " AND (mS.name LIKE {p} ESCAPE '!' OR mS.code LIKE {p} ESCAPE '!'))"
                );

            StringBuilder where = new StringBuilder(" WHERE so.createdAt >= :fromTs AND so.createdAt < :toTs ");
            if (employeeId != null) where.append(" AND so.employee.id = :empId ");
            where.append(search.hql());

            SelectionQuery<Long> idsQuery = session
                .createSelectionQuery(
                    "SELECT so.id FROM SalesOrder so " + where + " ORDER BY so.createdAt DESC, so.id DESC",
                    Long.class
                )
                .setFirstResult((validPage - 1) * validSize)
                .setMaxResults(validSize);
            bindOrderFilter(idsQuery, fromDate, toDate, employeeId, search);
            List<Long> pageIds = idsQuery.list();

            long totalRecords;
            if (pageIds.size() < validSize && (validPage == 1 || !pageIds.isEmpty())) {
                // Trang chưa đầy: biết ngay tổng số dòng, không cần COUNT
                totalRecords = (long) (validPage - 1) * validSize + pageIds.size();
            } else {
                SelectionQuery<Long> countQuery = session.createSelectionQuery(
                    "SELECT COUNT(so.id) FROM SalesOrder so " + where,
                    Long.class
                );
                bindOrderFilter(countQuery, fromDate, toDate, employeeId, search);
                totalRecords = toLong(countQuery.uniqueResult());
            }
            if (totalRecords == 0) {
                return new PageResult<>(Collections.emptyList(), 0L, validPage, validSize);
            }

            List<SalesOrderDTO> list = loadOrderSummariesByIds(session, pageIds);
            return new PageResult<>(list, totalRecords, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn lịch sử bán hàng: ", e);
            return new PageResult<>(Collections.emptyList(), 0L, validPage, validSize);
        }
    }

    // Nạp chi tiết tổng hợp cho danh sách ID hóa đơn trong trang hiện tại (2 truy vấn HQL theo index)
    private static List<SalesOrderDTO> loadOrderSummariesByIds(Session session, List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object[]> headerRows = session
            .createSelectionQuery(
                """
                SELECT so.id, so.code, so.createdAt, e.id, e.fullName,
                       so.customerPhone, so.discount, so.totalAmount, so.paymentMethod
                FROM SalesOrder so
                LEFT JOIN so.employee e
                WHERE so.id IN (:ids)
                ORDER BY so.createdAt DESC, so.id DESC
                """,
                Object[].class
            )
            .setParameter("ids", orderIds)
            .list();

        List<Object[]> aggRows = session
            .createSelectionQuery(
                """
                SELECT soi.salesOrder.id, COUNT(DISTINCT soi.medicine.id),
                       COALESCE(SUM(soi.quantity), 0), COALESCE(SUM(soi.lineTotal), 0)
                FROM SalesOrderItem soi
                WHERE soi.salesOrder.id IN (:ids)
                GROUP BY soi.salesOrder.id
                """,
                Object[].class
            )
            .setParameter("ids", orderIds)
            .list();
        Map<Long, Object[]> aggByOrderId = new HashMap<>(aggRows.size() * 2);
        for (Object[] a : aggRows) {
            aggByOrderId.put(toLongObj(a[0]), a);
        }

        List<SalesOrderDTO> list = new ArrayList<>(headerRows.size());
        for (Object[] r : headerRows) {
            Long id = toLongObj(r[0]);
            Object[] agg = aggByOrderId.get(id);
            int itemCount = agg != null ? toInt(agg[1]) : 0;
            int totalQty = agg != null ? toInt(agg[2]) : 0;
            BigDecimal subtotal = agg != null ? toBigDecimal(agg[3]) : BigDecimal.ZERO;
            String paymentMethod = r[8] != null ? r[8].toString() : null;
            list.add(
                new SalesOrderDTO(
                    id,
                    toStr(r[1]),
                    toLocalDateTime(r[2]),
                    toStr(r[4]),
                    null,
                    toStr(r[5]),
                    itemCount,
                    totalQty,
                    subtotal,
                    toBigDecimal(r[6]),
                    toBigDecimal(r[7]),
                    paymentMethod
                )
            );
        }
        return list;
    }

    // Gán tham số bộ lọc cho truy vấn danh sách hóa đơn
    private static void bindOrderFilter(
        SelectionQuery<?> query,
        LocalDate fromDate,
        LocalDate toDate,
        Integer employeeId,
        SearchFilter search
    ) {
        query.setParameter("fromTs", fromDate.atStartOfDay());
        query.setParameter("toTs", toDate.plusDays(1).atStartOfDay());
        if (employeeId != null) query.setParameter("empId", employeeId);
        search.bind(query);
    }

    // Lấy danh sách chi tiết các mặt hàng của một hóa đơn
    public List<SalesOrderDetailDTO> getSalesOrderItems(long orderId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String hql = """
                SELECT soi.id, m.id, m.code, m.name, mc.name, m.activeIngredient, m.unit,
                       il.lotNumber, il.expiryDate, soi.quantity, soi.unitPrice, soi.lineTotal
                FROM SalesOrderItem soi
                JOIN soi.medicine m
                LEFT JOIN m.category mc
                LEFT JOIN soi.inventoryLot il
                WHERE soi.salesOrder.id = :orderId
                ORDER BY soi.id ASC
            """;
            List<Object[]> rows = session
                .createSelectionQuery(hql, Object[].class)
                .setParameter("orderId", orderId)
                .list();
            List<SalesOrderDetailDTO> list = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                list.add(
                    new SalesOrderDetailDTO(
                        toStr(r[2]),
                        toStr(r[3]),
                        toStr(r[4]),
                        toStr(r[5]),
                        toStr(r[6]),
                        toStr(r[7]),
                        toLocalDate(r[8]),
                        toInt(r[9]),
                        toBigDecimal(r[10]),
                        toBigDecimal(r[11])
                    )
                );
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết mặt hàng đơn bán {}: ", orderId, e);
            return Collections.emptyList();
        }
    }

    // Lấy danh sách các đơn bán hàng gần nhất cho Dashboard
    public List<SalesOrderDTO> getRecentSalesOrders(int limit, Integer employeeId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String idsHql =
                "SELECT so.id FROM SalesOrder so" +
                (employeeId != null ? " WHERE so.employee.id = :employeeId" : "") +
                " ORDER BY so.createdAt DESC, so.id DESC";
            SelectionQuery<Long> idsQuery = session
                .createSelectionQuery(idsHql, Long.class)
                .setMaxResults(Math.max(1, limit));
            if (employeeId != null) {
                idsQuery.setParameter("employeeId", employeeId);
            }
            return loadOrderSummariesByIds(session, idsQuery.list());
        } catch (Exception e) {
            log.error("Lỗi khi lấy giao dịch gần đây trong SalesRepository: ", e);
            return Collections.emptyList();
        }
    }
}
