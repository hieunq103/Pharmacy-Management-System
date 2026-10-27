package com.pharmacy.sales;

import com.pharmacy.common.PageResult;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.CartItem;
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

    /** Cột thông tin đơn hàng (đã tổng hợp chi tiết) theo thứ tự {@link #mapOrderRow(Object[])} đọc. */
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

    /**
     * Lấy danh sách tất cả danh mục thuốc để hiển thị các nút pill filter hoặc combobox
     */
    public List<String> getAllCategories() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery("SELECT name FROM medicine_categories ORDER BY id ASC", String.class).list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh mục thuốc cho màn hình bán hàng: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Phân trang thuốc cho quầy bán hàng (thuốc còn hàng bán được xếp trước, rồi theo tên).
     * Bước 1 chỉ lấy ID của trang (mỗi thuốc 1 lần dò chỉ mục EXISTS), bước 2 tính tồn kho
     * cho đúng các thuốc trong trang bằng LATERAL — thay cho 5 subquery x toàn bộ danh mục.
     */
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

    private static void bindMedicineFilter(NativeQuery<?> query, String keyword, String category) {
        if (keyword != null) query.setParameter("keyword", likeParam(keyword));
        if (category != null) query.setParameter("category", category.trim());
    }

    /**
     * Thuốc ở quầy bán: số lượng hiển thị là tồn BÁN ĐƯỢC (không tính lô đã hết hạn),
     * đúng với số lượng mà {@link #createSalesOrder(SalesOrderRequest)} cho phép trừ kho.
     */
    private static MedicineInventoryDTO mapSalesMedicineRow(Object[] row) {
        int sellableQty = Math.max(0, toInt(row[15]));
        int expiredCount = toInt(row[12]);
        int nearExpiredCount = toInt(row[13]);

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
                toBigDecimal(row[6]), toStr(row[9]), toLocalDate(row[10]), sellableQty, status);
        dto.setActiveIngredient(toStr(row[7]));
        dto.setManufacturer(toStr(row[8]));
        return dto;
    }

    /**
     * Sinh trước mã hóa đơn bán lẻ độc nhất: HDB-[STORE_CODE]-YYMMDD-HHmmss-XX
     * - STORE_CODE: Đọc động từ file payment.properties (tuyệt đối không hardcode).
     * - YYMMDD-HHmmss: Thời gian thực tạo đơn đến từng giây.
     * - XX: Số ngẫu nhiên chống xung đột đồng thời khi nhiều quầy/cửa hàng cùng bấm thanh toán.
     * Ví dụ: HDB-CH01-261022-152630-78 (Độ dài 25 ký tự, vừa vặn cột VARCHAR(30) của Database).
     */
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

    /**
     * Tạo hóa đơn bán lẻ và trừ kho theo FEFO trong một transaction.
     * <ul>
     *   <li>Giá bán lấy từ bảng medicines (không tin giá/tổng tiền gửi từ client); nếu giá trong giỏ
     *       khác giá hiện tại thì từ chối để tránh lệch với số tiền khách đã chuyển khoản.</li>
     *   <li>Chỉ trừ lô CÒN HẠN (expiry_date &gt; hôm nay) — không bán thuốc hết hạn.</li>
     *   <li>Các lô được khóa bằng SELECT ... FOR UPDATE nên hai quầy thanh toán đồng thời
     *       không thể bán vượt tồn.</li>
     *   <li>Chi tiết hóa đơn và cập nhật tồn chạy bằng JDBC batch.</li>
     * </ul>
     */
    public String createSalesOrder(SalesOrderRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng rỗng, không có sản phẩm nào để thanh toán.");
        }
        if (req.getEmployeeId() == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên lập hóa đơn.");
        }
        String orderCode = hasText(req.getOrderCode()) ? req.getOrderCode().trim() : generateNextOrderCode();

        try {
            HibernateUtil.execute(session -> session.doWork(conn -> {
                // Gộp số lượng theo thuốc (giữ thứ tự trong giỏ)
                Map<Integer, Integer> qtyByMedicine = new LinkedHashMap<>();
                Map<Integer, CartItem> cartByMedicine = new HashMap<>();
                for (CartItem item : req.items()) {
                    if (item.getMedicineId() == null || item.getQuantity() <= 0) {
                        throw new IllegalArgumentException("Số lượng của thuốc '" + item.getMedicineName() + "' phải lớn hơn 0.");
                    }
                    qtyByMedicine.merge(item.getMedicineId(), item.getQuantity(), Integer::sum);
                    cartByMedicine.putIfAbsent(item.getMedicineId(), item);
                }

                Map<Integer, BigDecimal> prices = loadActivePrices(conn, qtyByMedicine.keySet());
                Map<Integer, List<long[]>> lotsByMedicine = lockSellableLots(conn, qtyByMedicine.keySet());

                // 1. Tính tiền từ giá DB và phân bổ số lượng vào các lô theo FEFO
                BigDecimal subtotal = BigDecimal.ZERO;
                List<Object[]> lines = new ArrayList<>(); // {medicineId, lotId, qty, unitPrice, lineTotal}
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

                BigDecimal discount = req.getDiscount() != null ? req.getDiscount().max(BigDecimal.ZERO).min(subtotal) : BigDecimal.ZERO;
                BigDecimal totalAmount = subtotal.subtract(discount);
                String phone = hasText(req.getCustomerPhone()) ? req.getCustomerPhone().trim() : null;

                // 2. Hóa đơn
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

                // 3. Chi tiết hóa đơn + trừ tồn (batch)
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

                // 4. Giao dịch xuất kho EXPORT_SALE
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

    private static String placeholders(int n) {
        return String.join(",", Collections.nCopies(n, "?"));
    }

    /** Giá bán hiện tại của các thuốc đang kinh doanh. Khóa dòng (FOR SHARE) để giá không đổi giữa chừng. */
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

    /**
     * Khóa (FOR UPDATE) toàn bộ lô còn tồn và còn hạn của các thuốc trong giỏ bằng 1 câu lệnh,
     * sắp theo FEFO. Trả về medicineId -> [{lotId, quantityRemaining}, ...].
     */
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

    /**
     * Lịch sử bán hàng theo ngày, phân quyền nhân viên/quản lý.
     * @param date Ngày bán (mặc định hôm nay nếu null)
     * @param employeeId ID nhân viên nếu là cấp Nhân viên (chỉ xem của bản thân); null nếu là Quản lý
     * @param keyword Từ khóa tìm kiếm mã đơn, SĐT hoặc tên nhân viên
     */
    public PageResult<SalesOrderDTO> getSalesOrders(LocalDate date, Integer employeeId, String keyword, int page, int size) {
        if (date == null) date = LocalDate.now();
        return getSalesOrders(date, date, employeeId, keyword, page, size);
    }

    /**
     * Lịch sử bán hàng trong khoảng ngày [fromDate, toDate] (gồm cả hai đầu), ví dụ cả một tháng.
     */
    public PageResult<SalesOrderDTO> getSalesOrders(LocalDate fromDate, LocalDate toDate, Integer employeeId, String keyword,
                                                    int page, int size) {
        if (fromDate == null) fromDate = LocalDate.now();
        if (toDate == null || toDate.isBefore(fromDate)) toDate = fromDate;
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = hasText(keyword);
            // Khoảng nửa mở [00:00 ngày đầu, 00:00 ngày sau ngày cuối) dùng được chỉ mục created_at
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

    private static void bindOrderFilter(NativeQuery<?> query, LocalDate fromDate, LocalDate toDate, Integer employeeId, String keyword) {
        query.setParameter("fromTs", Timestamp.valueOf(fromDate.atStartOfDay()));
        query.setParameter("toTs", Timestamp.valueOf(toDate.plusDays(1).atStartOfDay()));
        if (employeeId != null) query.setParameter("empId", employeeId);
        if (keyword != null) query.setParameter("kw", likeParam(keyword));
    }

    private static SalesOrderDTO mapOrderRow(Object[] r) {
        return new SalesOrderDTO(
                toLongObj(r[0]), toStr(r[1]), toLocalDateTime(r[2]), toInteger(r[3]), toStr(r[4]), null,
                toStr(r[5]), toInt(r[6]), toInt(r[7]), toBigDecimal(r[8]), toBigDecimal(r[9]),
                toBigDecimal(r[10]), toStr(r[11]));
    }

    /**
     * Lấy danh sách các mặt hàng chi tiết trong 1 đơn bán hàng
     */
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

    /**
     * Doanh số và số hóa đơn hôm nay so với hôm qua — 1 câu SQL, quét theo chỉ mục created_at.
     */
    public TodaySalesSummary getTodaySalesSummary(Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate today = LocalDate.now();
            String sql = """
                SELECT COALESCE(SUM(CASE WHEN created_at >= :todayStart THEN total_amount END), 0),
                       COUNT(CASE WHEN created_at >= :todayStart THEN 1 END),
                       COALESCE(SUM(CASE WHEN created_at < :todayStart THEN total_amount END), 0),
                       COUNT(CASE WHEN created_at < :todayStart THEN 1 END)
                FROM sales_orders
                WHERE created_at >= :yesterdayStart AND created_at < :tomorrowStart
            """ + (employeeId != null ? " AND employee_id = :employeeId" : "");
            var query = session.createNativeQuery(sql, Object[].class)
                    .setParameter("todayStart", Timestamp.valueOf(today.atStartOfDay()))
                    .setParameter("yesterdayStart", Timestamp.valueOf(today.minusDays(1).atStartOfDay()))
                    .setParameter("tomorrowStart", Timestamp.valueOf(today.plusDays(1).atStartOfDay()));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }
            Object[] r = query.uniqueResult();
            return TodaySalesSummary.of(toBigDecimal(r[0]), toBigDecimal(r[2]), toLong(r[1]), toLong(r[3]));
        } catch (Exception e) {
            log.error("Lỗi khi thống kê doanh số hôm nay trong SalesRepository: ", e);
            return TodaySalesSummary.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);
        }
    }

    /**
     * Doanh số theo từng ngày trong 7 ngày gần nhất phục vụ vẽ biểu đồ tuần
     */
    public List<DailySalesDTO> getLast7DaysSales(Integer employeeId) {
        return get7DaysSalesUpTo(LocalDate.now(), employeeId);
    }

    /**
     * Doanh số, số đơn và số lượng thuốc theo ngày trong 7 ngày tính đến endDate (gồm endDate).
     * Một câu SQL: số lượng thuốc của mỗi đơn lấy qua LATERAL nên SUM(total_amount) không bị nhân bản bởi JOIN.
     */
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

    /**
     * Các giao dịch bán hàng gần đây nhất cho Dashboard.
     * Lấy LIMIT trên sales_orders trước (backward scan chỉ mục created_at), sau đó mới tổng hợp chi tiết —
     * trước đây GROUP BY toàn bộ sales_orders JOIN sales_order_items rồi mới LIMIT.
     */
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
