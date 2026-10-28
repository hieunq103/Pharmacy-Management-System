package com.pharmacy.inventory;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.AppConstants;
import com.pharmacy.common.PageResult;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.infra.db.TransactionGuards;
import com.pharmacy.inventory.dto.BatchExportItemDTO;
import com.pharmacy.inventory.dto.BatchImportItemDTO;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDetailDTO;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;
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
import java.sql.Types;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.pharmacy.common.SqlRows.*;
import static com.pharmacy.inventory.InventorySql.*;

public class InventoryRepository {
    private static final Logger log = LoggerFactory.getLogger(InventoryRepository.class);

    public record InventorySummary(long totalMedicines, long inStockMedicines, long lowStockMedicines, long nearExpiryMedicines) {}

    // Lấy thống kê tổng quan tồn kho cho Dashboard
    public InventorySummary getInventorySummary() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT
                    COUNT(*) AS total_medicines,
                    COALESCE(SUM(ls.total_qty > 0), 0) AS instock_medicines,
                    COALESCE(SUM(COALESCE(ls.total_qty, 0) <= %s), 0) AS low_stock_medicines,
                    COALESCE(SUM(ls.near_expiry_lots > 0), 0) AS near_expiry_medicines
                FROM medicines m
                LEFT JOIN (
                    SELECT il.medicine_id,
                           SUM(il.quantity_remaining) AS total_qty,
                           SUM(il.expiry_date > CURDATE() AND il.expiry_date <= DATE_ADD(CURDATE(), INTERVAL %d DAY)) AS near_expiry_lots
                    FROM inventory_lots il
                    WHERE il.quantity_remaining > 0
                    GROUP BY il.medicine_id
                ) ls ON ls.medicine_id = m.id
                WHERE m.status = 'ACTIVE'
            """.formatted(LOW_STOCK_THRESHOLD, AppConstants.NEAR_EXPIRY_DAYS);
            Object[] row = session.createNativeQuery(sql, Object[].class).getSingleResult();
            return new InventorySummary(toLong(row[0]), toLong(row[1]), toLong(row[2]), toLong(row[3]));
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê kho: ", e);
            return new InventorySummary(0L, 0L, 0L, 0L);
        }
    }

    // Lấy danh sách cảnh báo thuốc cận hạn và sắp hết hàng
    public List<InventoryAlertDTO> getInventoryAlerts(int limit) {
        List<InventoryAlertDTO> alerts = new ArrayList<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate today = LocalDate.now();

            String expirySql = """
                SELECT m.id, m.name, il.lot_number, il.expiry_date, il.quantity_remaining, m.unit
                FROM inventory_lots il
                JOIN medicines m ON il.medicine_id = m.id
                WHERE il.expiry_date <= :maxDate
                  AND il.quantity_remaining > 0
                  AND m.status = 'ACTIVE'
                ORDER BY il.expiry_date ASC
                LIMIT :limit
            """;
            List<Object[]> expiryRows = session.createNativeQuery(expirySql, Object[].class)
                    .setParameter("maxDate", java.sql.Date.valueOf(today.plusDays(AppConstants.NEAR_EXPIRY_DAYS)))
                    .setParameter("limit", limit)
                    .list();

            for (Object[] r : expiryRows) {
                String name = toStr(r[1]);
                String lot = toStr(r[2]);
                LocalDate expDate = toLocalDate(r[3]);
                String title = name + (hasText(lot) ? " — " + lot : "");
                String subtitle;
                if (expDate == null) {
                    subtitle = "Cận hạn sử dụng";
                } else if (expDate.isBefore(today)) {
                    subtitle = "Đã hết hạn " + expDate.format(FormatUtils.DATE_FORMATTER);
                } else {
                    long daysLeft = ChronoUnit.DAYS.between(today, expDate);
                    subtitle = "Hết hạn " + expDate.format(FormatUtils.DATE_FORMATTER) + (daysLeft == 0 ? " (Hôm nay)" : " (Còn " + daysLeft + " ngày)");
                }
                alerts.add(new InventoryAlertDTO(toInt(r[0]), title, subtitle, toInt(r[4]), toStr(r[5]),
                        InventoryAlertDTO.AlertType.EXPIRY));
            }

            String lowStockSql = """
                SELECT m.id, m.name, COALESCE(ls.total_qty, 0) AS total_qty, m.unit, %s AS threshold
                FROM medicines m
                LEFT JOIN (
                    SELECT il.medicine_id, SUM(il.quantity_remaining) AS total_qty
                    FROM inventory_lots il
                    WHERE il.quantity_remaining > 0
                    GROUP BY il.medicine_id
                ) ls ON ls.medicine_id = m.id
                WHERE m.status = 'ACTIVE'
                  AND COALESCE(ls.total_qty, 0) <= %s
                ORDER BY total_qty ASC
                LIMIT :limit
            """.formatted(LOW_STOCK_THRESHOLD, LOW_STOCK_THRESHOLD);
            List<Object[]> lowStockRows = session.createNativeQuery(lowStockSql, Object[].class)
                    .setParameter("limit", limit)
                    .list();

            for (Object[] r : lowStockRows) {
                int qty = toInt(r[2]);
                String unit = toStr(r[3]);
                String subtitle = qty == 0 ? "Đã hết hàng trong kho"
                        : "Tồn kho dưới ngưỡng tối thiểu (" + qty + "/" + toInt(r[4]) + " " + unit + ")";
                alerts.add(new InventoryAlertDTO(toInt(r[0]), toStr(r[1]), subtitle, qty, unit,
                        InventoryAlertDTO.AlertType.LOW_STOCK));
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách cảnh báo kho trong InventoryRepository: ", e);
        }
        return alerts;
    }

    // Lấy danh sách thuốc rút gọn dùng cho bộ chọn thuốc
    public List<MedicineInventoryDTO> getMedicinesForPicker() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT m.id, m.code, m.name, mc.name AS category_name, m.unit, m.sell_price, m.active_ingredient, m.manufacturer,
                       COALESCE(ls.total_qty, 0), COALESCE(ls.expired_count, 0), COALESCE(ls.near_expired_count, 0)
                FROM medicines m
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                %s
                WHERE m.status = 'ACTIVE'
                ORDER BY m.name ASC
            """.formatted(LOT_STATS_ALL);
            List<Object[]> rows = session.createNativeQuery(sql, Object[].class).list();
            List<MedicineInventoryDTO> list = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                MedicineInventoryDTO dto = new MedicineInventoryDTO();
                dto.setMedicineId(toInteger(row[0]));
                dto.setMedicineCode(toStr(row[1]));
                dto.setMedicineName(toStr(row[2]));
                dto.setCategoryName(toStr(row[3]));
                dto.setUnit(toStr(row[4]));
                dto.setSellPrice(toBigDecimal(row[5]));
                dto.setActiveIngredient(toStr(row[6]));
                dto.setManufacturer(toStr(row[7]));
                dto.setTotalQuantity(toInt(row[8]));
                dto.setExpiryStatus(expiryStatus(toInt(row[9]), toInt(row[10])));
                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách thuốc cho picker: ", e);
            return Collections.emptyList();
        }
    }

    // Phân trang và tìm kiếm danh sách thuốc trong kho theo tồn kho và hạn dùng
    public PageResult<MedicineInventoryDTO> getInventoryPageResult(int page, int size, String searchKeyword, String stockFilter, String expiryFilter) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = hasText(searchKeyword);
            String filterCondition = buildFilterConditions(stockFilter, expiryFilter);

            String statsJoin = filterCondition.isEmpty() ? "" : LOT_STATS_ALL;
            String where = " WHERE m.status = 'ACTIVE' " + (hasKeyword ? KEYWORD_FILTER : "") + filterCondition;

            var countQuery = session.createNativeQuery("SELECT COUNT(*) FROM medicines m " + statsJoin + where, Long.class);
            if (hasKeyword) countQuery.setParameter("keyword", likeParam(searchKeyword));
            long total = toLong(countQuery.uniqueResult());
            if (total == 0) {
                return PageResult.empty(validPage, validSize);
            }

            String pageIdsSql = "SELECT m.id FROM medicines m " + statsJoin + where
                    + " ORDER BY m.id ASC LIMIT :limit OFFSET :offset";
            String dataSql = """
                SELECT %s
                FROM (%s) page_ids
                JOIN medicines m ON page_ids.id = m.id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                %s
                ORDER BY m.id ASC
            """.formatted(INVENTORY_COLUMNS, pageIdsSql, LOT_STATS_LATERAL);

            NativeQuery<Object[]> dataQuery = session.createNativeQuery(dataSql, Object[].class);
            if (hasKeyword) dataQuery.setParameter("keyword", likeParam(searchKeyword));
            dataQuery.setParameter("limit", validSize);
            dataQuery.setParameter("offset", (validPage - 1) * validSize);

            List<MedicineInventoryDTO> items = new ArrayList<>();
            for (Object[] row : dataQuery.list()) {
                items.add(mapInventoryRow(row));
            }
            return new PageResult<>(items, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang kho thuốc: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    // Xây dựng điều kiện WHERE lọc theo trạng thái tồn kho và hạn dùng
    private String buildFilterConditions(String stockFilter, String expiryFilter) {
        StringBuilder sb = new StringBuilder();

        if (hasText(stockFilter) && !"Tất cả".equalsIgnoreCase(stockFilter)) {
            List<String> conditions = new ArrayList<>();
            for (String tok : stockFilter.split(",")) {
                switch (tok.trim()) {
                    case "Hết hàng" -> conditions.add(" ls.total_qty IS NULL ");
                    case "Sắp hết hàng" -> conditions.add(" ls.total_qty <= " + LOW_STOCK_THRESHOLD + " ");
                    case "Bình thường" -> conditions.add(" ls.total_qty > " + LOW_STOCK_THRESHOLD + " ");
                    default -> { }
                }
            }
            if (!conditions.isEmpty()) {
                sb.append(" AND (").append(String.join(" OR ", conditions)).append(") ");
            }
        }

        if (hasText(expiryFilter) && !"Tất cả".equalsIgnoreCase(expiryFilter)) {
            List<String> conditions = new ArrayList<>();
            for (String tok : expiryFilter.split(",")) {
                switch (tok.trim()) {
                    case "Có lô hết hạn" -> conditions.add(" ls.expired_count > 0 ");
                    case "Có lô cận hạn" -> conditions.add(" ls.near_expired_count > 0 ");
                    case "Còn hạn" -> conditions.add(" COALESCE(ls.expired_count, 0) = 0 AND COALESCE(ls.near_expired_count, 0) = 0 ");
                    default -> { }
                }
            }
            if (!conditions.isEmpty()) {
                sb.append(" AND (").append(String.join(" OR ", conditions)).append(") ");
            }
        }
        return sb.toString();
    }

    static MedicineInventoryDTO mapInventoryRow(Object[] row) {
        MedicineInventoryDTO dto = new MedicineInventoryDTO();
        dto.setMedicineId(toInteger(row[0]));
        dto.setMedicineCode(toStr(row[1]));
        dto.setMedicineName(toStr(row[2]));
        dto.setCategoryName(toStr(row[3]));
        dto.setRequiresPrescription(toBoolean(row[4]));
        dto.setUnit(toStr(row[5]));
        dto.setSellPrice(toBigDecimal(row[6]));
        dto.setActiveIngredient(toStr(row[7]));
        dto.setManufacturer(toStr(row[8]));

        int qty = toInt(row[9]);
        dto.setTotalQuantity(qty);
        int minStock = toInt(row[12]);
        int threshold = minStock > 0 ? minStock : AppConstants.DEFAULT_LOW_STOCK_THRESHOLD;

        if (qty == 0) {
            dto.setStockStatus(MedicineInventoryDTO.StockStatus.OUT_OF_STOCK);
        } else if (qty <= threshold) {
            dto.setStockStatus(MedicineInventoryDTO.StockStatus.LOW_STOCK);
        } else {
            dto.setStockStatus(MedicineInventoryDTO.StockStatus.NORMAL);
        }
        dto.setExpiryStatus(expiryStatus(toInt(row[10]), toInt(row[11])));

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
        return dto;
    }

    // Xác định trạng thái hạn dùng của thuốc dựa trên số lô hết hạn và cận hạn
    private static MedicineInventoryDTO.ExpiryStatus expiryStatus(int expiredCount, int nearExpiredCount) {
        if (expiredCount > 0) return MedicineInventoryDTO.ExpiryStatus.EXPIRED;
        if (nearExpiredCount > 0) return MedicineInventoryDTO.ExpiryStatus.NEAR_EXPIRY;
        return MedicineInventoryDTO.ExpiryStatus.NORMAL;
    }

    // Cập nhật thông tin cơ bản và giá bán của thuốc
    public boolean updateMedicine(int medicineId, String name, String activeIngredient, String manufacturer, BigDecimal sellPrice) {
        try {
            return HibernateUtil.executeWithResult(session -> session.createNativeMutationQuery("""
                        UPDATE medicines
                        SET name = :name,
                            active_ingredient = :activeIngredient,
                            manufacturer = :manufacturer,
                            sell_price = :sellPrice
                        WHERE id = :id
                    """)
                    .setParameter("name", name)
                    .setParameter("activeIngredient", activeIngredient)
                    .setParameter("manufacturer", manufacturer)
                    .setParameter("sellPrice", sellPrice)
                    .setParameter("id", medicineId)
                    .executeUpdate() > 0);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật thông tin thuốc ID {}: ", medicineId, e);
            return false;
        }
    }

    // Lấy danh sách các lô thuốc còn tồn theo ID thuốc
    public List<MedicineLotDTO> getLotsByMedicineId(int medicineId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT il.id, il.medicine_id, il.lot_number, il.expiry_date, il.import_price,
                       il.quantity_in, il.quantity_remaining, it.transaction_at, it.type,
                       s.name AS supplier_name, it.note, m.manufacturer
                FROM inventory_lots il
                JOIN medicines m ON il.medicine_id = m.id
                LEFT JOIN inventory_transactions it ON il.transaction_id = it.id
                LEFT JOIN suppliers s ON it.supplier_id = s.id
                WHERE il.medicine_id = :medicineId
                ORDER BY il.expiry_date ASC, il.id ASC
            """;
            List<Object[]> rows = session.createNativeQuery(sql, Object[].class)
                    .setParameter("medicineId", medicineId)
                    .list();
            List<MedicineLotDTO> list = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                MedicineLotDTO dto = new MedicineLotDTO();
                dto.setId(toLongObj(row[0]));
                dto.setMedicineId(toInteger(row[1]));
                dto.setLotNumber(toStr(row[2]));
                dto.setExpiryDate(toLocalDate(row[3]));
                dto.setImportPrice(toBigDecimal(row[4]));
                dto.setQuantityIn(toInt(row[5]));
                dto.setQuantityRemaining(toInt(row[6]));
                dto.setTransactionAt(toLocalDateTime(row[7]));
                dto.setSupplierName(toStr(row[9]));
                dto.setNote(toStr(row[10]));
                dto.setManufacturer(toStr(row[11]));
                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách lô của thuốc ID {}: ", medicineId, e);
            return Collections.emptyList();
        }
    }

    // Lấy danh sách tên tất cả nhà cung cấp
    public List<String> getAllSupplierNames() {
        return listNames("SELECT name FROM suppliers ORDER BY name ASC", "nhà cung cấp");
    }

    // Lấy danh sách tên tất cả danh mục thuốc
    public List<String> getAllCategoryNames() {
        return listNames("SELECT name FROM medicine_categories ORDER BY name ASC", "nhóm thuốc");
    }

    // Truy vấn danh sách tên chuỗi từ CSDL
    private List<String> listNames(String sql, String label) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createNativeQuery(sql, String.class).list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách {}: ", label, e);
            return Collections.emptyList();
        }
    }

    // Kiểm tra tên thuốc đã tồn tại trong CSDL chưa
    public boolean existsMedicineByName(String name) {
        if (!hasText(name)) return false;
        return exists("SELECT COUNT(*) FROM medicines WHERE name = :v", name.trim());
    }

    // Kiểm tra thuốc có tồn tại theo ID không
    public boolean existsMedicineById(Integer id) {
        if (id == null || id <= 0) return false;
        return exists("SELECT COUNT(*) FROM medicines WHERE id = :v", id);
    }

    // Kiểm tra sự tồn tại bản ghi bằng câu lệnh COUNT
    private boolean exists(String countSql, Object value) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long count = session.createNativeQuery(countSql, Long.class).setParameter("v", value).uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi kiểm tra tồn tại [{}]: ", countSql, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu.", e);
        }
    }

    private static volatile boolean medicineSequenceReady;

    // Tạo dòng bộ đếm MEDICINE (khởi tạo từ mã MED lớn nhất hiện có) trong giao dịch riêng, ngắn,
    // để giao dịch nhập kho chỉ cần UPDATE một dòng đã có
    private static void ensureMedicineSequence() {
        if (medicineSequenceReady) return;
        HibernateUtil.execute(session -> session.doWork(conn -> {
            // Đọc MAX bằng SELECT thường (không khóa bảng medicines) rồi mới chèn, tránh deadlock với phiếu nhập đang chạy
            long lastNumber;
            try (Statement st = conn.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT 1 FROM code_sequences WHERE name = 'MEDICINE'")) {
                    if (rs.next()) return;
                }
                try (ResultSet rs = st.executeQuery(
                        "SELECT COALESCE(MAX(CAST(SUBSTRING(code, 4) AS UNSIGNED)), 0) FROM medicines WHERE code REGEXP '^MED[0-9]+$'")) {
                    rs.next();
                    lastNumber = rs.getLong(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO code_sequences (name, current_number) VALUES ('MEDICINE', ?)")) {
                ps.setLong(1, lastNumber);
                ps.executeUpdate();
            }
        }));
        medicineSequenceReady = true;
    }

    // Giữ trước count số liên tiếp cho phiếu nhập và trả về số đầu tiên. Khóa dòng bộ đếm đến khi commit nên các phiếu
    // nhập cùng lúc xếp hàng (luôn khóa đầu tiên nên không deadlock); rollback thì bộ đếm quay lại nên không mất số
    private static long reserveMedicineCodes(Connection conn, int count) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE code_sequences SET current_number = LAST_INSERT_ID(current_number + ?) WHERE name = 'MEDICINE'")) {
            ps.setInt(1, count);
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Thiếu bộ đếm mã thuốc trong bảng code_sequences");
            }
        }
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
            rs.next();
            return rs.getLong(1) - count + 1;
        }
    }

    // Thực hiện giao dịch nhập kho thuốc theo lô
    public boolean importBatchStock(String supplierName, Integer employeeId, String note, List<BatchImportItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng nhập kho không được rỗng!");
        }
        Map<BatchImportItemDTO, String> allocatedCodes = new LinkedHashMap<>();
        int newMedicineCount = (int) items.stream().filter(BatchImportItemDTO::isNewMedicine).count();
        try {
            ensureMedicineSequence();
            HibernateUtil.execute(session -> session.doWork(conn -> {
                allocatedCodes.clear();
                long[] nextCodeNumber = {reserveMedicineCodes(conn, newMedicineCount)};
                TransactionGuards.requireActiveEmployee(conn, employeeId);
                Integer supplierId = hasText(supplierName) ? findOrCreateByName(conn, "suppliers", supplierName.trim()) : null;

                long transactionId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO inventory_transactions (type, supplier_id, employee_id, transaction_at, note) VALUES ('IMPORT', ?, ?, NOW(), ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    setNullableInt(ps, 1, supplierId);
                    ps.setInt(2, employeeId);
                    ps.setString(3, hasText(note) ? note.trim() : "Nhập kho theo đợt");
                    ps.executeUpdate();
                    transactionId = generatedKey(ps);
                }

                Map<String, Integer> categoryCache = new HashMap<>();
                try (PreparedStatement lotPs = conn.prepareStatement("""
                        INSERT INTO inventory_lots (medicine_id, transaction_id, lot_number, expiry_date, import_price, quantity_in, quantity_remaining)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    for (BatchImportItemDTO item : items) {
                        int medicineId = (!item.isNewMedicine() && item.getExistingMedicineId() != null && item.getExistingMedicineId() > 0)
                                ? item.getExistingMedicineId()
                                : insertNewMedicine(conn, item, categoryCache, nextCodeNumber, allocatedCodes);
                        lotPs.setInt(1, medicineId);
                        lotPs.setLong(2, transactionId);
                        lotPs.setString(3, item.getLotNumber().trim());
                        lotPs.setObject(4, item.getExpiryDate());
                        lotPs.setBigDecimal(5, item.getImportPrice() != null ? item.getImportPrice() : BigDecimal.ZERO);
                        lotPs.setInt(6, item.getQuantityIn());
                        lotPs.setInt(7, item.getQuantityIn());
                        lotPs.addBatch();
                    }
                    lotPs.executeBatch();
                }
            }));
            // Chỉ ghi mã thật vào phiếu sau khi commit thành công
            allocatedCodes.forEach(BatchImportItemDTO::setMedicineCode);
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi thực hiện giao dịch Nhập kho hàng loạt: ", e);
            throw new RuntimeException("Lỗi nhập kho hàng loạt: " + rootMessage(e), e);
        }
    }

    // Thêm mới thông tin thuốc chưa có trong danh mục khi nhập kho
    private int insertNewMedicine(Connection conn, BatchImportItemDTO item, Map<String, Integer> categoryCache,
                                  long[] nextCodeNumber, Map<BatchImportItemDTO, String> allocatedCodes) throws SQLException {
        Integer categoryId = null;
        if (hasText(item.getCategoryName())) {
            String catName = item.getCategoryName().trim();
            categoryId = categoryCache.get(catName);
            if (categoryId == null) {
                categoryId = findOrCreateByName(conn, "medicine_categories", catName);
                categoryCache.put(catName, categoryId);
            }
        }

        String code = String.format("MED%03d", nextCodeNumber[0]++);
        allocatedCodes.put(item, code);

        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO medicines (code, name, active_ingredient, unit, category_id, manufacturer, sell_price, requires_prescription, min_stock_threshold, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0, 'ACTIVE')
                """, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code);
            ps.setString(2, item.getMedicineName().trim());
            ps.setString(3, item.getActiveIngredient() != null ? item.getActiveIngredient().trim() : null);
            ps.setString(4, item.getUnit() != null ? item.getUnit().trim() : "Hộp");
            setNullableInt(ps, 5, categoryId);
            ps.setString(6, item.getManufacturer() != null ? item.getManufacturer().trim() : null);
            ps.setBigDecimal(7, item.getSellPrice() != null ? item.getSellPrice() : BigDecimal.ZERO);
            ps.setBoolean(8, item.isRequiresPrescription());
            ps.executeUpdate();
            return (int) generatedKey(ps);
        }
    }

    // Tìm ID hoặc tạo mới bản ghi theo tên danh mục hoặc nhà cung cấp
    private static int findOrCreateByName(Connection conn, String table, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM " + table + " WHERE name = ? LIMIT 1")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        // Hai quầy cùng tạo một tên mới: cột name là UNIQUE nên người sau nhận lại id đã có thay vì báo lỗi trùng
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO " + table + " (name) VALUES (?) ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            return (int) generatedKey(ps);
        }
    }

    // Thực hiện giao dịch xuất hủy thuốc theo lô
    public boolean exportBatchStock(String reason, Integer employeeId, String note, List<BatchExportItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng xuất kho không được rỗng!");
        }
        try {
            HibernateUtil.execute(session -> session.doWork(conn -> {
                TransactionGuards.requireActiveEmployee(conn, employeeId);

                Map<Long, Integer> requested = new HashMap<>();
                for (BatchExportItemDTO item : items) {
                    requested.merge(item.getLotId(), item.getExportQuantity(), Integer::sum);
                }
                Map<Long, Integer> remaining = lockLots(conn, requested.keySet());
                for (BatchExportItemDTO item : items) {
                    Integer lotQty = remaining.get(item.getLotId());
                    if (lotQty == null) {
                        throw new IllegalArgumentException("Không tìm thấy thông tin lô hàng " + item.getLotNumber() + " trong kho.");
                    }
                    int need = requested.get(item.getLotId());
                    if (lotQty < need) {
                        throw new IllegalArgumentException("Thuốc " + item.getMedicineName() + " (Lô: " + item.getLotNumber()
                                + ") yêu cầu xuất " + need + " nhưng chỉ còn tồn " + lotQty + "!");
                    }
                }

                String fullNote = (reason != null ? reason : "") + (hasText(note) ? " - " + note.trim() : "");
                long transactionId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO inventory_transactions (type, employee_id, transaction_at, note) VALUES ('EXPORT_DISPOSAL', ?, NOW(), ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, employeeId);
                    ps.setString(2, fullNote);
                    ps.executeUpdate();
                    transactionId = generatedKey(ps);
                }

                try (PreparedStatement itemPs = conn.prepareStatement(
                        "INSERT INTO inventory_disposal_items (transaction_id, inventory_lot_id, quantity, reason) VALUES (?, ?, ?, ?)");
                     PreparedStatement lotPs = conn.prepareStatement(
                             "UPDATE inventory_lots SET quantity_remaining = quantity_remaining - ? WHERE id = ?")) {
                    for (BatchExportItemDTO item : items) {
                        String itemReason = hasText(item.getReason()) ? item.getReason() : (hasText(reason) ? reason : "Xuất hủy");
                        itemPs.setLong(1, transactionId);
                        itemPs.setLong(2, item.getLotId());
                        itemPs.setInt(3, item.getExportQuantity());
                        itemPs.setString(4, itemReason);
                        itemPs.addBatch();

                        lotPs.setInt(1, item.getExportQuantity());
                        lotPs.setLong(2, item.getLotId());
                        lotPs.addBatch();
                    }
                    itemPs.executeBatch();
                    lotPs.executeBatch();
                }
            }));
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi thực hiện giao dịch Xuất kho hàng loạt: ", e);
            throw new RuntimeException("Lỗi xuất kho: " + rootMessage(e), e);
        }
    }

    // Khóa và lấy số lượng tồn hiện tại của các lô thuốc để xuất kho
    private static Map<Long, Integer> lockLots(Connection conn, java.util.Collection<Long> lotIds) throws SQLException {
        String placeholders = String.join(",", Collections.nCopies(lotIds.size(), "?"));
        Map<Long, Integer> result = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, quantity_remaining FROM inventory_lots WHERE id IN (" + placeholders + ") FOR UPDATE")) {
            int i = 1;
            for (Long id : lotIds) ps.setLong(i++, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.put(rs.getLong(1), rs.getInt(2));
            }
        }
        return result;
    }

    static long generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("Không lấy được khóa vừa sinh.");
            return keys.getLong(1);
        }
    }

    // Gán giá trị số nguyên có thể null vào PreparedStatement
    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }

    static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : e.getMessage();
    }

    private static final String IMPORT_KEYWORD_FILTER = """
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
            """;

    private static final String EXPORT_KEYWORD_FILTER = """
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
            """;

    // Lấy 1 trang lịch sử nhập kho (phân trang trong SQL, LATERAL chỉ tính cho các phiếu của trang)
    public PageResult<InventoryTransactionDTO> getImportTransactions(String keyword, LocalDate fromDate, LocalDate toDate,
                                                                     int page, int size) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = hasText(keyword);
            StringBuilder from = new StringBuilder("""
                FROM inventory_transactions it
                LEFT JOIN employees e ON it.employee_id = e.id
                LEFT JOIN suppliers s ON it.supplier_id = s.id
                WHERE it.type = 'IMPORT'
            """);
            if (hasKeyword) from.append(IMPORT_KEYWORD_FILTER);
            appendDateRange(from, fromDate, toDate);

            long total = countHistory(session, from.toString(), hasKeyword ? keyword : null, fromDate, toDate);
            if (total == 0) {
                return PageResult.empty(validPage, validSize);
            }
            String sql = """
                SELECT it.id, it.type, it.transaction_at, it.note,
                       e.id AS employee_id, e.full_name AS employee_name,
                       s.id AS supplier_id, s.name AS supplier_name,
                       agg.item_count, agg.total_quantity, agg.total_amount
                FROM (SELECT it.id %s ORDER BY it.transaction_at DESC, it.id DESC LIMIT :limit OFFSET :offset) page_ids
                JOIN inventory_transactions it ON it.id = page_ids.id
                LEFT JOIN employees e ON it.employee_id = e.id
                LEFT JOIN suppliers s ON it.supplier_id = s.id
                LEFT JOIN LATERAL (
                    SELECT COUNT(*) AS item_count,
                           COALESCE(SUM(il.quantity_in), 0) AS total_quantity,
                           COALESCE(SUM(il.quantity_in * il.import_price), 0) AS total_amount
                    FROM inventory_lots il WHERE il.transaction_id = it.id
                ) agg ON TRUE
                ORDER BY it.transaction_at DESC, it.id DESC
            """.formatted(from);
            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            bindHistoryParams(query, hasKeyword ? keyword : null, fromDate, toDate);
            bindPage(query, validPage, validSize);

            List<InventoryTransactionDTO> result = new ArrayList<>();
            for (Object[] row : query.list()) {
                result.add(new InventoryTransactionDTO(
                        toLongObj(row[0]), toStr(row[1]), toLocalDateTime(row[2]), toInteger(row[4]), toStr(row[5]),
                        toStr(row[7]), toInt(row[8]), toInt(row[9]), toBigDecimal(row[10]), toStr(row[3])));
            }
            return new PageResult<>(result, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử Nhập kho: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    // Lấy 1 trang lịch sử xuất kho (phân trang trong SQL, LATERAL chỉ tính cho các phiếu của trang)
    public PageResult<InventoryTransactionDTO> getExportTransactions(String keyword, LocalDate fromDate, LocalDate toDate,
                                                                     int page, int size) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = hasText(keyword);
            StringBuilder from = new StringBuilder("""
                FROM inventory_transactions it
                LEFT JOIN employees e ON it.employee_id = e.id
                WHERE it.type IN ('EXPORT_SALE', 'EXPORT_DISPOSAL')
            """);
            if (hasKeyword) from.append(EXPORT_KEYWORD_FILTER);
            appendDateRange(from, fromDate, toDate);

            long total = countHistory(session, from.toString(), hasKeyword ? keyword : null, fromDate, toDate);
            if (total == 0) {
                return PageResult.empty(validPage, validSize);
            }
            String sql = """
                SELECT it.id, it.type, it.transaction_at, it.note,
                       e.id AS employee_id, e.full_name AS employee_name,
                       COALESCE(agg.item_count, 0), COALESCE(agg.total_quantity, 0), COALESCE(agg.total_amount, 0)
                FROM (SELECT it.id %s ORDER BY it.transaction_at DESC, it.id DESC LIMIT :limit OFFSET :offset) page_ids
                JOIN inventory_transactions it ON it.id = page_ids.id
                LEFT JOIN employees e ON it.employee_id = e.id
                LEFT JOIN LATERAL (
                    SELECT COUNT(*) AS item_count, SUM(idi.quantity) AS total_quantity, 0 AS total_amount
                    FROM inventory_disposal_items idi
                    WHERE it.type = 'EXPORT_DISPOSAL' AND idi.transaction_id = it.id
                    UNION ALL
                    SELECT COUNT(*), SUM(soi.quantity), SUM(soi.line_total)
                    FROM sales_order_items soi
                    WHERE it.type = 'EXPORT_SALE' AND soi.sales_order_id = it.sales_order_id
                ) agg ON agg.item_count > 0
                ORDER BY it.transaction_at DESC, it.id DESC
            """.formatted(from);
            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            bindHistoryParams(query, hasKeyword ? keyword : null, fromDate, toDate);
            bindPage(query, validPage, validSize);

            List<InventoryTransactionDTO> result = new ArrayList<>();
            for (Object[] row : query.list()) {
                String type = toStr(row[1]);
                InventoryTransactionDTO dto = new InventoryTransactionDTO(
                        toLongObj(row[0]), type, toLocalDateTime(row[2]), toInteger(row[4]), toStr(row[5]),
                        null, toInt(row[6]), toInt(row[7]), toBigDecimal(row[8]), toStr(row[3]));
                if ("EXPORT_DISPOSAL".equalsIgnoreCase(type)) {
                    dto.setExportTypeLabel("Xuất hủy hàng hỏng/hết hạn");
                } else if ("EXPORT_SALE".equalsIgnoreCase(type)) {
                    dto.setExportTypeLabel("Xuất bán lẻ cho khách");
                } else {
                    dto.setExportTypeLabel(dto.getTypeDisplayName());
                }
                result.add(dto);
            }
            return new PageResult<>(result, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử Xuất kho: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    private static long countHistory(Session session, String fromClause, String keyword, LocalDate fromDate, LocalDate toDate) {
        NativeQuery<Long> count = session.createNativeQuery("SELECT COUNT(*) " + fromClause, Long.class);
        bindHistoryParams(count, keyword, fromDate, toDate);
        return toLong(count.uniqueResult());
    }

    private static void bindPage(NativeQuery<?> query, int page, int size) {
        query.setParameter("limit", size);
        query.setParameter("offset", (page - 1) * size);
    }

    // Thêm điều kiện lọc theo khoảng ngày vào câu lệnh SQL
    private static void appendDateRange(StringBuilder sql, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) sql.append(" AND it.transaction_at >= :fromDt ");
        if (toDate != null) sql.append(" AND it.transaction_at < :toDt ");
    }

    // Gán tham số tìm kiếm và khoảng ngày cho truy vấn lịch sử kho
    private static void bindHistoryParams(NativeQuery<?> query, String keyword, LocalDate fromDate, LocalDate toDate) {
        if (keyword != null) query.setParameter("kw", likeParam(keyword));
        if (fromDate != null) query.setParameter("fromDt", fromDate.atStartOfDay());
        if (toDate != null) query.setParameter("toDt", toDate.plusDays(1).atStartOfDay());
    }

    // Lấy chi tiết các mặt hàng trong một giao dịch kho
    public List<InventoryTransactionDetailDTO> getTransactionDetails(Long transactionId, String type) {
        if (transactionId == null) return Collections.emptyList();
        String sql;
        if (type == null || "IMPORT".equalsIgnoreCase(type)) {
            sql = """
                SELECT il.id, il.transaction_id, il.medicine_id, m.code, m.name,
                       COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                       il.lot_number, il.expiry_date, il.quantity_in,
                       il.import_price, (il.quantity_in * il.import_price) AS total_price,
                       '' AS reason
                FROM inventory_lots il
                JOIN medicines m ON il.medicine_id = m.id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                WHERE il.transaction_id = :txId
                ORDER BY il.id ASC
            """;
        } else if ("EXPORT_DISPOSAL".equalsIgnoreCase(type)) {
            sql = """
                SELECT idi.id, idi.transaction_id, il.medicine_id, m.code, m.name,
                       COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                       il.lot_number, il.expiry_date, idi.quantity,
                       il.import_price, (idi.quantity * il.import_price) AS total_price,
                       COALESCE(idi.reason, 'Xuất hủy') AS reason
                FROM inventory_disposal_items idi
                JOIN inventory_lots il ON idi.inventory_lot_id = il.id
                JOIN medicines m ON il.medicine_id = m.id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                WHERE idi.transaction_id = :txId
                ORDER BY idi.id ASC
            """;
        } else {
            sql = """
                SELECT soi.id, it.id AS transaction_id, soi.medicine_id, m.code, m.name,
                       COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                       il.lot_number, il.expiry_date, soi.quantity,
                       soi.unit_price, soi.line_total AS total_price,
                       'Xuất bán hóa đơn' AS reason
                FROM inventory_transactions it
                JOIN sales_order_items soi ON soi.sales_order_id = it.sales_order_id
                JOIN inventory_lots il ON soi.inventory_lot_id = il.id
                JOIN medicines m ON soi.medicine_id = m.id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                WHERE it.id = :txId
                ORDER BY soi.id ASC
            """;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> rows = session.createNativeQuery(sql, Object[].class)
                    .setParameter("txId", transactionId)
                    .list();
            List<InventoryTransactionDetailDTO> list = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                list.add(new InventoryTransactionDetailDTO(
                        toLongObj(row[0]), toInteger(row[2]), toStr(row[3]), toStr(row[4]),
                        toStr(row[5]), toStr(row[6]), toStr(row[7]), toStr(row[8]), toLocalDate(row[9]),
                        toInt(row[10]), toBigDecimal(row[11]), toBigDecimal(row[12]), toStr(row[13])));
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết giao dịch kho #{}: ", transactionId, e);
            return Collections.emptyList();
        }
    }
}
