package com.pharmacy.inventory;

import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.common.PageResult;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.pharmacy.inventory.dto.InventoryTransactionDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDetailDTO;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;

public class InventoryRepository {

    private static final Logger log = LoggerFactory.getLogger(InventoryRepository.class);

    /**
     * Lấy toàn bộ danh sách thuốc và thông tin tồn kho
     */
    public List<MedicineInventoryDTO> getAllInventory() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT 
                    m.id, m.code, m.name, mc.name as category_name, m.requires_prescription, m.unit, m.sell_price, 
                    m.active_ingredient, m.manufacturer,
                    (SELECT il.lot_number FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_lot, 
                    (SELECT il.expiry_date FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_expiry, 
                    (SELECT SUM(il.quantity_remaining) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0) as total_qty, 
                    (SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= CURDATE()) as expired_count, 
                    (SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)) as near_expired_count,
                    m.min_stock_threshold 
                FROM medicines m 
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id 
                WHERE m.status = 'ACTIVE' 
                ORDER BY m.id ASC
            """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);

            List<Object[]> rows = query.list();
            return mapRowsToDTO(rows);
        } catch (Exception e) {
            log.error("Lỗi khi lấy toàn bộ danh sách kho: ", e);
            return Collections.emptyList();
        }
    }

    public record InventorySummary(long totalMedicines, long inStockMedicines, long lowStockMedicines, long nearExpiryLots) {}

    /**
     * Lấy thống kê tổng quan kho (chỉ 1 query siêu nhanh, không load toàn bộ dữ liệu)
     */
    public InventorySummary getInventorySummary() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT 
                    (SELECT COUNT(m.id) FROM medicines m WHERE m.status = 'ACTIVE') as total_medicines,
                    (SELECT COUNT(DISTINCT il.medicine_id) FROM inventory_lots il 
                     JOIN medicines m ON il.medicine_id = m.id 
                     WHERE m.status = 'ACTIVE' AND il.quantity_remaining > 0) as instock_medicines,
                    (SELECT COUNT(*) FROM (
                        SELECT m.id FROM medicines m
                        LEFT JOIN inventory_lots il ON m.id = il.medicine_id AND il.quantity_remaining > 0
                        WHERE m.status = 'ACTIVE'
                        GROUP BY m.id
                        HAVING COALESCE(SUM(il.quantity_remaining), 0) <= 10
                    ) as low_stocks) as low_stock_medicines,
                    (SELECT COUNT(DISTINCT il.id) FROM inventory_lots il
                     JOIN medicines m ON il.medicine_id = m.id
                     WHERE il.quantity_remaining > 0
                       AND il.expiry_date >= CURDATE()
                       AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY)
                       AND m.status = 'ACTIVE') as near_expiry_lots
            """;
            Object[] row = session.createNativeQuery(sql, Object[].class).getSingleResult();
            long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            long inStock = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long lowStock = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            long nearExpiry = row[3] != null ? ((Number) row[3]).longValue() : 0L;
            return new InventorySummary(total, inStock, lowStock, nearExpiry);
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê kho: ", e);
            return new InventorySummary(0L, 0L, 0L, 0L);
        }
    }

    /**
     * Lấy danh sách cảnh báo kho (lô cận hạn và thuốc sắp hết hàng) phục vụ Dashboard / Báo cáo
     */
    public List<com.pharmacy.inventory.dto.InventoryAlertDTO> getInventoryAlerts(int limit) {
        List<com.pharmacy.inventory.dto.InventoryAlertDTO> alerts = new ArrayList<>();
        java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate maxNearExpiry = today.plusDays(30);

            // 1. Lô cận hạn & hết hạn (Ưu tiên cao nhất)
            String expirySql = """
                SELECT m.id, m.name, il.lot_number, il.expiry_date, il.quantity_remaining, m.unit
                FROM inventory_lots il
                JOIN medicines m ON il.medicine_id = m.id
                WHERE il.quantity_remaining > 0
                  AND il.expiry_date <= :maxDate
                  AND m.status = 'ACTIVE'
                ORDER BY il.expiry_date ASC
                LIMIT :limit
            """;

            List<Object[]> expiryRows = session.createNativeQuery(expirySql, Object[].class)
                    .setParameter("maxDate", java.sql.Date.valueOf(maxNearExpiry))
                    .setParameter("limit", limit)
                    .list();

            for (Object[] r : expiryRows) {
                int medId = ((Number) r[0]).intValue();
                String name = (String) r[1];
                String lot = (String) r[2];
                java.time.LocalDate expDate = null;
                if (r[3] instanceof java.sql.Date sd) expDate = sd.toLocalDate();
                else if (r[3] instanceof java.time.LocalDate ld) expDate = ld;

                int qty = r[4] != null ? ((Number) r[4]).intValue() : 0;
                String unit = (String) r[5];

                String title = name + (lot != null && !lot.isBlank() ? " — " + lot : "");
                String subtitle;
                if (expDate != null) {
                    if (expDate.isBefore(today)) {
                        subtitle = "Đã hết hạn " + expDate.format(df);
                    } else {
                        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(today, expDate);
                        subtitle = "Hết hạn " + expDate.format(df) + (daysLeft == 0 ? " (Hôm nay)" : " (Còn " + daysLeft + " ngày)");
                    }
                } else {
                    subtitle = "Cận hạn sử dụng";
                }

                alerts.add(new com.pharmacy.inventory.dto.InventoryAlertDTO(
                        medId, title, subtitle, qty, unit, com.pharmacy.inventory.dto.InventoryAlertDTO.AlertType.EXPIRY));
            }

            // 2. Thuốc sắp hết hàng (Lấy thêm up to limit thuốc tồn ít nhất)
            String lowStockSql = """
                SELECT m.id, m.name, COALESCE(SUM(il.quantity_remaining), 0) as total_qty, m.unit, m.min_stock_threshold
                FROM medicines m
                LEFT JOIN inventory_lots il ON m.id = il.medicine_id AND il.quantity_remaining > 0
                WHERE m.status = 'ACTIVE'
                GROUP BY m.id, m.name, m.unit, m.min_stock_threshold
                HAVING total_qty <= COALESCE(NULLIF(m.min_stock_threshold, 0), 10)
                ORDER BY total_qty ASC
                LIMIT :limit
            """;

            List<Object[]> lowStockRows = session.createNativeQuery(lowStockSql, Object[].class)
                    .setParameter("limit", limit)
                    .list();

            for (Object[] r : lowStockRows) {
                int medId = ((Number) r[0]).intValue();
                String name = (String) r[1];
                int qty = r[2] != null ? ((Number) r[2]).intValue() : 0;
                String unit = (String) r[3];
                int minStock = r[4] != null ? ((Number) r[4]).intValue() : 10;
                if (minStock <= 0) minStock = 10;

                String subtitle = qty == 0 ? "Đã hết hàng trong kho" : "Tồn kho dưới ngưỡng tối thiểu (" + qty + "/" + minStock + " " + unit + ")";

                alerts.add(new com.pharmacy.inventory.dto.InventoryAlertDTO(
                        medId, name, subtitle, qty, unit, com.pharmacy.inventory.dto.InventoryAlertDTO.AlertType.LOW_STOCK));
            }

        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách cảnh báo kho trong InventoryRepository: ", e);
        }

        return alerts;
    }

    /**
     * Lấy danh sách thuốc rút gọn cho Popup Nhập / Xuất kho (không tính toán lô, siêu nhẹ)
     */
    public List<MedicineInventoryDTO> getMedicinesForPicker() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT m.id, m.code, m.name, mc.name as category_name, m.unit, m.sell_price, m.active_ingredient, m.manufacturer,
                       COALESCE((SELECT SUM(il.quantity_remaining) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0), 0) as total_qty,
                       COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= CURDATE()), 0) as expired_count,
                       COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)), 0) as near_expired_count
                FROM medicines m
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                WHERE m.status = 'ACTIVE'
                ORDER BY m.name ASC
            """;
            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            List<Object[]> rows = query.list();
            List<MedicineInventoryDTO> list = new ArrayList<>();
            for (Object[] row : rows) {
                MedicineInventoryDTO dto = new MedicineInventoryDTO();
                dto.setMedicineId(((Number) row[0]).intValue());
                dto.setMedicineCode((String) row[1]);
                dto.setMedicineName((String) row[2]);
                dto.setCategoryName((String) row[3]);
                dto.setUnit((String) row[4]);
                if (row[5] instanceof BigDecimal bd) {
                    dto.setSellPrice(bd);
                } else if (row[5] instanceof Number num) {
                    dto.setSellPrice(BigDecimal.valueOf(num.doubleValue()));
                }
                dto.setActiveIngredient((String) row[6]);
                dto.setManufacturer((String) row[7]);
                dto.setTotalQuantity(row[8] != null ? ((Number) row[8]).intValue() : 0);

                int expiredCount = row.length > 9 && row[9] != null ? ((Number) row[9]).intValue() : 0;
                int nearExpiredCount = row.length > 10 && row[10] != null ? ((Number) row[10]).intValue() : 0;

                if (expiredCount > 0) {
                    dto.setExpiryStatus(MedicineInventoryDTO.ExpiryStatus.EXPIRED);
                } else if (nearExpiredCount > 0) {
                    dto.setExpiryStatus(MedicineInventoryDTO.ExpiryStatus.NEAR_EXPIRY);
                } else {
                    dto.setExpiryStatus(MedicineInventoryDTO.ExpiryStatus.NORMAL);
                }

                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách thuốc cho picker: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Phân trang chuẩn Server-side: Hỗ trợ tìm kiếm từ khóa và lọc trạng thái tại CSDL
     */
    public PageResult<MedicineInventoryDTO> getInventoryPageResult(int page, int size, String searchKeyword, String statusFilter) {
        if ("Hết hàng".equalsIgnoreCase(statusFilter) || "Sắp hết hàng".equalsIgnoreCase(statusFilter)) {
            return getInventoryPageResult(page, size, searchKeyword, statusFilter, null);
        } else if ("Có lô hết hạn".equalsIgnoreCase(statusFilter) || "Có lô cận hạn".equalsIgnoreCase(statusFilter)) {
            return getInventoryPageResult(page, size, searchKeyword, null, statusFilter);
        } else if ("Bình thường".equalsIgnoreCase(statusFilter)) {
            return getInventoryPageResult(page, size, searchKeyword, "Bình thường", null);
        }
        return getInventoryPageResult(page, size, searchKeyword, null, null);
    }

    public PageResult<MedicineInventoryDTO> getInventoryPageResult(int page, int size, String searchKeyword, String stockFilter, String expiryFilter) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = searchKeyword != null && !searchKeyword.trim().isEmpty();
            String filterCondition = buildFilterConditions(stockFilter, expiryFilter);

            // 1. Query COUNT siêu tốc
            String countSql = "SELECT COUNT(m.id) FROM medicines m WHERE m.status = 'ACTIVE' " + filterCondition;
            if (hasKeyword) {
                countSql += " AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) ";
            }

            var countQuery = session.createNativeQuery(countSql);
            if (hasKeyword) {
                countQuery.setParameter("keyword", "%" + searchKeyword.trim() + "%");
            }
            Object totalCount = countQuery.uniqueResult();
            long total = totalCount != null ? ((Number) totalCount).longValue() : 0L;

            if (total == 0) {
                return PageResult.empty(page, size);
            }

            // 2. Query DATA: Phân trang lấy trước các ID của trang hiện tại (chỉ 7 IDs),
            // sau đó chỉ tính toán chi tiết lô cho ĐÚNG các bản ghi của trang đó
            String pageIdsSql = "SELECT m.id FROM medicines m WHERE m.status = 'ACTIVE' " + filterCondition;
            if (hasKeyword) {
                pageIdsSql += " AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) ";
            }
            pageIdsSql += " ORDER BY m.id ASC LIMIT :limit OFFSET :offset";

            String dataSql = String.format("""
                SELECT 
                    m.id, m.code, m.name, mc.name as category_name, m.requires_prescription, m.unit, m.sell_price, 
                    m.active_ingredient, m.manufacturer,
                    (SELECT il.lot_number FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_lot, 
                    (SELECT il.expiry_date FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_expiry, 
                    COALESCE((SELECT SUM(il.quantity_remaining) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0), 0) as total_qty, 
                    COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= CURDATE()), 0) as expired_count, 
                    COALESCE((SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)), 0) as near_expired_count,
                    m.min_stock_threshold 
                FROM (%s) page_ids 
                JOIN medicines m ON page_ids.id = m.id 
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id 
                ORDER BY m.id ASC
            """, pageIdsSql);

            NativeQuery<Object[]> dataQuery = session.createNativeQuery(dataSql, Object[].class);
            if (hasKeyword) {
                dataQuery.setParameter("keyword", "%" + searchKeyword.trim() + "%");
            }
            dataQuery.setParameter("limit", size);
            dataQuery.setParameter("offset", (page - 1) * size);

            List<Object[]> rows = dataQuery.list();
            List<MedicineInventoryDTO> items = mapRowsToDTO(rows);

            return new PageResult<>(items, total, page, size);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang kho thuốc: ", e);
            return PageResult.empty(page, size);
        }
    }

    private String buildFilterConditions(String stockFilter, String expiryFilter) {
        StringBuilder sb = new StringBuilder();

        // 1. Lọc theo Mức tồn kho (Hỗ trợ multi-select: ví dụ "Sắp hết hàng,Hết hàng")
        if (stockFilter != null && !stockFilter.isBlank() && !"Tất cả".equalsIgnoreCase(stockFilter)) {
            List<String> conditions = new ArrayList<>();
            for (String tok : stockFilter.split(",")) {
                String trimmed = tok.trim();
                if ("Hết hàng".equalsIgnoreCase(trimmed)) {
                    conditions.add(" NOT EXISTS (SELECT 1 FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0) ");
                } else if ("Sắp hết hàng".equalsIgnoreCase(trimmed)) {
                    conditions.add(" (EXISTS (SELECT 1 FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0) " +
                            " AND (SELECT SUM(il.quantity_remaining) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0) <= COALESCE(NULLIF(m.min_stock_threshold, 0), 10)) ");
                } else if ("Bình thường".equalsIgnoreCase(trimmed)) {
                    conditions.add(" (SELECT COALESCE(SUM(il.quantity_remaining), 0) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0) > COALESCE(NULLIF(m.min_stock_threshold, 0), 10) ");
                }
            }
            if (!conditions.isEmpty()) {
                sb.append(" AND (").append(String.join(" OR ", conditions)).append(") ");
            }
        }

        // 2. Lọc theo Hạn dùng (Hỗ trợ multi-select: ví dụ "Có lô cận hạn,Có lô hết hạn")
        if (expiryFilter != null && !expiryFilter.isBlank() && !"Tất cả".equalsIgnoreCase(expiryFilter)) {
            List<String> conditions = new ArrayList<>();
            for (String tok : expiryFilter.split(",")) {
                String trimmed = tok.trim();
                if ("Có lô hết hạn".equalsIgnoreCase(trimmed)) {
                    conditions.add(" EXISTS (SELECT 1 FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= CURDATE()) ");
                } else if ("Có lô cận hạn".equalsIgnoreCase(trimmed)) {
                    conditions.add(" EXISTS (SELECT 1 FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)) ");
                } else if ("Còn hạn".equalsIgnoreCase(trimmed)) {
                    conditions.add(" NOT EXISTS (SELECT 1 FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)) ");
                }
            }
            if (!conditions.isEmpty()) {
                sb.append(" AND (").append(String.join(" OR ", conditions)).append(") ");
            }
        }

        return sb.toString();
    }

    /**
     * Lấy danh sách kho có hỗ trợ tìm kiếm và phân trang
     * (Sử dụng Native SQL để tối ưu truy vấn GROUP BY và Sub-query)
     */
    public List<MedicineInventoryDTO> getInventoryPage(int page, int size, String searchKeyword) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            String sql = """
                SELECT 
                    m.id, m.code, m.name, mc.name as category_name, m.requires_prescription, m.unit, m.sell_price, 
                    m.active_ingredient, m.manufacturer,
                    (SELECT il.lot_number FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_lot, 
                    (SELECT il.expiry_date FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 ORDER BY il.expiry_date ASC LIMIT 1) as nearest_expiry, 
                    (SELECT SUM(il.quantity_remaining) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0) as total_qty, 
                    (SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date <= CURDATE()) as expired_count, 
                    (SELECT COUNT(*) FROM inventory_lots il WHERE il.medicine_id = m.id AND il.quantity_remaining > 0 AND il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL 90 DAY)) as near_expired_count,
                    m.min_stock_threshold 
                FROM medicines m 
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id 
                WHERE m.status = 'ACTIVE' 
                AND (m.code LIKE :keyword OR m.name LIKE :keyword OR m.active_ingredient LIKE :keyword) 
                ORDER BY m.id ASC
            """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);

            query.setParameter("keyword", "%" + (searchKeyword == null ? "" : searchKeyword.trim()) + "%");

            query.setFirstResult((page - 1) * size);
            query.setMaxResults(size);

            List<Object[]> rows = query.list();
            return mapRowsToDTO(rows);
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách kho: ", e);
            return Collections.emptyList();
        }
    }

    private List<MedicineInventoryDTO> mapRowsToDTO(List<Object[]> rows) {
        List<MedicineInventoryDTO> resultList = new ArrayList<>();
        if (rows == null) return resultList;

        // Ánh xạ mảng Object[] sang class DTO
        for (Object[] row : rows) {
            MedicineInventoryDTO dto = new MedicineInventoryDTO();

            dto.setMedicineId(row[0] != null ? ((Number) row[0]).intValue() : null);
            dto.setMedicineCode((String) row[1]);
            dto.setMedicineName((String) row[2]);
            dto.setCategoryName((String) row[3]);

            Boolean reqPrescription = false;
            if (row[4] instanceof Boolean b) {
                reqPrescription = b;
            } else if (row[4] instanceof Number n) {
                reqPrescription = n.intValue() == 1;
            }
            dto.setRequiresPrescription(reqPrescription);

            dto.setUnit((String) row[5]);

            if (row[6] instanceof BigDecimal bd) {
                dto.setSellPrice(bd);
            } else if (row[6] instanceof Number num) {
                dto.setSellPrice(BigDecimal.valueOf(num.doubleValue()));
            }

            dto.setActiveIngredient((String) row[7]);
            dto.setManufacturer((String) row[8]);

            dto.setNearestLotNumber((String) row[9]);

            if (row[10] instanceof LocalDate ld) {
                dto.setNearestExpiryDate(ld);
            } else if (row[10] instanceof java.sql.Date sd) {
                dto.setNearestExpiryDate(sd.toLocalDate());
            }

            Number totalQty = (Number) row[11];
            int qty = totalQty != null ? totalQty.intValue() : 0;
            dto.setTotalQuantity(qty);

            // Xác định trạng thái kho dựa vào kết quả đếm (COUNT) và ngưỡng tồn kho
            Number expiredCount = (Number) row[12];
            Number nearExpiredCount = (Number) row[13];
            int threshold = 10;
            if (row.length > 14 && row[14] instanceof Number n) {
                int val = n.intValue();
                if (val > 0) threshold = val;
                dto.setMinStockThreshold(val);
            }

            // 1. Phân loại Mức tồn kho (StockStatus)
            if (qty == 0) {
                dto.setStockStatus(MedicineInventoryDTO.StockStatus.OUT_OF_STOCK);
            } else if (qty <= threshold) {
                dto.setStockStatus(MedicineInventoryDTO.StockStatus.LOW_STOCK);
            } else {
                dto.setStockStatus(MedicineInventoryDTO.StockStatus.NORMAL);
            }

            // 2. Phân loại Hạn dùng (ExpiryStatus)
            if (expiredCount != null && expiredCount.intValue() > 0) {
                dto.setExpiryStatus(MedicineInventoryDTO.ExpiryStatus.EXPIRED);
            } else if (nearExpiredCount != null && nearExpiredCount.intValue() > 0) {
                dto.setExpiryStatus(MedicineInventoryDTO.ExpiryStatus.NEAR_EXPIRY);
            } else {
                dto.setExpiryStatus(MedicineInventoryDTO.ExpiryStatus.NORMAL);
            }

            // 3. Trạng thái tổng hợp tương thích ngược (Legacy InventoryStatus)
            if (dto.getStockStatus() == MedicineInventoryDTO.StockStatus.OUT_OF_STOCK) {
                dto.setStatus(MedicineInventoryDTO.InventoryStatus.OUT_OF_STOCK);
            } else if (dto.getExpiryStatus() == MedicineInventoryDTO.ExpiryStatus.EXPIRED) {
                dto.setStatus(MedicineInventoryDTO.InventoryStatus.HAS_EXPIRED_LOT);
            } else if (dto.getStockStatus() == MedicineInventoryDTO.StockStatus.LOW_STOCK) {
                dto.setStatus(MedicineInventoryDTO.InventoryStatus.LOW_STOCK);
            } else if (dto.getExpiryStatus() == MedicineInventoryDTO.ExpiryStatus.NEAR_EXPIRY) {
                dto.setStatus(MedicineInventoryDTO.InventoryStatus.HAS_NEAR_EXPIRY_LOT);
            } else {
                dto.setStatus(MedicineInventoryDTO.InventoryStatus.NORMAL);
            }

            resultList.add(dto);
        }
        return resultList;
    }

    /**
     * Cập nhật thông tin thuốc (Tên thuốc, Hoạt chất chính, Nhà sản xuất, Giá bán)
     */
    public boolean updateMedicine(int medicineId, String name, String activeIngredient, String manufacturer, BigDecimal sellPrice) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String sql = """
                UPDATE medicines 
                SET name = :name, 
                    active_ingredient = :activeIngredient, 
                    manufacturer = :manufacturer, 
                    sell_price = :sellPrice 
                WHERE id = :id
            """;
            int updated = session.createNativeQuery(sql)
                    .setParameter("name", name)
                    .setParameter("activeIngredient", activeIngredient)
                    .setParameter("manufacturer", manufacturer)
                    .setParameter("sellPrice", sellPrice)
                    .setParameter("id", medicineId)
                    .executeUpdate();
            tx.commit();
            return updated > 0;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("Lỗi khi cập nhật thông tin thuốc ID {}: ", medicineId, e);
            return false;
        }
    }

    /**
     * Đếm tổng số lượng thuốc thỏa mãn điều kiện tìm kiếm để làm phân trang
     */
    public long countTotalInventory(String searchKeyword) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT COUNT(m.id) FROM medicines m 
                WHERE m.status = 'ACTIVE' 
                AND (m.code LIKE :keyword OR m.name LIKE :keyword)
            """;

            var query = session.createNativeQuery(sql);
            query.setParameter("keyword", "%" + (searchKeyword == null ? "" : searchKeyword.trim()) + "%");

            Object count = query.uniqueResult();
            return count != null ? ((Number) count).longValue() : 0L;

        } catch (Exception e) {
            log.error("Lỗi khi đếm tổng danh sách kho: ", e);
            return 0L;
        }
    }

    /**
     * Lấy danh sách các lô hàng của một loại thuốc theo ID (FEFO: HSD tăng dần)
     */
    public List<MedicineLotDTO> getLotsByMedicineId(int medicineId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT 
                    il.id, 
                    il.medicine_id, 
                    il.lot_number, 
                    il.expiry_date, 
                    il.import_price, 
                    il.quantity_in, 
                    il.quantity_remaining, 
                    it.transaction_at, 
                    it.type, 
                    s.name as supplier_name, 
                    it.note,
                    m.manufacturer
                FROM inventory_lots il 
                JOIN medicines m ON il.medicine_id = m.id
                LEFT JOIN inventory_transactions it ON il.transaction_id = it.id 
                LEFT JOIN suppliers s ON it.supplier_id = s.id 
                WHERE il.medicine_id = :medicineId 
                ORDER BY il.expiry_date ASC, il.id ASC
            """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            query.setParameter("medicineId", medicineId);

            List<Object[]> rows = query.list();
            List<MedicineLotDTO> list = new ArrayList<>();
            for (Object[] row : rows) {
                MedicineLotDTO dto = new MedicineLotDTO();
                dto.setId(row[0] != null ? ((Number) row[0]).longValue() : null);
                dto.setMedicineId(row[1] != null ? ((Number) row[1]).intValue() : null);
                dto.setLotNumber((String) row[2]);

                if (row[3] instanceof LocalDate ld) {
                    dto.setExpiryDate(ld);
                } else if (row[3] instanceof java.sql.Date sd) {
                    dto.setExpiryDate(sd.toLocalDate());
                }

                if (row[4] instanceof BigDecimal bd) {
                    dto.setImportPrice(bd);
                } else if (row[4] instanceof Number num) {
                    dto.setImportPrice(BigDecimal.valueOf(num.doubleValue()));
                }

                dto.setQuantityIn(row[5] != null ? ((Number) row[5]).intValue() : 0);
                dto.setQuantityRemaining(row[6] != null ? ((Number) row[6]).intValue() : 0);

                if (row[7] instanceof LocalDateTime ldt) {
                    dto.setTransactionAt(ldt);
                } else if (row[7] instanceof java.sql.Timestamp ts) {
                    dto.setTransactionAt(ts.toLocalDateTime());
                }

                dto.setTransactionType(row[8] != null ? row[8].toString() : "");
                dto.setSupplierName((String) row[9]);
                dto.setNote((String) row[10]);
                dto.setManufacturer((String) row[11]);

                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách lô của thuốc ID {}: ", medicineId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy toàn bộ danh sách tên nhà cung cấp đã có trong hệ thống
     */
    public List<String> getAllSupplierNames() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            NativeQuery<String> query = session.createNativeQuery(
                    "SELECT name FROM suppliers ORDER BY name ASC", String.class);
            return query.list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách nhà cung cấp: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy toàn bộ danh sách tên nhóm thuốc đã có trong hệ thống
     */
    public List<String> getAllCategoryNames() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            NativeQuery<String> query = session.createNativeQuery(
                    "SELECT name FROM medicine_categories ORDER BY name ASC", String.class);
            return query.list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách nhóm thuốc: ", e);
            return Collections.emptyList();
        }
    }

    public boolean existsMedicineByName(String name) {
        if (name == null || name.isBlank()) return false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createNativeQuery(
                    "SELECT COUNT(*) FROM medicines WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))", Long.class)
                    .setParameter("name", name.trim())
                    .uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi kiểm tra tên thuốc tồn tại: ", e);
            return false;
        }
    }

    public boolean existsMedicineById(Integer id) {
        if (id == null || id <= 0) return false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createNativeQuery(
                    "SELECT COUNT(*) FROM medicines WHERE id = :id", Long.class)
                    .setParameter("id", id)
                    .uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi kiểm tra ID thuốc tồn tại: ", e);
            return false;
        }
    }

    public boolean existsMedicineByCode(String code) {
        if (code == null || code.isBlank()) return false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createNativeQuery(
                    "SELECT COUNT(*) FROM medicines WHERE code = :code", Long.class)
                    .setParameter("code", code.trim())
                    .uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi kiểm tra mã thuốc tồn tại: ", e);
            return false;
        }
    }

    public Integer getLotRemainingQuantity(long lotId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery(
                    "SELECT quantity_remaining FROM inventory_lots WHERE id = :lotId", Integer.class)
                    .setParameter("lotId", lotId)
                    .uniqueResult();
        } catch (Exception e) {
            log.error("Lỗi lấy số lượng tồn của lô: ", e);
            return null;
        }
    }

    /**
     * Tự động sinh mã thuốc tiếp theo theo định dạng MEDxxx (ví dụ: MED201)
     */
    public String getNextMedicineCode() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            NativeQuery<String> query = session.createNativeQuery(
                    "SELECT code FROM medicines WHERE code REGEXP '^MED[0-9]+$' ORDER BY CAST(SUBSTRING(code, 4) AS UNSIGNED) DESC LIMIT 1",
                    String.class);
            String lastCode = query.uniqueResult();
            if (lastCode != null && lastCode.startsWith("MED")) {
                try {
                    int num = Integer.parseInt(lastCode.substring(3));
                    return String.format("MED%03d", num + 1);
                } catch (NumberFormatException ignored) {}
            }
            return "MED001";
        } catch (Exception e) {
            log.error("Lỗi khi sinh mã thuốc tiếp theo: ", e);
            return "MED001";
        }
    }

    /**
     * Thực hiện nghiệp vụ NHẬP KHO:
     * - Nếu là thuốc mới: lưu thông tin thuốc vào bảng medicines
     * - Tạo giao dịch kho (inventory_transactions) loại IMPORT
     * - Tạo lô hàng mới (inventory_lots) với số lượng và hạn sử dụng
     */
    public boolean importStock(
            Integer existingMedicineId,
            String newCode, String newName, String newActiveIngredient,
            String newCategoryName, String newManufacturer, BigDecimal newSellPrice,
            boolean newRequiresPrescription,
            String lotNumber, String supplierName, LocalDate expiryDate,
            int quantityIn, String unit, BigDecimal importPrice, String note,
            Integer employeeId
    ) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            int medicineId;
            if (existingMedicineId != null && existingMedicineId > 0) {
                medicineId = existingMedicineId;
            } else {
                // 1. Tạo nhóm thuốc nếu chưa có
                Integer categoryId = null;
                if (newCategoryName != null && !newCategoryName.isBlank()) {
                    var catQuery = session.createNativeQuery(
                            "SELECT id FROM medicine_categories WHERE name = :name LIMIT 1", Integer.class);
                    catQuery.setParameter("name", newCategoryName.trim());
                    categoryId = catQuery.uniqueResult();
                    if (categoryId == null) {
                        session.createNativeQuery("INSERT INTO medicine_categories (name) VALUES (:name)")
                                .setParameter("name", newCategoryName.trim())
                                .executeUpdate();
                        categoryId = session.createNativeQuery(
                                "SELECT id FROM medicine_categories WHERE name = :name LIMIT 1", Integer.class)
                                .setParameter("name", newCategoryName.trim())
                                .uniqueResult();
                    }
                }

                // 2. Lưu loại thuốc mới vào bảng medicines
                String insertMedSql = """
                    INSERT INTO medicines (code, name, active_ingredient, unit, category_id, manufacturer, sell_price, requires_prescription, min_stock_threshold, status)
                    VALUES (:code, :name, :activeIngredient, :unit, :categoryId, :manufacturer, :sellPrice, :requiresPrescription, 0, 'ACTIVE')
                """;
                session.createNativeQuery(insertMedSql)
                        .setParameter("code", newCode.trim())
                        .setParameter("name", newName.trim())
                        .setParameter("activeIngredient", newActiveIngredient != null ? newActiveIngredient.trim() : null)
                        .setParameter("unit", unit != null ? unit.trim() : "Hộp")
                        .setParameter("categoryId", categoryId)
                        .setParameter("manufacturer", newManufacturer != null ? newManufacturer.trim() : null)
                        .setParameter("sellPrice", newSellPrice != null ? newSellPrice : BigDecimal.ZERO)
                        .setParameter("requiresPrescription", newRequiresPrescription)
                        .executeUpdate();

                medicineId = session.createNativeQuery("SELECT id FROM medicines WHERE code = :code LIMIT 1", Integer.class)
                        .setParameter("code", newCode.trim())
                        .uniqueResult();
            }

            // 3. Tìm hoặc tạo Nhà cung cấp
            Integer supplierId = null;
            if (supplierName != null && !supplierName.isBlank()) {
                var supQuery = session.createNativeQuery(
                        "SELECT id FROM suppliers WHERE name = :name LIMIT 1", Integer.class);
                supQuery.setParameter("name", supplierName.trim());
                supplierId = supQuery.uniqueResult();
                if (supplierId == null) {
                    session.createNativeQuery("INSERT INTO suppliers (name) VALUES (:name)")
                            .setParameter("name", supplierName.trim())
                            .executeUpdate();
                    supplierId = session.createNativeQuery(
                            "SELECT id FROM suppliers WHERE name = :name LIMIT 1", Integer.class)
                            .setParameter("name", supplierName.trim())
                            .uniqueResult();
                }
            }

            // 4. Tìm ID nhân viên thực hiện
            int validEmployeeId = employeeId != null ? employeeId : 1;
            var empCheck = session.createNativeQuery("SELECT id FROM employees WHERE id = :id LIMIT 1", Integer.class)
                    .setParameter("id", validEmployeeId).uniqueResult();
            if (empCheck == null) {
                validEmployeeId = session.createNativeQuery("SELECT id FROM employees ORDER BY id ASC LIMIT 1", Integer.class).uniqueResult();
            }

            // 5. Lưu giao dịch nhập kho
            String insertTxSql = """
                INSERT INTO inventory_transactions (type, supplier_id, employee_id, transaction_at, note)
                VALUES ('IMPORT', :supplierId, :employeeId, NOW(), :note)
            """;
            session.createNativeQuery(insertTxSql)
                    .setParameter("supplierId", supplierId)
                    .setParameter("employeeId", validEmployeeId)
                    .setParameter("note", note != null && !note.isBlank() ? note.trim() : "Nhập kho thuốc")
                    .executeUpdate();

            Long transactionId = ((Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult()).longValue();

            // 6. Lưu lô thuốc vào inventory_lots
            String insertLotSql = """
                INSERT INTO inventory_lots (medicine_id, transaction_id, lot_number, expiry_date, import_price, quantity_in, quantity_remaining)
                VALUES (:medicineId, :transactionId, :lotNumber, :expiryDate, :importPrice, :quantityIn, :quantityIn)
            """;
            session.createNativeQuery(insertLotSql)
                    .setParameter("medicineId", medicineId)
                    .setParameter("transactionId", transactionId)
                    .setParameter("lotNumber", lotNumber.trim())
                    .setParameter("expiryDate", expiryDate)
                    .setParameter("importPrice", importPrice != null ? importPrice : BigDecimal.ZERO)
                    .setParameter("quantityIn", quantityIn)
                    .executeUpdate();

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("Lỗi khi thực hiện giao dịch Nhập kho: ", e);
            throw new RuntimeException("Lỗi nhập kho: " + e.getMessage(), e);
        }
    }

    /**
     * Thực hiện nghiệp vụ LẬP PHIẾU NHẬP KHO HÀNG LOẠT (Một phiếu gồm nhiều mặt hàng/lô thuốc):
     * - Tạo hoặc tìm nhà cung cấp
     * - Tạo đúng 1 giao dịch kho inventory_transactions (type = 'IMPORT')
     * - Với mỗi mặt hàng trong phiếu:
     *   + Nếu là thuốc mới: Tạo nhóm (nếu chưa có) và tạo thuốc mới trong bảng medicines
     *   + Tạo lô thuốc inventory_lots gắn với transactionId vừa tạo
     */
    public boolean importBatchStock(
            String supplierName,
            Integer employeeId,
            String note,
            List<com.pharmacy.inventory.dto.BatchImportItemDTO> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng nhập kho không được rỗng!");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // 1. Tìm hoặc tạo Nhà cung cấp
            Integer supplierId = null;
            if (supplierName != null && !supplierName.isBlank()) {
                var supQuery = session.createNativeQuery(
                        "SELECT id FROM suppliers WHERE name = :name LIMIT 1", Integer.class);
                supQuery.setParameter("name", supplierName.trim());
                supplierId = supQuery.uniqueResult();
                if (supplierId == null) {
                    session.createNativeQuery("INSERT INTO suppliers (name) VALUES (:name)")
                            .setParameter("name", supplierName.trim())
                            .executeUpdate();
                    supplierId = session.createNativeQuery(
                            "SELECT id FROM suppliers WHERE name = :name LIMIT 1", Integer.class)
                            .setParameter("name", supplierName.trim())
                            .uniqueResult();
                }
            }

            // 2. Tìm ID nhân viên thực hiện
            int validEmployeeId = employeeId != null ? employeeId : 1;
            var empCheck = session.createNativeQuery("SELECT id FROM employees WHERE id = :id LIMIT 1", Integer.class)
                    .setParameter("id", validEmployeeId).uniqueResult();
            if (empCheck == null) {
                validEmployeeId = session.createNativeQuery("SELECT id FROM employees ORDER BY id ASC LIMIT 1", Integer.class).uniqueResult();
            }

            // 3. Lưu đúng 1 giao dịch nhập kho trong inventory_transactions
            String insertTxSql = """
                INSERT INTO inventory_transactions (type, supplier_id, employee_id, transaction_at, note)
                VALUES ('IMPORT', :supplierId, :employeeId, NOW(), :note)
            """;
            session.createNativeQuery(insertTxSql)
                    .setParameter("supplierId", supplierId)
                    .setParameter("employeeId", validEmployeeId)
                    .setParameter("note", note != null && !note.isBlank() ? note.trim() : "Nhập kho theo đợt")
                    .executeUpdate();

            Long transactionId = ((Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult()).longValue();

            // 4. Lưu từng mặt hàng/lô thuốc vào inventory_lots
            for (com.pharmacy.inventory.dto.BatchImportItemDTO item : items) {
                int medicineId;
                if (!item.isNewMedicine() && item.getExistingMedicineId() != null && item.getExistingMedicineId() > 0) {
                    medicineId = item.getExistingMedicineId();
                } else {
                    // Thuốc mới: Tạo nhóm thuốc nếu chưa có
                    Integer categoryId = null;
                    if (item.getCategoryName() != null && !item.getCategoryName().isBlank()) {
                        var catQuery = session.createNativeQuery(
                                "SELECT id FROM medicine_categories WHERE name = :name LIMIT 1", Integer.class);
                        catQuery.setParameter("name", item.getCategoryName().trim());
                        categoryId = catQuery.uniqueResult();
                        if (categoryId == null) {
                            session.createNativeQuery("INSERT INTO medicine_categories (name) VALUES (:name)")
                                    .setParameter("name", item.getCategoryName().trim())
                                    .executeUpdate();
                            categoryId = session.createNativeQuery(
                                    "SELECT id FROM medicine_categories WHERE name = :name LIMIT 1", Integer.class)
                                    .setParameter("name", item.getCategoryName().trim())
                                    .uniqueResult();
                        }
                    }

                    // Tự động sinh mã nếu chưa có hoặc là "Tự động"
                    String code = item.getMedicineCode();
                    if (code == null || code.isBlank() || "Tự động".equalsIgnoreCase(code)) {
                        code = getNextMedicineCode();
                    } else {
                        // Kiểm tra an toàn xem mã này đã bị thuốc khác trong DB chiếm chưa
                        var existCheck = session.createNativeQuery("SELECT id FROM medicines WHERE code = :code LIMIT 1", Integer.class)
                                .setParameter("code", code.trim()).uniqueResult();
                        if (existCheck != null) {
                            code = getNextMedicineCode();
                        }
                    }

                    String insertMedSql = """
                        INSERT INTO medicines (code, name, active_ingredient, unit, category_id, manufacturer, sell_price, requires_prescription, min_stock_threshold, status)
                        VALUES (:code, :name, :activeIngredient, :unit, :categoryId, :manufacturer, :sellPrice, :requiresPrescription, 0, 'ACTIVE')
                    """;
                    session.createNativeQuery(insertMedSql)
                            .setParameter("code", code.trim())
                            .setParameter("name", item.getMedicineName().trim())
                            .setParameter("activeIngredient", item.getActiveIngredient() != null ? item.getActiveIngredient().trim() : null)
                            .setParameter("unit", item.getUnit() != null ? item.getUnit().trim() : "Hộp")
                            .setParameter("categoryId", categoryId)
                            .setParameter("manufacturer", item.getManufacturer() != null ? item.getManufacturer().trim() : null)
                            .setParameter("sellPrice", item.getSellPrice() != null ? item.getSellPrice() : BigDecimal.ZERO)
                            .setParameter("requiresPrescription", item.isRequiresPrescription())
                            .executeUpdate();

                    medicineId = session.createNativeQuery("SELECT id FROM medicines WHERE code = :code LIMIT 1", Integer.class)
                            .setParameter("code", code.trim())
                            .uniqueResult();
                }

                // Lưu lô hàng gắn với transactionId
                String insertLotSql = """
                    INSERT INTO inventory_lots (medicine_id, transaction_id, lot_number, expiry_date, import_price, quantity_in, quantity_remaining)
                    VALUES (:medicineId, :transactionId, :lotNumber, :expiryDate, :importPrice, :quantityIn, :quantityIn)
                """;
                session.createNativeQuery(insertLotSql)
                        .setParameter("medicineId", medicineId)
                        .setParameter("transactionId", transactionId)
                        .setParameter("lotNumber", item.getLotNumber().trim())
                        .setParameter("expiryDate", item.getExpiryDate())
                        .setParameter("importPrice", item.getImportPrice() != null ? item.getImportPrice() : BigDecimal.ZERO)
                        .setParameter("quantityIn", item.getQuantityIn())
                        .executeUpdate();
            }

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("Lỗi khi thực hiện giao dịch Nhập kho hàng loạt: ", e);
            throw new RuntimeException("Lỗi nhập kho hàng loạt: " + e.getMessage(), e);
        }
    }

    /**
     * Thực hiện nghiệp vụ XUẤT KHO / XUẤT HỦY:
     * - Kiểm tra tồn kho của lô chỉ định
     * - Lưu giao dịch EXPORT_DISPOSAL
     * - Lưu chi tiết lý do hủy vào inventory_disposal_items
     * - Trừ số lượng tồn kho quantity_remaining của lô tương ứng
     */
    public boolean exportDisposalStock(
            int medicineId,
            long lotId,
            int quantity,
            String reason,
            String note,
            Integer employeeId
    ) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // 1. Kiểm tra số lượng tồn của lô
            var lotQty = session.createNativeQuery(
                    "SELECT quantity_remaining FROM inventory_lots WHERE id = :lotId FOR UPDATE", Integer.class)
                    .setParameter("lotId", lotId)
                    .uniqueResult();
            if (lotQty == null) {
                throw new IllegalArgumentException("Không tìm thấy thông tin lô hàng (ID: " + lotId + ") trong kho.");
            }
            if (lotQty < quantity) {
                throw new IllegalArgumentException("Số lượng xuất (" + quantity + ") vượt quá tồn khả dụng của lô (" + lotQty + ")!");
            }

            // 2. Tìm nhân viên hợp lệ
            int validEmployeeId = employeeId != null ? employeeId : 1;
            var empCheck = session.createNativeQuery("SELECT id FROM employees WHERE id = :id LIMIT 1", Integer.class)
                    .setParameter("id", validEmployeeId).uniqueResult();
            if (empCheck == null) {
                validEmployeeId = session.createNativeQuery("SELECT id FROM employees ORDER BY id ASC LIMIT 1", Integer.class).uniqueResult();
            }

            // 3. Lưu giao dịch kho loại EXPORT_DISPOSAL
            String fullNote = (reason != null ? reason : "") + (note != null && !note.isBlank() ? " - " + note.trim() : "");
            String insertTxSql = """
                INSERT INTO inventory_transactions (type, employee_id, transaction_at, note)
                VALUES ('EXPORT_DISPOSAL', :employeeId, NOW(), :note)
            """;
            session.createNativeQuery(insertTxSql)
                    .setParameter("employeeId", validEmployeeId)
                    .setParameter("note", fullNote)
                    .executeUpdate();

            Long transactionId = ((Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult()).longValue();

            // 4. Lưu chi tiết lý do xuất hủy
            String insertDisposalSql = """
                INSERT INTO inventory_disposal_items (transaction_id, inventory_lot_id, quantity, reason)
                VALUES (:transactionId, :lotId, :quantity, :reason)
            """;
            session.createNativeQuery(insertDisposalSql)
                    .setParameter("transactionId", transactionId)
                    .setParameter("lotId", lotId)
                    .setParameter("quantity", quantity)
                    .setParameter("reason", reason != null ? reason : "Xuất hủy")
                    .executeUpdate();

            // 5. Cập nhật trừ tồn kho của lô
            String updateLotSql = """
                UPDATE inventory_lots 
                SET quantity_remaining = quantity_remaining - :quantity 
                WHERE id = :lotId
            """;
            session.createNativeQuery(updateLotSql)
                    .setParameter("quantity", quantity)
                    .setParameter("lotId", lotId)
                    .executeUpdate();

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("Lỗi khi thực hiện giao dịch Xuất kho: ", e);
            throw new RuntimeException("Lỗi xuất kho: " + e.getMessage(), e);
        }
    }

    /**
     * Thực hiện nghiệp vụ XUẤT KHO / XUẤT HỦY HÀNG LOẠT:
     * - Lưu 1 giao dịch EXPORT_DISPOSAL trong inventory_transactions
     * - Lưu danh sách chi tiết các mặt hàng xuất hủy vào inventory_disposal_items
     * - Trừ số lượng tồn kho quantity_remaining của từng lô tương ứng
     */
    public boolean exportBatchStock(
            String reason,
            Integer employeeId,
            String note,
            List<com.pharmacy.inventory.dto.BatchExportItemDTO> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng xuất kho không được rỗng!");
        }

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // 1. Tìm nhân viên hợp lệ
            int validEmployeeId = employeeId != null ? employeeId : 1;
            var empCheck = session.createNativeQuery("SELECT id FROM employees WHERE id = :id LIMIT 1", Integer.class)
                    .setParameter("id", validEmployeeId).uniqueResult();
            if (empCheck == null) {
                validEmployeeId = session.createNativeQuery("SELECT id FROM employees ORDER BY id ASC LIMIT 1", Integer.class).uniqueResult();
            }

            // 2. Lưu giao dịch kho loại EXPORT_DISPOSAL
            String fullNote = (reason != null ? reason : "") + (note != null && !note.isBlank() ? " - " + note.trim() : "");
            String insertTxSql = """
                INSERT INTO inventory_transactions (type, employee_id, transaction_at, note)
                VALUES ('EXPORT_DISPOSAL', :employeeId, NOW(), :note)
            """;
            session.createNativeQuery(insertTxSql)
                    .setParameter("employeeId", validEmployeeId)
                    .setParameter("note", fullNote)
                    .executeUpdate();

            Long transactionId = ((Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").uniqueResult()).longValue();

            // 3. Xử lý từng mặt hàng xuất kho
            for (com.pharmacy.inventory.dto.BatchExportItemDTO item : items) {
                // Kiểm tra số lượng tồn của lô
                var lotQty = session.createNativeQuery(
                        "SELECT quantity_remaining FROM inventory_lots WHERE id = :lotId FOR UPDATE", Integer.class)
                        .setParameter("lotId", item.getLotId())
                        .uniqueResult();
                if (lotQty == null) {
                    throw new IllegalArgumentException("Không tìm thấy thông tin lô hàng " + item.getLotNumber() + " trong kho.");
                }
                if (lotQty < item.getExportQuantity()) {
                    throw new IllegalArgumentException("Thuốc " + item.getMedicineName() + " (Lô: " + item.getLotNumber() +
                            ") yêu cầu xuất " + item.getExportQuantity() + " nhưng chỉ còn tồn " + lotQty + "!");
                }

                String itemReason = item.getReason() != null && !item.getReason().isBlank() ? item.getReason() : reason;
                if (itemReason == null || itemReason.isBlank()) {
                    itemReason = "Xuất hủy";
                }

                // Lưu vào inventory_disposal_items
                String insertDisposalSql = """
                    INSERT INTO inventory_disposal_items (transaction_id, inventory_lot_id, quantity, reason)
                    VALUES (:transactionId, :lotId, :quantity, :reason)
                """;
                session.createNativeQuery(insertDisposalSql)
                        .setParameter("transactionId", transactionId)
                        .setParameter("lotId", item.getLotId())
                        .setParameter("quantity", item.getExportQuantity())
                        .setParameter("reason", itemReason)
                        .executeUpdate();

                // Cập nhật trừ tồn kho của lô
                String updateLotSql = """
                    UPDATE inventory_lots 
                    SET quantity_remaining = quantity_remaining - :quantity 
                    WHERE id = :lotId
                """;
                session.createNativeQuery(updateLotSql)
                        .setParameter("quantity", item.getExportQuantity())
                        .setParameter("lotId", item.getLotId())
                        .executeUpdate();
            }

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("Lỗi khi thực hiện giao dịch Xuất kho hàng loạt: ", e);
            throw new RuntimeException("Lỗi xuất kho: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy danh sách lịch sử Nhập kho (có hỗ trợ tìm kiếm và lọc khoảng ngày).
     */
    public List<InventoryTransactionDTO> getImportTransactions(String keyword, LocalDate fromDate, LocalDate toDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder sql = new StringBuilder("""
                SELECT it.id, it.type, it.transaction_at, it.note,
                       e.id as employee_id, e.full_name as employee_name,
                       s.id as supplier_id, s.name as supplier_name,
                       COALESCE((SELECT COUNT(il.id) FROM inventory_lots il WHERE il.transaction_id = it.id), 0) as item_count,
                       COALESCE((SELECT SUM(il.quantity_in) FROM inventory_lots il WHERE il.transaction_id = it.id), 0) as total_quantity,
                       COALESCE((SELECT SUM(il.quantity_in * il.import_price) FROM inventory_lots il WHERE il.transaction_id = it.id), 0) as total_amount
                FROM inventory_transactions it
                LEFT JOIN employees e ON it.employee_id = e.id
                LEFT JOIN suppliers s ON it.supplier_id = s.id
                WHERE it.type = 'IMPORT'
            """);

            if (keyword != null && !keyword.isBlank()) {
                sql.append("""
                     AND (
                        e.full_name LIKE :kw
                        OR s.name LIKE :kw
                        OR it.note LIKE :kw
                        OR it.id IN (
                            SELECT il_sub.transaction_id FROM inventory_lots il_sub
                            JOIN medicines m_sub ON il_sub.medicine_id = m_sub.id
                            WHERE m_sub.name LIKE :kw OR m_sub.code LIKE :kw OR il_sub.lot_number LIKE :kw
                        )
                     )
                """);
            }
            if (fromDate != null) {
                sql.append(" AND it.transaction_at >= :fromDt ");
            }
            if (toDate != null) {
                sql.append(" AND it.transaction_at <= :toDt ");
            }

            sql.append(" ORDER BY it.transaction_at DESC, it.id DESC ");

            NativeQuery<?> query = session.createNativeQuery(sql.toString());
            if (keyword != null && !keyword.isBlank()) {
                query.setParameter("kw", "%" + keyword.trim() + "%");
            }
            if (fromDate != null) {
                query.setParameter("fromDt", fromDate.atStartOfDay());
            }
            if (toDate != null) {
                query.setParameter("toDt", toDate.atTime(23, 59, 59));
            }

            List<?> rows = query.getResultList();
            List<InventoryTransactionDTO> result = new ArrayList<>();
            for (Object rowObj : rows) {
                Object[] row = (Object[]) rowObj;
                Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
                String type = (String) row[1];
                LocalDateTime txAt = toLocalDateTime(row[2]);
                String note = (String) row[3];
                Integer empId = row[4] != null ? ((Number) row[4]).intValue() : null;
                String empName = (String) row[5];
                Integer supId = row[6] != null ? ((Number) row[6]).intValue() : null;
                String supName = (String) row[7];
                int itemCount = row[8] != null ? ((Number) row[8]).intValue() : 0;
                int totalQty = row[9] != null ? ((Number) row[9]).intValue() : 0;
                BigDecimal totalAmt = row[10] != null ? new BigDecimal(row[10].toString()) : BigDecimal.ZERO;

                InventoryTransactionDTO dto = new InventoryTransactionDTO(
                        id, type, txAt, empId, empName, supId, supName, itemCount, totalQty, totalAmt, note
                );
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử Nhập kho: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy danh sách lịch sử Xuất kho (có hỗ trợ tìm kiếm và lọc khoảng ngày).
     */
    public List<InventoryTransactionDTO> getExportTransactions(String keyword, LocalDate fromDate, LocalDate toDate) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder sql = new StringBuilder("""
                SELECT it.id, it.type, it.transaction_at, it.note,
                       e.id as employee_id, e.full_name as employee_name,
                       CASE 
                           WHEN it.type = 'EXPORT_DISPOSAL' THEN COALESCE((SELECT COUNT(idi.id) FROM inventory_disposal_items idi WHERE idi.transaction_id = it.id), 0)
                           WHEN it.sales_order_id IS NOT NULL THEN COALESCE((SELECT COUNT(soi.id) FROM sales_order_items soi WHERE soi.sales_order_id = it.sales_order_id), 0)
                           ELSE 0
                       END as item_count,
                       CASE 
                           WHEN it.type = 'EXPORT_DISPOSAL' THEN COALESCE((SELECT SUM(idi.quantity) FROM inventory_disposal_items idi WHERE idi.transaction_id = it.id), 0)
                           WHEN it.sales_order_id IS NOT NULL THEN COALESCE((SELECT SUM(soi.quantity) FROM sales_order_items soi WHERE soi.sales_order_id = it.sales_order_id), 0)
                           ELSE 0
                       END as total_quantity,
                       CASE 
                           WHEN it.sales_order_id IS NOT NULL THEN COALESCE((SELECT SUM(soi.line_total) FROM sales_order_items soi WHERE soi.sales_order_id = it.sales_order_id), 0)
                           ELSE 0
                       END as total_amount
                FROM inventory_transactions it
                LEFT JOIN employees e ON it.employee_id = e.id
                WHERE it.type != 'IMPORT'
            """);

            if (keyword != null && !keyword.isBlank()) {
                sql.append("""
                     AND (
                        e.full_name LIKE :kw
                        OR it.note LIKE :kw
                        OR it.id IN (
                            SELECT idi_sub.transaction_id FROM inventory_disposal_items idi_sub
                            JOIN inventory_lots il_sub ON idi_sub.inventory_lot_id = il_sub.id
                            JOIN medicines m_sub ON il_sub.medicine_id = m_sub.id
                            WHERE m_sub.name LIKE :kw OR m_sub.code LIKE :kw OR il_sub.lot_number LIKE :kw OR idi_sub.reason LIKE :kw
                        )
                        OR it.sales_order_id IN (
                            SELECT soi_sub.sales_order_id FROM sales_order_items soi_sub
                            JOIN medicines m_sub2 ON soi_sub.medicine_id = m_sub2.id
                            WHERE m_sub2.name LIKE :kw OR m_sub2.code LIKE :kw
                        )
                     )
                """);
            }
            if (fromDate != null) {
                sql.append(" AND it.transaction_at >= :fromDt ");
            }
            if (toDate != null) {
                sql.append(" AND it.transaction_at <= :toDt ");
            }

            sql.append(" ORDER BY it.transaction_at DESC, it.id DESC ");

            NativeQuery<?> query = session.createNativeQuery(sql.toString());
            if (keyword != null && !keyword.isBlank()) {
                query.setParameter("kw", "%" + keyword.trim() + "%");
            }
            if (fromDate != null) {
                query.setParameter("fromDt", fromDate.atStartOfDay());
            }
            if (toDate != null) {
                query.setParameter("toDt", toDate.atTime(23, 59, 59));
            }

            List<?> rows = query.getResultList();
            List<InventoryTransactionDTO> result = new ArrayList<>();
            for (Object rowObj : rows) {
                Object[] row = (Object[]) rowObj;
                Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
                String type = (String) row[1];
                LocalDateTime txAt = toLocalDateTime(row[2]);
                String note = (String) row[3];
                Integer empId = row[4] != null ? ((Number) row[4]).intValue() : null;
                String empName = (String) row[5];
                int itemCount = row[6] != null ? ((Number) row[6]).intValue() : 0;
                int totalQty = row[7] != null ? ((Number) row[7]).intValue() : 0;
                BigDecimal totalAmt = row[8] != null ? new BigDecimal(row[8].toString()) : BigDecimal.ZERO;

                InventoryTransactionDTO dto = new InventoryTransactionDTO(
                        id, type, txAt, empId, empName, null, null, itemCount, totalQty, totalAmt, note
                );
                if ("EXPORT_DISPOSAL".equalsIgnoreCase(type)) {
                    dto.setExportTypeLabel("Xuất hủy hàng hỏng/hết hạn");
                } else if ("EXPORT_SALE".equalsIgnoreCase(type)) {
                    dto.setExportTypeLabel("Xuất bán lẻ cho khách");
                } else {
                    dto.setExportTypeLabel(dto.getTypeDisplayName());
                }
                result.add(dto);
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử Xuất kho: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy chi tiết từng mặt hàng thuốc trong một lần Nhập hoặc Xuất kho.
     */
    public List<InventoryTransactionDetailDTO> getTransactionDetails(Long transactionId, String type) {
        if (transactionId == null) return Collections.emptyList();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<InventoryTransactionDetailDTO> list = new ArrayList<>();

            if (type == null || "IMPORT".equalsIgnoreCase(type)) {
                String sql = """
                    SELECT il.id, il.transaction_id, il.medicine_id, m.code, m.name,
                           COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                           il.lot_number, il.expiry_date, il.quantity_in,
                           il.import_price, (il.quantity_in * il.import_price) as total_price,
                           '' as reason
                    FROM inventory_lots il
                    JOIN medicines m ON il.medicine_id = m.id
                    LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                    WHERE il.transaction_id = :txId
                    ORDER BY il.id ASC
                """;
                List<?> rows = session.createNativeQuery(sql)
                        .setParameter("txId", transactionId)
                        .getResultList();
                for (Object rowObj : rows) {
                    list.add(mapTransactionDetail((Object[]) rowObj));
                }
            } else if ("EXPORT_DISPOSAL".equalsIgnoreCase(type)) {
                String sql = """
                    SELECT idi.id, idi.transaction_id, il.medicine_id, m.code, m.name,
                           COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                           il.lot_number, il.expiry_date, idi.quantity,
                           il.import_price, (idi.quantity * il.import_price) as total_price,
                           COALESCE(idi.reason, 'Xuất hủy') as reason
                FROM inventory_disposal_items idi
                JOIN inventory_lots il ON idi.inventory_lot_id = il.id
                JOIN medicines m ON il.medicine_id = m.id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                WHERE idi.transaction_id = :txId
                ORDER BY idi.id ASC
            """;
                List<?> rows = session.createNativeQuery(sql)
                        .setParameter("txId", transactionId)
                        .getResultList();
                for (Object rowObj : rows) {
                    list.add(mapTransactionDetail((Object[]) rowObj));
                }
            } else {
                // EXPORT_SALE hoặc các loại khác
                String sql = """
                    SELECT soi.id, it.id as transaction_id, soi.medicine_id, m.code, m.name,
                           COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                           il.lot_number, il.expiry_date, soi.quantity,
                           soi.unit_price, soi.line_total as total_price,
                           'Xuất bán hóa đơn' as reason
                    FROM inventory_transactions it
                    JOIN sales_orders so ON it.sales_order_id = so.id
                    JOIN sales_order_items soi ON so.id = soi.sales_order_id
                    JOIN inventory_lots il ON soi.inventory_lot_id = il.id
                    JOIN medicines m ON soi.medicine_id = m.id
                    LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                    WHERE it.id = :txId
                    ORDER BY soi.id ASC
                """;
                List<?> rows = session.createNativeQuery(sql)
                        .setParameter("txId", transactionId)
                        .getResultList();
                for (Object rowObj : rows) {
                    list.add(mapTransactionDetail((Object[]) rowObj));
                }
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết giao dịch kho #{}: ", transactionId, e);
            return Collections.emptyList();
        }
    }

    private InventoryTransactionDetailDTO mapTransactionDetail(Object[] row) {
        Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
        Long txId = row[1] != null ? ((Number) row[1]).longValue() : null;
        Integer medId = row[2] != null ? ((Number) row[2]).intValue() : null;
        String code = (String) row[3];
        String name = (String) row[4];
        String cat = (String) row[5];
        String unit = (String) row[6];
        String manufacturer = (String) row[7];
        String lot = (String) row[8];
        LocalDate exp = toLocalDate(row[9]);
        int qty = row[10] != null ? ((Number) row[10]).intValue() : 0;
        BigDecimal price = row[11] != null ? new BigDecimal(row[11].toString()) : BigDecimal.ZERO;
        BigDecimal total = row[12] != null ? new BigDecimal(row[12].toString()) : BigDecimal.ZERO;
        String reason = (String) row[13];

        return new InventoryTransactionDetailDTO(
                id, txId, medId, code, name, cat, unit, manufacturer, lot, exp, qty, price, total, reason
        );
    }

    private LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDateTime ldt) return ldt;
        if (obj instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        return null;
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDate ld) return ld;
        if (obj instanceof java.sql.Date d) return d.toLocalDate();
        return null;
    }
}