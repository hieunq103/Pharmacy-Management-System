package com.pharmacy.sales;

import com.pharmacy.common.PageResult;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.sales.dto.TodaySalesSummary;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SalesRepository {

    private static final Logger log = LoggerFactory.getLogger(SalesRepository.class);

    /**
     * Lấy danh sách tất cả danh mục thuốc để hiển thị các nút pill filter hoặc combobox
     */
    public List<String> getAllCategories() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT name FROM medicine_categories ORDER BY id ASC";
            return session.createNativeQuery(sql, String.class).list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh mục thuốc cho màn hình bán hàng: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Phân trang thuốc phục vụ quầy bán hàng (mỗi trang hiển thị 6 thuốc)
     */
    public PageResult<MedicineInventoryDTO> getMedicinesPage(int page, int size, String keyword, String categoryName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            boolean hasCategory = categoryName != null && !categoryName.trim().isEmpty() 
                    && !"Tất cả".equalsIgnoreCase(categoryName)
                    && !"Tất cả nhóm thuốc".equalsIgnoreCase(categoryName);

            // 1. COUNT
            StringBuilder countSql = new StringBuilder("""
                SELECT COUNT(m.id) 
                FROM medicines m 
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id 
                WHERE m.status = 'ACTIVE'
            """);
            if (hasKeyword) {
                countSql.append(" AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) ");
            }
            if (hasCategory) {
                countSql.append(" AND mc.name = :category ");
            }

            var countQuery = session.createNativeQuery(countSql.toString());
            if (hasKeyword) {
                countQuery.setParameter("keyword", "%" + keyword.trim() + "%");
            }
            if (hasCategory) {
                countQuery.setParameter("category", categoryName.trim());
            }

            Object countResult = countQuery.uniqueResult();
            long total = countResult != null ? ((Number) countResult).longValue() : 0L;
            if (total == 0) {
                return PageResult.empty(page, size);
            }

            // 2. DATA
            StringBuilder sql = new StringBuilder("""
                SELECT 
                    m.id, m.code, m.name, mc.name as category_name, m.requires_prescription, m.unit, m.sell_price, 
                    m.active_ingredient, m.manufacturer,
                    (SELECT il.lot_number FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_lot, 
                    (SELECT il.expiry_date FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_expiry, 
                    COALESCE((SELECT SUM(il.quantity_remaining) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0), 0) as total_qty, 
                    COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= CURDATE()), 0) as expired_count, 
                    COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)), 0) as near_expired_count 
                FROM medicines m 
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id 
                WHERE m.status = 'ACTIVE'
            """);

            if (hasKeyword) {
                sql.append(" AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) ");
            }
            if (hasCategory) {
                sql.append(" AND mc.name = :category ");
            }

            sql.append(" ORDER BY (CASE WHEN total_qty > 0 THEN 0 ELSE 1 END) ASC, m.name ASC ");

            NativeQuery<Object[]> query = session.createNativeQuery(sql.toString(), Object[].class);
            if (hasKeyword) {
                query.setParameter("keyword", "%" + keyword.trim() + "%");
            }
            if (hasCategory) {
                query.setParameter("category", categoryName.trim());
            }

            query.setFirstResult((page - 1) * size);
            query.setMaxResults(size);

            List<Object[]> rows = query.list();
            List<MedicineInventoryDTO> list = new ArrayList<>();
            for (Object[] row : rows) {
                list.add(mapRowToDTO(row));
            }
            return new PageResult<>(list, total, page, size);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang thuốc cho bán hàng: ", e);
            return PageResult.empty(page, size);
        }
    }

    /**
     * Lấy danh sách thuốc phục vụ quầy bán hàng (hỗ trợ lọc từ khóa và danh mục)
     */
    public List<MedicineInventoryDTO> getMedicinesForSales(String keyword, String categoryName) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder sql = new StringBuilder("""
                SELECT 
                    m.id, m.code, m.name, mc.name as category_name, m.requires_prescription, m.unit, m.sell_price, 
                    m.active_ingredient, m.manufacturer,
                    (SELECT il.lot_number FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_lot, 
                    (SELECT il.expiry_date FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_expiry, 
                    COALESCE((SELECT SUM(il.quantity_remaining) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0), 0) as total_qty, 
                    COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= CURDATE()), 0) as expired_count, 
                    COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)), 0) as near_expired_count 
                FROM medicines m 
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id 
                WHERE m.status = 'ACTIVE'
            """);

            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            boolean hasCategory = categoryName != null && !categoryName.trim().isEmpty() 
                    && !"Tất cả".equalsIgnoreCase(categoryName)
                    && !"Tất cả nhóm thuốc".equalsIgnoreCase(categoryName);

            if (hasKeyword) {
                sql.append(" AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) ");
            }
            if (hasCategory) {
                sql.append(" AND mc.name = :category ");
            }

            sql.append(" ORDER BY (CASE WHEN total_qty > 0 THEN 0 ELSE 1 END) ASC, m.name ASC ");

            NativeQuery<Object[]> query = session.createNativeQuery(sql.toString(), Object[].class);
            if (hasKeyword) {
                query.setParameter("keyword", "%" + keyword.trim() + "%");
            }
            if (hasCategory) {
                query.setParameter("category", categoryName.trim());
            }

            List<Object[]> rows = query.list();
            List<MedicineInventoryDTO> list = new ArrayList<>();
            for (Object[] row : rows) {
                list.add(mapRowToDTO(row));
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi tải thuốc cho bán hàng: ", e);
            return Collections.emptyList();
        }
    }

    private MedicineInventoryDTO mapRowToDTO(Object[] row) {
        Integer id = row[0] != null ? ((Number) row[0]).intValue() : null;
        String code = (String) row[1];
        String name = (String) row[2];
        String cat = (String) row[3];
        Boolean reqPrescription = row[4] != null ? (Boolean) row[4] : false;
        String unit = (String) row[5];
        BigDecimal sellPrice = row[6] != null ? (BigDecimal) row[6] : BigDecimal.ZERO;
        String actIngr = (String) row[7];
        String manuf = (String) row[8];
        String nearestLot = (String) row[9];

        LocalDate nearestExpiry = null;
        if (row[10] instanceof Date d) {
            nearestExpiry = d.toLocalDate();
        } else if (row[10] instanceof LocalDate ld) {
            nearestExpiry = ld;
        }

        int totalQty = row[11] != null ? ((Number) row[11]).intValue() : 0;
        int expiredCount = row[12] != null ? ((Number) row[12]).intValue() : 0;
        int nearExpiredCount = row[13] != null ? ((Number) row[13]).intValue() : 0;

        MedicineInventoryDTO.InventoryStatus status = MedicineInventoryDTO.InventoryStatus.NORMAL;
        if (totalQty == 0) {
            status = MedicineInventoryDTO.InventoryStatus.OUT_OF_STOCK;
        } else if (expiredCount > 0) {
            status = MedicineInventoryDTO.InventoryStatus.HAS_EXPIRED_LOT;
        } else if (nearExpiredCount > 0) {
            status = MedicineInventoryDTO.InventoryStatus.HAS_NEAR_EXPIRY_LOT;
        }

        MedicineInventoryDTO dto = new MedicineInventoryDTO(
                id, code, name, cat, reqPrescription, unit, sellPrice,
                nearestLot, nearestExpiry, totalQty, status
        );
        dto.setActiveIngredient(actIngr);
        dto.setManufacturer(manuf);
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
        String storeClean = (store != null && !store.isBlank())
                ? store.trim().toUpperCase().replaceAll("[^A-Z0-9]", "")
                : "CH01";
        if (storeClean.length() > 6) {
            storeClean = storeClean.substring(0, 6);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmmss"));
        int randomSuffix = java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 100);
        return String.format("HDB-%s-%s-%02d", storeClean, timestamp, randomSuffix);
    }

    /**
     * Tạo hóa đơn bán lẻ tại quầy và trừ kho tự động theo nguyên tắc FEFO (First Expired First Out)
     */
    public String createSalesOrder(SalesOrderRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng rỗng, không có sản phẩm nào để thanh toán.");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                // 1. Mã hóa đơn HDB-YYMMDD-XXXX (dùng mã đã sinh trước nếu có)
                String orderCode = req.getOrderCode();
                if (orderCode == null || orderCode.isBlank()) {
                    orderCode = generateNextOrderCode();
                }

                Integer employeeId = req.getEmployeeId();
                if (employeeId == null) {
                    employeeId = 1; // Fallback nhân viên mặc định nếu session dev
                }

                BigDecimal totalAmount = req.getTotalAmount();
                BigDecimal discount = req.getDiscount();
                String paymentMethod = req.getPaymentMethod(); // 'CASH' or 'TRANSFER'

                // 2. Insert vào bảng sales_orders
                String insertOrderSql = """
                    INSERT INTO sales_orders (code, employee_id, customer_phone, total_amount, discount, payment_method, created_at)
                    VALUES (:code, :employeeId, :phone, :totalAmount, :discount, :paymentMethod, NOW())
                """;
                session.createNativeQuery(insertOrderSql)
                        .setParameter("code", orderCode)
                        .setParameter("employeeId", employeeId)
                        .setParameter("phone", req.getCustomerPhone() != null && !req.getCustomerPhone().isBlank() ? req.getCustomerPhone().trim() : null)
                        .setParameter("totalAmount", totalAmount)
                        .setParameter("discount", discount)
                        .setParameter("paymentMethod", paymentMethod)
                        .executeUpdate();

                // Lấy ID hóa đơn vừa tạo
                Number orderIdNum = (Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult();
                long orderId = orderIdNum.longValue();

                // 3. Xử lý từng sản phẩm trong giỏ: Trừ lô kho theo FEFO (Hạn gần nhất trừ trước)
                for (CartItem item : req.items()) {
                    int remainingNeeded = item.getQuantity();

                    // Query các lô còn hạn và còn số lượng tồn của thuốc này
                    String lotSql = """
                        SELECT id, quantity_remaining 
                        FROM inventory_lots 
                        WHERE medicine_id = :medId AND quantity_remaining > 0 
                        ORDER BY (CASE WHEN expiry_date IS NULL THEN 1 ELSE 0 END) ASC, expiry_date ASC, id ASC
                    """;
                    List<Object[]> availableLots = session.createNativeQuery(lotSql, Object[].class)
                            .setParameter("medId", item.getMedicineId())
                            .list();

                    int availableTotal = availableLots.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
                    if (availableTotal < remainingNeeded) {
                        throw new IllegalStateException("Thuốc '" + item.getMedicineName() + "' không đủ số lượng tồn trong kho để bán (Còn " + availableTotal + ", yêu cầu " + remainingNeeded + ").");
                    }

                    for (Object[] lotRow : availableLots) {
                        if (remainingNeeded <= 0) break;

                        long lotId = ((Number) lotRow[0]).longValue();
                        int lotQty = ((Number) lotRow[1]).intValue();

                        int deductQty = Math.min(lotQty, remainingNeeded);

                        // Trừ số lượng trong inventory_lots
                        String updateLotSql = "UPDATE inventory_lots SET quantity_remaining = quantity_remaining - :deduct WHERE id = :lotId";
                        session.createNativeQuery(updateLotSql)
                                .setParameter("deduct", deductQty)
                                .setParameter("lotId", lotId)
                                .executeUpdate();

                        // Thêm chi tiết dòng hóa đơn
                        BigDecimal lineTotal = item.getSellPrice().multiply(BigDecimal.valueOf(deductQty));
                        String insertItemSql = """
                            INSERT INTO sales_order_items (sales_order_id, medicine_id, inventory_lot_id, quantity, unit_price, line_total)
                            VALUES (:orderId, :medicineId, :lotId, :qty, :unitPrice, :lineTotal)
                        """;
                        session.createNativeQuery(insertItemSql)
                                .setParameter("orderId", orderId)
                                .setParameter("medicineId", item.getMedicineId())
                                .setParameter("lotId", lotId)
                                .setParameter("qty", deductQty)
                                .setParameter("unitPrice", item.getSellPrice())
                                .setParameter("lineTotal", lineTotal)
                                .executeUpdate();

                        remainingNeeded -= deductQty;
                    }
                }

                // 4. Ghi nhận giao dịch xuất kho loại EXPORT_SALE
                String insertTxSql = """
                    INSERT INTO inventory_transactions (type, sales_order_id, employee_id, transaction_at, note)
                    VALUES ('EXPORT_SALE', :orderId, :empId, NOW(), :note)
                """;
                session.createNativeQuery(insertTxSql)
                        .setParameter("orderId", orderId)
                        .setParameter("empId", employeeId)
                        .setParameter("note", "Bán lẻ tại quầy - Mã đơn " + orderCode)
                        .executeUpdate();

                tx.commit();
                return orderCode;
            } catch (Exception e) {
                if (tx.isActive()) tx.rollback();
                log.error("Lỗi giao dịch thanh toán đơn hàng: ", e);
                throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Lỗi khi xử lý thanh toán đơn hàng.", e);
            }
        }
    }

    /**
     * Lấy danh sách lịch sử bán hàng theo ngày và theo phân quyền nhân viên/quản lý
     * @param date Ngày bán (mặc định hôm nay nếu null)
     * @param employeeId ID nhân viên nếu là cấp Nhân viên (chỉ xem của bản thân); null nếu là Quản lý (xem toàn hệ thống)
     * @param keyword Từ khóa tìm kiếm mã đơn, SĐT hoặc tên nhân viên
     * @param page Trang hiện tại (1-indexed)
     * @param size Số bản ghi mỗi trang
     */
    public PageResult<SalesOrderDTO> getSalesOrders(LocalDate date, Integer employeeId, String keyword, int page, int size) {
        if (date == null) date = LocalDate.now();
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        int offset = (validPage - 1) * validSize;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            boolean hasEmployee = employeeId != null;

            // 1. COUNT QUERY
            StringBuilder countSql = new StringBuilder("""
                SELECT COUNT(so.id)
                FROM sales_orders so
                LEFT JOIN employees e ON so.employee_id = e.id
                WHERE DATE(so.created_at) = :orderDate
            """);
            if (hasEmployee) {
                countSql.append(" AND so.employee_id = :empId ");
            }
            if (hasKeyword) {
                countSql.append(" AND (so.code LIKE :kw OR so.customer_phone LIKE :kw OR e.full_name LIKE :kw) ");
            }

            NativeQuery<?> countQuery = session.createNativeQuery(countSql.toString());
            countQuery.setParameter("orderDate", Date.valueOf(date));
            if (hasEmployee) {
                countQuery.setParameter("empId", employeeId);
            }
            if (hasKeyword) {
                countQuery.setParameter("kw", "%" + keyword.trim() + "%");
            }
            Number totalRecordsNum = (Number) countQuery.uniqueResult();
            long totalRecords = totalRecordsNum != null ? totalRecordsNum.longValue() : 0L;

            if (totalRecords == 0) {
                return new PageResult<>(Collections.emptyList(), 0L, validPage, validSize);
            }

            // 2. DATA QUERY
            StringBuilder dataSql = new StringBuilder("""
                SELECT 
                    so.id,
                    so.code,
                    so.created_at,
                    so.employee_id,
                    e.full_name AS employee_name,
                    NULL AS employee_code,
                    so.customer_phone,
                    COUNT(DISTINCT soi.medicine_id) AS item_count,
                    COALESCE(SUM(soi.quantity), 0) AS total_qty,
                    COALESCE(SUM(soi.line_total), 0) AS subtotal,
                    so.discount,
                    so.total_amount,
                    so.payment_method
                FROM sales_orders so
                LEFT JOIN employees e ON so.employee_id = e.id
                LEFT JOIN sales_order_items soi ON so.id = soi.sales_order_id
                WHERE DATE(so.created_at) = :orderDate
            """);
            if (hasEmployee) {
                dataSql.append(" AND so.employee_id = :empId ");
            }
            if (hasKeyword) {
                dataSql.append(" AND (so.code LIKE :kw OR so.customer_phone LIKE :kw OR e.full_name LIKE :kw) ");
            }
            dataSql.append("""
                GROUP BY so.id, so.code, so.created_at, so.employee_id, e.full_name, so.customer_phone, so.discount, so.total_amount, so.payment_method
                ORDER BY so.created_at DESC, so.id DESC
                LIMIT :limit OFFSET :offset
            """);

            NativeQuery<Object[]> query = session.createNativeQuery(dataSql.toString(), Object[].class);
            query.setParameter("orderDate", Date.valueOf(date));
            if (hasEmployee) {
                query.setParameter("empId", employeeId);
            }
            if (hasKeyword) {
                query.setParameter("kw", "%" + keyword.trim() + "%");
            }
            query.setParameter("limit", validSize);
            query.setParameter("offset", offset);

            List<Object[]> rows = query.list();
            List<SalesOrderDTO> list = new ArrayList<>();
            for (Object[] r : rows) {
                Long id = ((Number) r[0]).longValue();
                String code = (String) r[1];
                java.time.LocalDateTime createdAt = null;
                if (r[2] instanceof java.sql.Timestamp ts) {
                    createdAt = ts.toLocalDateTime();
                } else if (r[2] instanceof java.time.LocalDateTime ldt) {
                    createdAt = ldt;
                }
                Integer empId = r[3] != null ? ((Number) r[3]).intValue() : null;
                String empName = (String) r[4];
                String empCode = (String) r[5];
                String phone = (String) r[6];
                int itemCount = r[7] != null ? ((Number) r[7]).intValue() : 0;
                int totalQty = r[8] != null ? ((Number) r[8]).intValue() : 0;
                BigDecimal subtotal = r[9] != null ? (BigDecimal) r[9] : BigDecimal.ZERO;
                BigDecimal discount = r[10] != null ? (BigDecimal) r[10] : BigDecimal.ZERO;
                BigDecimal totalAmount = r[11] != null ? (BigDecimal) r[11] : BigDecimal.ZERO;
                String paymentMethod = (String) r[12];

                list.add(new SalesOrderDTO(
                        id, code, createdAt, empId, empName, empCode,
                        phone, itemCount, totalQty, subtotal, discount, totalAmount, paymentMethod
                ));
            }

            return new PageResult<>(list, totalRecords, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn lịch sử bán hàng: ", e);
            return new PageResult<>(Collections.emptyList(), 0L, validPage, validSize);
        }
    }

    /**
     * Lấy danh sách các mặt hàng chi tiết trong 1 đơn bán hàng
     */
    public List<SalesOrderDetailDTO> getSalesOrderItems(long orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT 
                    soi.id,
                    soi.medicine_id,
                    m.code AS medicine_code,
                    m.name AS medicine_name,
                    mc.name AS category_name,
                    m.active_ingredient,
                    m.unit,
                    il.lot_number,
                    il.expiry_date,
                    soi.quantity,
                    soi.unit_price,
                    soi.line_total
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

            List<SalesOrderDetailDTO> list = new ArrayList<>();
            for (Object[] r : rows) {
                Long id = ((Number) r[0]).longValue();
                Integer medId = ((Number) r[1]).intValue();
                String medCode = (String) r[2];
                String medName = (String) r[3];
                String catName = (String) r[4];
                String actIngr = (String) r[5];
                String unit = (String) r[6];
                String lotNum = (String) r[7];
                LocalDate expDate = null;
                if (r[8] instanceof Date d) {
                    expDate = d.toLocalDate();
                } else if (r[8] instanceof LocalDate ld) {
                    expDate = ld;
                }
                int qty = r[9] != null ? ((Number) r[9]).intValue() : 0;
                BigDecimal unitPrice = r[10] != null ? (BigDecimal) r[10] : BigDecimal.ZERO;
                BigDecimal lineTotal = r[11] != null ? (BigDecimal) r[11] : BigDecimal.ZERO;

                list.add(new SalesOrderDetailDTO(
                        id, medId, medCode, medName, catName, actIngr, unit,
                        lotNum, expDate, qty, unitPrice, lineTotal
                ));
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết mặt hàng đơn bán {}: ", orderId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Thống kê doanh số và số hóa đơn bán hôm nay so với hôm qua
     */
    public TodaySalesSummary getTodaySalesSummary() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            String todaySql = "SELECT COALESCE(SUM(total_amount), 0), COUNT(id) FROM sales_orders WHERE DATE(created_at) = :tDate";
            Object[] todayRes = session.createNativeQuery(todaySql, Object[].class)
                    .setParameter("tDate", Date.valueOf(today))
                    .uniqueResult();
            BigDecimal todayRev = todayRes != null && todayRes[0] != null ? (BigDecimal) todayRes[0] : BigDecimal.ZERO;
            long todayCount = todayRes != null && todayRes[1] != null ? ((Number) todayRes[1]).longValue() : 0L;

            String yesterdaySql = "SELECT COALESCE(SUM(total_amount), 0), COUNT(id) FROM sales_orders WHERE DATE(created_at) = :yDate";
            Object[] yRes = session.createNativeQuery(yesterdaySql, Object[].class)
                    .setParameter("yDate", Date.valueOf(yesterday))
                    .uniqueResult();
            BigDecimal yRev = yRes != null && yRes[0] != null ? (BigDecimal) yRes[0] : BigDecimal.ZERO;
            long yCount = yRes != null && yRes[1] != null ? ((Number) yRes[1]).longValue() : 0L;

            return TodaySalesSummary.of(todayRev, yRev, todayCount, yCount);
        } catch (Exception e) {
            log.error("Lỗi khi thống kê doanh số hôm nay trong SalesRepository: ", e);
            return TodaySalesSummary.of(BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L);
        }
    }

    /**
     * Doanh số theo từng ngày trong 7 ngày gần nhất phục vụ vẽ biểu đồ tuần
     */
    public List<DailySalesDTO> getLast7DaysSales() {
        return get7DaysSalesUpTo(LocalDate.now());
    }

    public List<DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate) {
        return get7DaysSalesUpTo(endDate, null);
    }

    /**
     * Doanh số theo từng ngày trong 7 ngày tính đến ngày endDate chỉ định (ngày đó và 6 ngày trước)
     * Hỗ trợ lọc theo nhân viên (nếu employeeId != null)
     */
    public List<DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate, Integer employeeId) {
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate startDate = endDate.minusDays(6);

            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = endDate.atTime(LocalTime.MAX);
            Timestamp startTs = Timestamp.valueOf(from);
            Timestamp endTs = Timestamp.valueOf(to);

            String empFilter1 = (employeeId != null) ? " AND employee_id = :employeeId " : "";
            String sql = """
                SELECT DATE(created_at) as order_date, COALESCE(SUM(total_amount), 0) as total_rev, COUNT(id) as total_orders
                FROM sales_orders
                WHERE created_at >= :startTime AND created_at <= :endTime
            """ + empFilter1 + """
                GROUP BY DATE(created_at)
                ORDER BY order_date ASC
            """;

            var nativeQuery = session.createNativeQuery(sql, Object[].class)
                    .setParameter("startTime", startTs)
                    .setParameter("endTime", endTs);
            if (employeeId != null) {
                nativeQuery.setParameter("employeeId", employeeId);
            }
            List<Object[]> rows = nativeQuery.list();

            // Truy vấn thêm tổng số lượng thuốc đã bán theo từng ngày trong cùng session
            String empFilter2 = (employeeId != null) ? " AND so.employee_id = :employeeId " : "";
            String qtySql = """
                SELECT DATE(so.created_at) as order_date, COALESCE(SUM(soi.quantity), 0) as total_qty
                FROM sales_orders so
                JOIN sales_order_items soi ON so.id = soi.sales_order_id
                WHERE so.created_at >= :startTime AND so.created_at <= :endTime
            """ + empFilter2 + """
                GROUP BY DATE(so.created_at)
            """;

            var nativeQtyQuery = session.createNativeQuery(qtySql, Object[].class)
                    .setParameter("startTime", startTs)
                    .setParameter("endTime", endTs);
            if (employeeId != null) {
                nativeQtyQuery.setParameter("employeeId", employeeId);
            }
            List<Object[]> qtyRows = nativeQtyQuery.list();


            java.util.Map<LocalDate, Long> qtyMap = new java.util.HashMap<>();
            for (Object[] qr : qtyRows) {
                LocalDate qd = null;
                if (qr[0] instanceof Date sd) {
                    qd = sd.toLocalDate();
                } else if (qr[0] instanceof LocalDate ld) {
                    qd = ld;
                }
                long qVal = qr[1] != null ? ((Number) qr[1]).longValue() : 0L;
                if (qd != null) {
                    qtyMap.put(qd, qVal);
                }
            }

            java.util.Map<LocalDate, DailySalesDTO> dateMap = new java.util.HashMap<>();
            for (Object[] r : rows) {
                LocalDate d = null;
                if (r[0] instanceof Date sd) {
                    d = sd.toLocalDate();
                } else if (r[0] instanceof LocalDate ld) {
                    d = ld;
                }
                BigDecimal rev = r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO;
                long count = r[2] != null ? ((Number) r[2]).longValue() : 0L;
                long totalQty = (d != null) ? qtyMap.getOrDefault(d, 0L) : 0L;

                if (d != null) {
                    dateMap.put(d, new DailySalesDTO(d, getDayOfWeekLabel(d.getDayOfWeek()), rev, count, totalQty));
                }
            }

            List<DailySalesDTO> result = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate target = startDate.plusDays(i);
                if (dateMap.containsKey(target)) {
                    result.add(dateMap.get(target));
                } else {
                    result.add(new DailySalesDTO(target, getDayOfWeekLabel(target.getDayOfWeek()), BigDecimal.ZERO, 0L, 0L));
                }
            }

            return result;
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn doanh số 7 ngày gần nhất trong SalesRepository: ", e);
            return Collections.emptyList();
        }
    }

    private String getDayOfWeekLabel(java.time.DayOfWeek dow) {
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
     * Lấy các giao dịch bán hàng gần đây nhất cho Dashboard
     */
    public List<SalesOrderDTO> getRecentSalesOrders(int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT 
                    so.id,
                    so.code,
                    so.created_at,
                    so.employee_id,
                    e.full_name AS employee_name,
                    NULL AS employee_code,
                    so.customer_phone,
                    COUNT(DISTINCT soi.medicine_id) AS item_count,
                    COALESCE(SUM(soi.quantity), 0) AS total_qty,
                    COALESCE(SUM(soi.line_total), 0) AS subtotal,
                    so.discount,
                    so.total_amount,
                    so.payment_method
                FROM sales_orders so
                LEFT JOIN employees e ON so.employee_id = e.id
                LEFT JOIN sales_order_items soi ON so.id = soi.sales_order_id
                GROUP BY so.id, so.code, so.created_at, so.employee_id, e.full_name, so.customer_phone, so.discount, so.total_amount, so.payment_method
                ORDER BY so.created_at DESC, so.id DESC
                LIMIT :limit
            """;

            List<Object[]> rows = session.createNativeQuery(sql, Object[].class)
                    .setParameter("limit", limit)
                    .list();

            List<SalesOrderDTO> list = new ArrayList<>();
            for (Object[] r : rows) {
                Long id = ((Number) r[0]).longValue();
                String code = (String) r[1];
                LocalDateTime createdAt = null;
                if (r[2] instanceof java.sql.Timestamp ts) {
                    createdAt = ts.toLocalDateTime();
                } else if (r[2] instanceof java.time.LocalDateTime ldt) {
                    createdAt = ldt;
                }
                Integer empId = r[3] != null ? ((Number) r[3]).intValue() : null;
                String empName = (String) r[4];
                String empCode = (String) r[5];
                String phone = (String) r[6];
                int itemCount = r[7] != null ? ((Number) r[7]).intValue() : 0;
                int totalQty = r[8] != null ? ((Number) r[8]).intValue() : 0;
                BigDecimal subtotal = r[9] != null ? (BigDecimal) r[9] : BigDecimal.ZERO;
                BigDecimal discount = r[10] != null ? (BigDecimal) r[10] : BigDecimal.ZERO;
                BigDecimal totalAmount = r[11] != null ? (BigDecimal) r[11] : BigDecimal.ZERO;
                String paymentMethod = (String) r[12];

                list.add(new SalesOrderDTO(
                        id, code, createdAt, empId, empName, empCode,
                        phone, itemCount, totalQty, subtotal, discount, totalAmount, paymentMethod
                ));
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy giao dịch gần đây trong SalesRepository: ", e);
            return Collections.emptyList();
        }
    }
}
