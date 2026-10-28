package com.pharmacy.sales;

import com.pharmacy.common.PageResult;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.CartTotals;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.sales.dto.TodaySalesSummary;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.pharmacy.common.SqlRows.*;
import static com.pharmacy.inventory.InventorySql.*;

public class SalesRepository {
    private static final Logger log = LoggerFactory.getLogger(SalesRepository.class);

    private static final String ORDER_SUMMARY_SELECT = """
            SELECT so.id, so.code, so.created_at, so.employee_id, e.full_name AS employee_name,
                   so.customer_phone, agg.item_count, agg.total_qty, agg.subtotal,
                   so.discount, so.total_amount, so.payment_method
            FROM (%s) page_ids
            JOIN sales_orders so ON so.id = page_ids.id
            LEFT JOIN employees e ON so.employee_id = e.id
            LEFT JOIN LATERAL (
                SELECT COUNT(DISTINCT soi.medicine_id) AS item_count,
                       COALESCE(SUM(soi.quantity), 0) AS total_qty,
                       COALESCE(SUM(soi.line_total), 0) AS subtotal
                FROM sales_order_items soi
                WHERE soi.sales_order_id = so.id
            ) agg ON TRUE
            ORDER BY so.created_at DESC, so.id DESC
            """;

    // Lấy danh sách tên tất cả danh mục thuốc cho màn hình bán hàng
    public List<String> getAllCategories() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery("SELECT name FROM medicine_categories ORDER BY id ASC", String.class).list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh mục thuốc cho màn hình bán hàng: ", e);
            return Collections.emptyList();
        }
    }

    // Phân trang danh sách thuốc bán kèm số lượng tồn khả dụng
    public PageResult<MedicineInventoryDTO> getMedicinesPage(int page, int size, String keyword, String categoryName) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = hasText(keyword);
            boolean hasCategory = hasText(categoryName)
                    && !"Tất cả".equalsIgnoreCase(categoryName)
                    && !"Tất cả nhóm thuốc".equalsIgnoreCase(categoryName);

            String where = " WHERE m.status = 'ACTIVE' "
                    + (hasKeyword ? KEYWORD_FILTER : "")
                    + (hasCategory ? " AND m.category_id = (SELECT id FROM medicine_categories WHERE name = :category) " : "");

            var countQuery = session.createNativeQuery("SELECT COUNT(*) FROM medicines m " + where, Long.class);
            bindMedicineFilter(countQuery, hasKeyword ? keyword : null, hasCategory ? categoryName : null);
            long total = toLong(countQuery.uniqueResult());
            if (total == 0) {
                return PageResult.empty(validPage, validSize);
            }

            String pageIdsSql = """
                SELECT m.id FROM medicines m %s
                ORDER BY NOT EXISTS (
                    SELECT 1 FROM inventory_lots il
                    WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE()
                ), m.name ASC, m.id ASC
                LIMIT :limit OFFSET :offset
            """.formatted(where);
            String dataSql = """
                SELECT %s,
                       COALESCE(ls.total_qty, 0) - COALESCE(ls.expired_qty, 0) AS sellable_qty
                FROM (%s) page_ids
                JOIN medicines m ON m.id = page_ids.id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                %s
                ORDER BY (COALESCE(ls.total_qty, 0) - COALESCE(ls.expired_qty, 0)) = 0, m.name ASC, m.id ASC
            """.formatted(INVENTORY_COLUMNS, pageIdsSql, LOT_STATS_LATERAL);

            NativeQuery<Object[]> query = session.createNativeQuery(dataSql, Object[].class);
            bindMedicineFilter(query, hasKeyword ? keyword : null, hasCategory ? categoryName : null);
            query.setParameter("limit", validSize);
            query.setParameter("offset", (validPage - 1) * validSize);

            List<MedicineInventoryDTO> list = new ArrayList<>();
            for (Object[] row : query.list()) {
                list.add(mapSalesMedicineRow(row));
            }
            return new PageResult<>(list, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang thuốc cho bán hàng: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    // Gán tham số tìm kiếm từ khóa và danh mục cho truy vấn thuốc bán
    private static void bindMedicineFilter(NativeQuery<?> query, String keyword, String category) {
        if (keyword != null) query.setParameter("keyword", likeParam(keyword));
        if (category != null) query.setParameter("category", category.trim());
    }

    // Ánh xạ dòng kết quả truy vấn sang DTO thuốc bán
    private static MedicineInventoryDTO mapSalesMedicineRow(Object[] row) {
        int sellableQty = Math.max(0, toInt(row[13]));
        int expiredCount = toInt(row[10]);
        int nearExpiredCount = toInt(row[11]);

        MedicineInventoryDTO.InventoryStatus status = MedicineInventoryDTO.InventoryStatus.NORMAL;
        if (sellableQty == 0) {
            status = MedicineInventoryDTO.InventoryStatus.OUT_OF_STOCK;
        } else if (expiredCount > 0) {
            status = MedicineInventoryDTO.InventoryStatus.HAS_EXPIRED_LOT;
        } else if (nearExpiredCount > 0) {
            status = MedicineInventoryDTO.InventoryStatus.HAS_NEAR_EXPIRY_LOT;
        }

        MedicineInventoryDTO dto = new MedicineInventoryDTO(
                toInteger(row[0]), toStr(row[1]), toStr(row[2]), toStr(row[3]), toBoolean(row[4]), toStr(row[5]),
                toBigDecimal(row[6]), sellableQty, status);
        dto.setActiveIngredient(toStr(row[7]));
        dto.setManufacturer(toStr(row[8]));
        return dto;
    }

    // Sinh mã hóa đơn tiếp theo theo định dạng HDB-yyMMdd-xxx
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

    // Tạo mới đơn bán hàng, phân bổ lô thuốc theo FEFO và cập nhật tồn kho
    public String createSalesOrder(SalesOrderRequest req) {
        if (req == null || req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng rỗng, không có sản phẩm nào để thanh toán.");
        }
        if (req.getEmployeeId() == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên lập hóa đơn.");
        }
        String orderCode = hasText(req.getOrderCode()) ? req.getOrderCode().trim() : generateNextOrderCode();

        try {
            HibernateUtil.execute(session -> session.doWork(conn -> {
                Map<Integer, Integer> qtyByMedicine = new LinkedHashMap<>();
                Map<Integer, CartItem> cartByMedicine = new HashMap<>();
                for (CartItem item : req.getItems()) {
                    if (item.getMedicineId() == null || item.getQuantity() <= 0) {
                        throw new IllegalArgumentException("Số lượng của thuốc '" + item.getMedicineName() + "' phải lớn hơn 0.");
                    }
                    qtyByMedicine.merge(item.getMedicineId(), item.getQuantity(), Integer::sum);
                    cartByMedicine.putIfAbsent(item.getMedicineId(), item);
                }

                Map<Integer, BigDecimal> prices = loadActivePrices(conn, qtyByMedicine.keySet());
                Map<Integer, List<long[]>> lotsByMedicine = lockSellableLots(conn, qtyByMedicine.keySet());

                BigDecimal subtotal = BigDecimal.ZERO;
                List<Object[]> lines = new ArrayList<>();
                for (Map.Entry<Integer, Integer> entry : qtyByMedicine.entrySet()) {
                    int medicineId = entry.getKey();
                    int needed = entry.getValue();
                    CartItem cart = cartByMedicine.get(medicineId);
                    BigDecimal unitPrice = prices.get(medicineId);
                    if (unitPrice == null) {
                        throw new IllegalStateException("Thuốc '" + cart.getMedicineName() + "' không tồn tại hoặc đã ngừng kinh doanh.");
                    }
                    if (cart.getSellPrice() != null && cart.getSellPrice().compareTo(unitPrice) != 0) {
                        throw new IllegalStateException("Giá bán của thuốc '" + cart.getMedicineName()
                                + "' đã thay đổi. Vui lòng tải lại giỏ hàng trước khi thanh toán.");
                    }

                    List<long[]> lots = lotsByMedicine.getOrDefault(medicineId, List.of());
                    long available = lots.stream().mapToLong(l -> l[1]).sum();
                    if (available < needed) {
                        throw new IllegalStateException("Thuốc '" + cart.getMedicineName() + "' không đủ số lượng tồn còn hạn để bán (Còn "
                                + available + ", yêu cầu " + needed + ").");
                    }
                    for (long[] lot : lots) {
                        if (needed <= 0) break;
                        int deduct = (int) Math.min(lot[1], needed);
                        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(deduct));
                        lines.add(new Object[]{medicineId, lot[0], deduct, unitPrice, lineTotal});
                        subtotal = subtotal.add(lineTotal);
                        needed -= deduct;
                    }
                }

                BigDecimal discount = CartTotals.clampDiscount(req.getDiscount(), subtotal);
                BigDecimal totalAmount = CartTotals.total(subtotal, discount);
                String phone = hasText(req.getCustomerPhone()) ? req.getCustomerPhone().trim() : null;

                long orderId;
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO sales_orders (code, employee_id, customer_phone, total_amount, discount, payment_method, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, NOW())
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, orderCode);
                    ps.setInt(2, req.getEmployeeId());
                    ps.setString(3, phone);
                    ps.setBigDecimal(4, totalAmount);
                    ps.setBigDecimal(5, discount);
                    ps.setString(6, req.getPaymentMethod());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getLong(1);
                    }
                }

                try (PreparedStatement itemPs = conn.prepareStatement("""
                        INSERT INTO sales_order_items (sales_order_id, medicine_id, inventory_lot_id, quantity, unit_price, line_total)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """);
                     PreparedStatement lotPs = conn.prepareStatement(
                             "UPDATE inventory_lots SET quantity_remaining = quantity_remaining - ? WHERE id = ? AND quantity_remaining >= ?")) {
                    for (Object[] line : lines) {
                        itemPs.setLong(1, orderId);
                        itemPs.setInt(2, (Integer) line[0]);
                        itemPs.setLong(3, (Long) line[1]);
                        itemPs.setInt(4, (Integer) line[2]);
                        itemPs.setBigDecimal(5, (BigDecimal) line[3]);
                        itemPs.setBigDecimal(6, (BigDecimal) line[4]);
                        itemPs.addBatch();

                        lotPs.setInt(1, (Integer) line[2]);
                        lotPs.setLong(2, (Long) line[1]);
                        lotPs.setInt(3, (Integer) line[2]);
                        lotPs.addBatch();
                    }
                    itemPs.executeBatch();
                    for (int count : lotPs.executeBatch()) {
                        if (count == 0) {
                            throw new IllegalStateException("Tồn kho vừa thay đổi, vui lòng thử lại.");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO inventory_transactions (type, sales_order_id, employee_id, transaction_at, note)
                        VALUES ('EXPORT_SALE', ?, ?, NOW(), ?)
                        """)) {
                    ps.setLong(1, orderId);
                    ps.setInt(2, req.getEmployeeId());
                    ps.setString(3, "Bán lẻ tại quầy - Mã đơn " + orderCode);
                    ps.executeUpdate();
                }
            }));
            return orderCode;
        } catch (Exception e) {
            log.error("Lỗi giao dịch thanh toán đơn hàng: ", e);
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) root = root.getCause();
            String msg = root.getMessage() != null ? root.getMessage() : "Lỗi khi xử lý thanh toán đơn hàng.";
            throw new RuntimeException(msg, e);
        }
    }

    // Tạo chuỗi dấu hỏi chấm cho mệnh đề IN trong SQL
    private static String placeholders(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

    // Lấy giá bán hiện tại của các thuốc đang kinh doanh
    private static Map<Integer, BigDecimal> loadActivePrices(Connection conn, java.util.Set<Integer> medicineIds) throws SQLException {
        Map<Integer, BigDecimal> prices = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, sell_price FROM medicines WHERE status = 'ACTIVE' AND id IN ("
                + placeholders(medicineIds.size()) + ") FOR SHARE")) {
            int i = 1;
            for (Integer id : medicineIds) ps.setInt(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) prices.put(rs.getInt(1), rs.getBigDecimal(2));
            }
        }
        return prices;
    }

    // Khóa và lấy danh sách lô thuốc còn hạn theo thứ tự ưu tiên FEFO
    private static Map<Integer, List<long[]>> lockSellableLots(Connection conn, java.util.Set<Integer> medicineIds) throws SQLException {
        Map<Integer, List<long[]>> result = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT id, medicine_id, quantity_remaining
                FROM inventory_lots
                WHERE medicine_id IN (%s) AND quantity_remaining > 0 AND expiry_date > CURDATE()
                ORDER BY medicine_id, expiry_date ASC, id ASC
                FOR UPDATE
                """.formatted(placeholders(medicineIds.size())))) {
            int i = 1;
            for (Integer id : medicineIds) ps.setInt(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.computeIfAbsent(rs.getInt(2), k -> new ArrayList<>())
                            .add(new long[]{rs.getLong(1), rs.getInt(3)});
                }
            }
        }
        return result;
    }

    // Lấy danh sách hóa đơn bán hàng theo một ngày cụ thể
    public PageResult<SalesOrderDTO> getSalesOrders(LocalDate date, Integer employeeId, String keyword, int page, int size) {
        if (date == null) date = LocalDate.now();
        return getSalesOrders(date, date, employeeId, keyword, page, size);
    }

    // Phân trang danh sách hóa đơn bán hàng theo khoảng ngày và từ khóa
    public PageResult<SalesOrderDTO> getSalesOrders(LocalDate fromDate, LocalDate toDate, Integer employeeId, String keyword,
                                                    int page, int size) {
        if (fromDate == null) fromDate = LocalDate.now();
        if (toDate == null || toDate.isBefore(fromDate)) toDate = fromDate;
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = hasText(keyword);

            StringBuilder where = new StringBuilder(" WHERE so.created_at >= :fromTs AND so.created_at < :toTs ");
            if (employeeId != null) where.append(" AND so.employee_id = :empId ");
            if (hasKeyword) {
                where.append(" AND (so.code LIKE :kw OR so.customer_phone LIKE :kw OR so.employee_id IN (SELECT id FROM employees WHERE full_name LIKE :kw)) ");
            }

            var countQuery = session.createNativeQuery("SELECT COUNT(*) FROM sales_orders so " + where, Long.class);
            bindOrderFilter(countQuery, fromDate, toDate, employeeId, hasKeyword ? keyword : null);
            long totalRecords = toLong(countQuery.uniqueResult());
            if (totalRecords == 0) {
                return new PageResult<>(Collections.emptyList(), 0L, validPage, validSize);
            }

            String pageIds = "SELECT so.id FROM sales_orders so " + where
                    + " ORDER BY so.created_at DESC, so.id DESC LIMIT :limit OFFSET :offset";
            NativeQuery<Object[]> query = session.createNativeQuery(ORDER_SUMMARY_SELECT.formatted(pageIds), Object[].class);
            bindOrderFilter(query, fromDate, toDate, employeeId, hasKeyword ? keyword : null);
            query.setParameter("limit", validSize);
            query.setParameter("offset", (validPage - 1) * validSize);

            List<SalesOrderDTO> list = new ArrayList<>();
            for (Object[] r : query.list()) {
                list.add(mapOrderRow(r));
            }
            return new PageResult<>(list, totalRecords, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn lịch sử bán hàng: ", e);
            return new PageResult<>(Collections.emptyList(), 0L, validPage, validSize);
        }
    }

    // Gán tham số bộ lọc cho truy vấn danh sách hóa đơn
    private static void bindOrderFilter(NativeQuery<?> query, LocalDate fromDate, LocalDate toDate, Integer employeeId, String keyword) {
        query.setParameter("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        query.setParameter("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        if (employeeId != null) query.setParameter("empId", employeeId);
        if (keyword != null) query.setParameter("kw", likeParam(keyword));
    }

    // Ánh xạ dòng dữ liệu SQL sang đối tượng SalesOrderDTO
    private static SalesOrderDTO mapOrderRow(Object[] r) {
        return new SalesOrderDTO(
                toLongObj(r[0]), toStr(r[1]), toLocalDateTime(r[2]), toInteger(r[3]), toStr(r[4]), null,
                toStr(r[5]), toInt(r[6]), toInt(r[7]), toBigDecimal(r[8]), toBigDecimal(r[9]),
                toBigDecimal(r[10]), toStr(r[11]));
    }

    // Lấy danh sách chi tiết các mặt hàng của một hóa đơn
    public List<SalesOrderDetailDTO> getSalesOrderItems(long orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT soi.id, soi.medicine_id, m.code, m.name, mc.name, m.active_ingredient, m.unit,
                       il.lot_number, il.expiry_date, soi.quantity, soi.unit_price, soi.line_total
                FROM sales_order_items soi
                JOIN medicines m ON soi.medicine_id = m.id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                LEFT JOIN inventory_lots il ON soi.inventory_lot_id = il.id
                WHERE soi.sales_order_id = :orderId
                ORDER BY soi.id ASC
            """;
            List<Object[]> rows = session.createNativeQuery(sql, Object[].class)
                    .setParameter("orderId", orderId)
                    .list();
            List<SalesOrderDetailDTO> list = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                list.add(new SalesOrderDetailDTO(
                        toLongObj(r[0]), toInteger(r[1]), toStr(r[2]), toStr(r[3]), toStr(r[4]), toStr(r[5]), toStr(r[6]),
                        toStr(r[7]), toLocalDate(r[8]), toInt(r[9]), toBigDecimal(r[10]), toBigDecimal(r[11])));
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết mặt hàng đơn bán {}: ", orderId, e);
            return Collections.emptyList();
        }
    }

    // Lấy thống kê tổng quan doanh thu và số đơn bán trong ngày hôm nay
    public TodaySalesSummary getTodaySalesSummary(Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate today = LocalDate.now();
            String sql = """
                SELECT COALESCE(SUM(total_amount), 0), COUNT(*)
                FROM sales_orders
                WHERE created_at >= :todayStart AND created_at < :tomorrowStart
            """ + (employeeId != null ? " AND employee_id = :employeeId" : "");
            var query = session.createNativeQuery(sql, Object[].class)
                    .setParameter("todayStart", Timestamp.valueOf(today.atStartOfDay()))
                    .setParameter("tomorrowStart", Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }
            Object[] r = query.uniqueResult();
            return new TodaySalesSummary(toBigDecimal(r[0]), toLong(r[1]));
        } catch (Exception e) {
            log.error("Lỗi khi thống kê doanh số hôm nay trong SalesRepository: ", e);
            return TodaySalesSummary.EMPTY;
        }
    }

    // Lấy doanh số bán hàng trong 7 ngày gần nhất tính đến hôm nay
    public List<DailySalesDTO> getLast7DaysSales(Integer employeeId) {
        return get7DaysSalesUpTo(LocalDate.now(), employeeId);
    }

    // Lấy doanh số bán hàng trong 7 ngày tính đến một ngày cụ thể
    public List<DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate, Integer employeeId) {
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        LocalDate startDate = endDate.minusDays(6);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT DATE(so.created_at) AS order_date,
                       COALESCE(SUM(so.total_amount), 0) AS total_rev,
                       COUNT(*) AS total_orders,
                       COALESCE(SUM(q.qty), 0) AS total_qty
                FROM sales_orders so
                LEFT JOIN LATERAL (
                    SELECT SUM(soi.quantity) AS qty FROM sales_order_items soi WHERE soi.sales_order_id = so.id
                ) q ON TRUE
                WHERE so.created_at >= :startTime AND so.created_at < :endTime
            """ + (employeeId != null ? " AND so.employee_id = :employeeId " : "") + """
                GROUP BY DATE(so.created_at)
            """;
            var query = session.createNativeQuery(sql, Object[].class)
                    .setParameter("startTime", Timestamp.valueOf(startDate.atStartOfDay()))
                    .setParameter("endTime", Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }

            Map<LocalDate, Object[]> byDate = new HashMap<>();
            for (Object[] r : query.list()) {
                LocalDate d = toLocalDate(r[0]);
                if (d != null) byDate.put(d, r);
            }

            List<DailySalesDTO> result = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                LocalDate d = startDate.plusDays(i);
                Object[] r = byDate.get(d);
                result.add(r == null
                        ? new DailySalesDTO(d, dayOfWeekLabel(d.getDayOfWeek()), BigDecimal.ZERO, 0L, 0L)
                        : new DailySalesDTO(d, dayOfWeekLabel(d.getDayOfWeek()), toBigDecimal(r[1]), toLong(r[2]), toLong(r[3])));
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn doanh số 7 ngày gần nhất trong SalesRepository: ", e);
            return Collections.emptyList();
        }
    }

    // Chuyển thứ trong tuần sang tên viết tắt tiếng Việt
    private static String dayOfWeekLabel(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    // Lấy danh sách các đơn bán hàng gần nhất cho Dashboard
    public List<SalesOrderDTO> getRecentSalesOrders(int limit, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String pageIds = "SELECT so.id FROM sales_orders so"
                    + (employeeId != null ? " WHERE so.employee_id = :employeeId" : "")
                    + " ORDER BY so.created_at DESC, so.id DESC LIMIT :limit";
            var query = session.createNativeQuery(ORDER_SUMMARY_SELECT.formatted(pageIds), Object[].class)
                    .setParameter("limit", Math.max(1, limit));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }
            List<Object[]> rows = query.list();
            List<SalesOrderDTO> list = new ArrayList<>(rows.size());
            for (Object[] r : rows) {
                list.add(mapOrderRow(r));
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy giao dịch gần đây trong SalesRepository: ", e);
            return Collections.emptyList();
        }
    }
}
