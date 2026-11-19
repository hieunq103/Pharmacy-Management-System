package com.pharmacy.inventory;

import static com.pharmacy.common.SqlRows.*;

import com.pharmacy.common.AppConstants;
import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.SearchFilter;
import com.pharmacy.employee.Employee;
import com.pharmacy.infra.db.CodeSequence;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.infra.db.TransactionGuards;
import com.pharmacy.inventory.dto.BatchExportItemDTO;
import com.pharmacy.inventory.dto.BatchImportItemDTO;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.inventory.dto.InventorySummaryDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDetailDTO;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;
import com.pharmacy.inventory.entity.InventoryDisposalItem;
import com.pharmacy.inventory.entity.InventoryLot;
import com.pharmacy.inventory.entity.InventoryTransaction;
import com.pharmacy.inventory.entity.Medicine;
import com.pharmacy.inventory.entity.MedicineCategory;
import com.pharmacy.inventory.entity.Supplier;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InventoryRepository {

    private static final Logger log = LoggerFactory.getLogger(InventoryRepository.class);

    private static final String LOW_STOCK_THRESHOLD =
        "CASE WHEN m.minStockThreshold > 0 THEN m.minStockThreshold ELSE " +
        AppConstants.DEFAULT_LOW_STOCK_THRESHOLD +
        " END";

    private static SearchFilter medicineSearch(String keyword) {
        return SearchFilter.of(keyword, "kw").prefix("m.code").contains("m.name", "m.activeIngredient");
    }

    public InventorySummaryDTO getInventorySummary() {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            LocalDate today = LocalDate.now();
            String hql = """
                SELECT
                    m.id,
                    %s,
                    COALESCE(SUM(il.quantityRemaining), 0),
                    COALESCE(SUM(CASE WHEN il.expiryDate > :today AND il.expiryDate <= :nearExpiryDate THEN 1 ELSE 0 END), 0)
                FROM Medicine m
                LEFT JOIN InventoryLot il ON il.medicine.id = m.id AND il.quantityRemaining > 0
                WHERE m.status = :active
                GROUP BY m.id, m.minStockThreshold
            """.formatted(LOW_STOCK_THRESHOLD);
            List<Object[]> rows = session
                .createSelectionQuery(hql, Object[].class)
                .setParameter("active", Medicine.Status.ACTIVE)
                .setParameter("today", today)
                .setParameter("nearExpiryDate", today.plusDays(AppConstants.NEAR_EXPIRY_DAYS))
                .list();
            long total = rows.size();
            long inStock = 0;
            long lowStock = 0;
            long nearExpired = 0;
            for (Object[] r : rows) {
                int threshold = toInt(r[1]);
                long qty = toLong(r[2]);
                long nearExp = toLong(r[3]);
                if (qty > 0) inStock++;
                if (qty <= threshold) lowStock++;
                if (nearExp > 0) nearExpired++;
            }
            return new InventorySummaryDTO(total, inStock, lowStock, nearExpired);
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê kho: ", e);
            return new InventorySummaryDTO(0L, 0L, 0L, 0L);
        }
    }

    public List<InventoryAlertDTO> getInventoryAlerts(int limit) {
        List<InventoryAlertDTO> alerts = new ArrayList<>();
        try (Session session = HibernateUtil.openReadOnlySession()) {
            LocalDate today = LocalDate.now();

            String expiryHql = """
                SELECT m.id, m.name, il.lotNumber, il.expiryDate, il.quantityRemaining, m.unit
                FROM InventoryLot il
                JOIN il.medicine m
                WHERE il.expiryDate <= :maxDate
                  AND il.quantityRemaining > 0
                  AND m.status = :active
                ORDER BY il.expiryDate ASC
            """;
            List<Object[]> expiryRows = session
                .createSelectionQuery(expiryHql, Object[].class)
                .setParameter("maxDate", today.plusDays(AppConstants.NEAR_EXPIRY_DAYS))
                .setParameter("active", Medicine.Status.ACTIVE)
                .setMaxResults(limit)
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
                    subtitle =
                        "Hết hạn " +
                        expDate.format(FormatUtils.DATE_FORMATTER) +
                        (daysLeft == 0 ? " (Hôm nay)" : " (Còn " + daysLeft + " ngày)");
                }
                alerts.add(
                    new InventoryAlertDTO(title, subtitle, toInt(r[4]), toStr(r[5]), InventoryAlertDTO.AlertType.EXPIRY)
                );
            }

            String lowStockHql = """
                SELECT m.id, m.name, COALESCE(SUM(il.quantityRemaining), 0), m.unit, %s
                FROM Medicine m
                LEFT JOIN InventoryLot il ON il.medicine.id = m.id AND il.quantityRemaining > 0
                WHERE m.status = :active
                GROUP BY m.id, m.name, m.unit, m.minStockThreshold
                HAVING COALESCE(SUM(il.quantityRemaining), 0) <= %s
                ORDER BY COALESCE(SUM(il.quantityRemaining), 0) ASC
            """.formatted(LOW_STOCK_THRESHOLD, LOW_STOCK_THRESHOLD);
            List<Object[]> lowStockRows = session
                .createSelectionQuery(lowStockHql, Object[].class)
                .setParameter("active", Medicine.Status.ACTIVE)
                .setMaxResults(limit)
                .list();

            for (Object[] r : lowStockRows) {
                int qty = toInt(r[2]);
                String unit = toStr(r[3]);
                String subtitle =
                    qty == 0
                        ? "Đã hết hàng trong kho"
                        : "Tồn kho dưới ngưỡng tối thiểu (" + qty + "/" + toInt(r[4]) + " " + unit + ")";
                alerts.add(
                    new InventoryAlertDTO(toStr(r[1]), subtitle, qty, unit, InventoryAlertDTO.AlertType.LOW_STOCK)
                );
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách cảnh báo kho trong InventoryRepository: ", e);
        }
        return alerts;
    }

    public List<MedicineInventoryDTO> getMedicinesForPicker() {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String hql = """
                SELECT m.id, m.code, m.name, mc.name, m.unit, m.sellPrice, m.activeIngredient, m.manufacturer
                FROM Medicine m
                LEFT JOIN m.category mc
                WHERE m.status = :active
                ORDER BY m.name ASC
            """;
            List<Object[]> rows = session
                .createSelectionQuery(hql, Object[].class)
                .setParameter("active", Medicine.Status.ACTIVE)
                .list();
            if (rows.isEmpty()) {
                return Collections.emptyList();
            }
            List<Integer> ids = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                ids.add(toInteger(row[0]));
            }
            Map<Integer, LotStats> statsMap = LotStats.load(session, ids);
            List<MedicineInventoryDTO> list = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                Integer medId = toInteger(row[0]);
                LotStats stats = statsMap.getOrDefault(medId, LotStats.EMPTY);
                MedicineInventoryDTO dto = new MedicineInventoryDTO();
                dto.setMedicineId(medId);
                dto.setMedicineCode(toStr(row[1]));
                dto.setMedicineName(toStr(row[2]));
                dto.setCategoryName(toStr(row[3]));
                dto.setUnit(toStr(row[4]));
                dto.setSellPrice(toBigDecimal(row[5]));
                dto.setActiveIngredient(toStr(row[6]));
                dto.setManufacturer(toStr(row[7]));
                dto.setTotalQuantity(stats.totalQty());
                dto.setExpiryStatus(expiryStatus(stats.expiredCount(), stats.nearExpiredCount()));
                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách thuốc cho picker: ", e);
            return Collections.emptyList();
        }
    }

    public PageResult<MedicineInventoryDTO> getInventoryPageResult(
        int page,
        int size,
        String searchKeyword,
        String stockFilter,
        String expiryFilter
    ) {
        return getInventoryPageResult(page, size, searchKeyword, stockFilter, expiryFilter, null);
    }

    public PageResult<MedicineInventoryDTO> getInventoryPageResult(
        int page,
        int size,
        String searchKeyword,
        String stockFilter,
        String expiryFilter,
        Long cachedTotal
    ) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.openReadOnlySession()) {
            SearchFilter search = medicineSearch(searchKeyword);
            String havingClause = buildHavingConditions(stockFilter, expiryFilter);
            boolean hasHaving = !havingClause.isEmpty();
            LocalDate today = LocalDate.now();

            long total;
            List<Object[]> rows;
            List<Integer> pageIds;

            if (hasHaving) {
                String filterHql =
                    "SELECT m.id FROM Medicine m LEFT JOIN InventoryLot il ON il.medicine.id = m.id AND il.quantityRemaining > 0 " +
                    " WHERE m.status = :active " +
                    search.hql() +
                    " GROUP BY m.id, m.minStockThreshold " +
                    havingClause +
                    " ORDER BY m.id ASC";
                SelectionQuery<Integer> query = session
                    .createSelectionQuery(filterHql, Integer.class)
                    .setParameter("active", Medicine.Status.ACTIVE);
                search.bind(query);
                if (havingClause.contains(":today")) query.setParameter("today", today);
                if (havingClause.contains(":nearExpiryDate")) query.setParameter(
                    "nearExpiryDate",
                    today.plusDays(AppConstants.NEAR_EXPIRY_DAYS)
                );

                int fromIndex = (validPage - 1) * validSize;
                if (cachedTotal != null && cachedTotal > 0) {
                    // Đã biết tổng số dòng (từ lần tải trước): để DB cắt trang, không kéo toàn bộ ID về JVM
                    total = cachedTotal;
                    if (fromIndex >= total) {
                        return PageResult.empty(validPage, validSize);
                    }
                    pageIds = query.setFirstResult(fromIndex).setMaxResults(validSize).list();
                } else {
                    List<Integer> allMatchedIds = query.list();
                    total = allMatchedIds.size();
                    if (total == 0 || fromIndex >= total) {
                        return PageResult.empty(validPage, validSize);
                    }
                    int toIndex = (int) Math.min(fromIndex + validSize, total);
                    pageIds = allMatchedIds.subList(fromIndex, toIndex);
                }

                String dataHql = """
                    SELECT m.id, m.code, m.name, mc.name, m.requiresPrescription, m.unit, m.sellPrice,
                           m.activeIngredient, m.manufacturer, m.minStockThreshold
                    FROM Medicine m
                    LEFT JOIN m.category mc
                    WHERE m.id IN (:ids)
                    ORDER BY m.id ASC
                """;
                rows = session.createSelectionQuery(dataHql, Object[].class).setParameter("ids", pageIds).list();
            } else {
                String where = " WHERE m.status = :active " + search.hql();
                if (cachedTotal != null && cachedTotal >= 0) {
                    total = cachedTotal;
                } else {
                    SelectionQuery<Long> countQuery = session
                        .createSelectionQuery("SELECT COUNT(m.id) FROM Medicine m " + where, Long.class)
                        .setParameter("active", Medicine.Status.ACTIVE);
                    search.bind(countQuery);
                    total = toLong(countQuery.uniqueResult());
                }
                if (total == 0) {
                    return PageResult.empty(validPage, validSize);
                }

                String dataHql =
                    """
                        SELECT m.id, m.code, m.name, mc.name, m.requiresPrescription, m.unit, m.sellPrice,
                               m.activeIngredient, m.manufacturer, m.minStockThreshold
                        FROM Medicine m
                        LEFT JOIN m.category mc
                    """ +
                    where +
                    " ORDER BY m.id ASC";
                var dataQuery = session
                    .createSelectionQuery(dataHql, Object[].class)
                    .setParameter("active", Medicine.Status.ACTIVE)
                    .setFirstResult((validPage - 1) * validSize)
                    .setMaxResults(validSize);
                search.bind(dataQuery);
                rows = dataQuery.list();

                pageIds = new ArrayList<>(rows.size());
                for (Object[] r : rows) {
                    pageIds.add(toInteger(r[0]));
                }
            }

            if (pageIds.isEmpty()) {
                return PageResult.empty(validPage, validSize);
            }
            Map<Integer, LotStats> statsById = LotStats.load(session, pageIds);

            List<MedicineInventoryDTO> items = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                Integer medId = toInteger(row[0]);
                items.add(mapInventoryRow(row, statsById.getOrDefault(medId, LotStats.EMPTY)));
            }
            return new PageResult<>(items, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang kho thuốc: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    private String buildHavingConditions(String stockFilter, String expiryFilter) {
        List<String> conditions = new ArrayList<>();

        if (hasText(stockFilter) && !"Tất cả".equalsIgnoreCase(stockFilter)) {
            List<String> stockConds = new ArrayList<>();
            for (String tok : stockFilter.split(",")) {
                switch (tok.trim()) {
                    case "Hết hàng" -> stockConds.add("COALESCE(SUM(il.quantityRemaining), 0) = 0");
                    case "Sắp hết hàng" -> stockConds.add(
                        "(COALESCE(SUM(il.quantityRemaining), 0) > 0 AND COALESCE(SUM(il.quantityRemaining), 0) <= " +
                            LOW_STOCK_THRESHOLD +
                            ")"
                    );
                    case "Bình thường" -> stockConds.add(
                        "COALESCE(SUM(il.quantityRemaining), 0) > " + LOW_STOCK_THRESHOLD
                    );
                    default -> {
                    }
                }
            }
            if (!stockConds.isEmpty()) {
                conditions.add("(" + String.join(" OR ", stockConds) + ")");
            }
        }

        if (hasText(expiryFilter) && !"Tất cả".equalsIgnoreCase(expiryFilter)) {
            List<String> expConds = new ArrayList<>();
            for (String tok : expiryFilter.split(",")) {
                switch (tok.trim()) {
                    case "Có lô hết hạn" -> expConds.add(
                        "SUM(CASE WHEN il.expiryDate <= :today THEN 1 ELSE 0 END) > 0"
                    );
                    case "Có lô cận hạn" -> expConds.add(
                        "SUM(CASE WHEN il.expiryDate > :today AND il.expiryDate <= :nearExpiryDate THEN 1 ELSE 0 END) > 0"
                    );
                    case "Còn hạn" -> expConds.add(
                        "SUM(CASE WHEN il.expiryDate <= :nearExpiryDate THEN 1 ELSE 0 END) = 0"
                    );
                    default -> {
                    }
                }
            }
            if (!expConds.isEmpty()) {
                conditions.add("(" + String.join(" OR ", expConds) + ")");
            }
        }

        if (conditions.isEmpty()) {
            return "";
        }
        return " HAVING " + String.join(" AND ", conditions);
    }

    static MedicineInventoryDTO mapInventoryRow(Object[] row, LotStats stats) {
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

        int qty = stats.totalQty();
        dto.setTotalQuantity(qty);
        int minStock = toInt(row[9]);
        int threshold = minStock > 0 ? minStock : AppConstants.DEFAULT_LOW_STOCK_THRESHOLD;

        if (qty == 0) {
            dto.setStockStatus(MedicineInventoryDTO.StockStatus.OUT_OF_STOCK);
        } else if (qty <= threshold) {
            dto.setStockStatus(MedicineInventoryDTO.StockStatus.LOW_STOCK);
        } else {
            dto.setStockStatus(MedicineInventoryDTO.StockStatus.NORMAL);
        }
        dto.setExpiryStatus(expiryStatus(stats.expiredCount(), stats.nearExpiredCount()));

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

    private static MedicineInventoryDTO.ExpiryStatus expiryStatus(int expiredCount, int nearExpiredCount) {
        if (expiredCount > 0) return MedicineInventoryDTO.ExpiryStatus.EXPIRED;
        if (nearExpiredCount > 0) return MedicineInventoryDTO.ExpiryStatus.NEAR_EXPIRY;
        return MedicineInventoryDTO.ExpiryStatus.NORMAL;
    }

    public boolean updateMedicine(
        int medicineId,
        String name,
        String activeIngredient,
        String manufacturer,
        BigDecimal sellPrice
    ) {
        try {
            return HibernateUtil.executeWithResult(
                session ->
                    session
                        .createMutationQuery(
                            """
                                UPDATE Medicine m
                                SET m.name = :name,
                                    m.activeIngredient = :activeIngredient,
                                    m.manufacturer = :manufacturer,
                                    m.sellPrice = :sellPrice
                                WHERE m.id = :id
                            """
                        )
                        .setParameter("name", name)
                        .setParameter("activeIngredient", activeIngredient)
                        .setParameter("manufacturer", manufacturer)
                        .setParameter("sellPrice", sellPrice)
                        .setParameter("id", medicineId)
                        .executeUpdate() > 0
            );
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật thông tin thuốc ID {}: ", medicineId, e);
            return false;
        }
    }

    public List<MedicineLotDTO> getLotsByMedicineId(int medicineId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String hql = """
                SELECT il.id, m.id, il.lotNumber, il.expiryDate, il.importPrice,
                       il.quantityIn, il.quantityRemaining, it.transactionAt, it.type,
                       s.name, it.note, m.manufacturer
                FROM InventoryLot il
                JOIN il.medicine m
                LEFT JOIN il.transaction it
                LEFT JOIN it.supplier s
                WHERE m.id = :medicineId
                ORDER BY il.expiryDate ASC, il.id ASC
            """;
            List<Object[]> rows = session
                .createSelectionQuery(hql, Object[].class)
                .setParameter("medicineId", medicineId)
                .list();
            List<MedicineLotDTO> list = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                MedicineLotDTO dto = new MedicineLotDTO();
                dto.setId(toLongObj(row[0]));
                dto.setLotNumber(toStr(row[2]));
                dto.setExpiryDate(toLocalDate(row[3]));
                dto.setImportPrice(toBigDecimal(row[4]));
                dto.setQuantityIn(toInt(row[5]));
                dto.setQuantityRemaining(toInt(row[6]));
                dto.setTransactionAt(toLocalDateTime(row[7]));
                dto.setSupplierName(toStr(row[9]));
                dto.setManufacturer(toStr(row[11]));
                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách lô của thuốc ID {}: ", medicineId, e);
            return Collections.emptyList();
        }
    }

    public List<String> getAllSupplierNames() {
        return listNames("SELECT s.name FROM Supplier s ORDER BY s.name ASC", "nhà cung cấp");
    }

    public List<String> getAllCategoryNames() {
        return listNames("SELECT mc.name FROM MedicineCategory mc ORDER BY mc.name ASC", "nhóm thuốc");
    }

    private List<String> listNames(String hql, String label) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            return session.createSelectionQuery(hql, String.class).list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách {}: ", label, e);
            return Collections.emptyList();
        }
    }

    public boolean existsMedicineByName(String name) {
        if (!hasText(name)) return false;
        return exists("SELECT COUNT(m.id) FROM Medicine m WHERE m.name = :v", name.trim());
    }

    public boolean existsMedicineById(Integer id) {
        if (id == null || id <= 0) return false;
        return exists("SELECT COUNT(m.id) FROM Medicine m WHERE m.id = :v", id);
    }

    private boolean exists(String countHql, Object value) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            Long count = session.createSelectionQuery(countHql, Long.class).setParameter("v", value).uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi kiểm tra tồn tại [{}]: ", countHql, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu.", e);
        }
    }

    private static volatile boolean medicineSequenceReady;

    private static void ensureMedicineSequence() {
        if (medicineSequenceReady) return;
        try {
            HibernateUtil.execute(session -> {
                if (session.find(CodeSequence.class, "MEDICINE") != null) {
                    return;
                }
                List<String> codes = session
                    .createSelectionQuery("SELECT m.code FROM Medicine m WHERE m.code LIKE 'MED%'", String.class)
                    .list();
                long lastNumber = 0;
                for (String code : codes) {
                    if (code != null && code.length() > 3) {
                        String suffix = code.substring(3);
                        if (suffix.matches("\\d+")) {
                            lastNumber = Math.max(lastNumber, Long.parseLong(suffix));
                        }
                    }
                }
                session.persist(new CodeSequence("MEDICINE", lastNumber));
            });
        } catch (Exception ignored) {
            // Đã có luồng khác tạo dòng bộ đếm cùng thời điểm
        }
        medicineSequenceReady = true;
    }

    private static long reserveMedicineCodes(Session session, int count) {
        if (count <= 0) return 0;
        CodeSequence seq = session.find(CodeSequence.class, "MEDICINE", LockModeType.PESSIMISTIC_WRITE);
        if (seq == null) {
            throw new IllegalStateException("Thiếu bộ đếm mã thuốc trong bảng code_sequences");
        }
        long start = seq.getCurrentNumber() + 1;
        seq.setCurrentNumber(seq.getCurrentNumber() + count);
        return start;
    }

    public boolean importBatchStock(
        String supplierName,
        Integer employeeId,
        String note,
        List<BatchImportItemDTO> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng nhập kho không được rỗng!");
        }
        Map<BatchImportItemDTO, String> allocatedCodes = new LinkedHashMap<>();
        int newMedicineCount = (int) items.stream().filter(BatchImportItemDTO::isNewMedicine).count();
        try {
            ensureMedicineSequence();
            HibernateUtil.execute(session -> {
                allocatedCodes.clear();
                long[] nextCodeNumber = { reserveMedicineCodes(session, newMedicineCount) };
                TransactionGuards.requireActiveEmployee(session, employeeId);

                Supplier supplier = hasText(supplierName) ? findOrCreateSupplier(session, supplierName.trim()) : null;
                Employee empRef = session.getReference(Employee.class, employeeId);

                InventoryTransaction tx = new InventoryTransaction();
                tx.setType(InventoryTransaction.Type.IMPORT);
                tx.setSupplier(supplier);
                tx.setEmployee(empRef);
                tx.setNote(hasText(note) ? note.trim() : "Nhập kho theo đợt");
                session.persist(tx);

                Map<String, MedicineCategory> categoryCache = new HashMap<>();
                for (BatchImportItemDTO item : items) {
                    Medicine medicine =
                        !item.isNewMedicine() &&
                        item.getExistingMedicineId() != null &&
                        item.getExistingMedicineId() > 0
                            ? session.getReference(Medicine.class, item.getExistingMedicineId())
                            : insertNewMedicine(session, item, categoryCache, nextCodeNumber, allocatedCodes);

                    InventoryLot lot = new InventoryLot();
                    lot.setMedicine(medicine);
                    lot.setTransaction(tx);
                    lot.setLotNumber(item.getLotNumber().trim());
                    lot.setExpiryDate(item.getExpiryDate());
                    lot.setImportPrice(item.getImportPrice() != null ? item.getImportPrice() : BigDecimal.ZERO);
                    lot.setQuantityIn(item.getQuantityIn());
                    lot.setQuantityRemaining(item.getQuantityIn());
                    session.persist(lot);
                }
            });
            // Chỉ ghi mã thật vào phiếu sau khi commit thành công
            allocatedCodes.forEach(BatchImportItemDTO::setMedicineCode);
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi thực hiện giao dịch Nhập kho hàng loạt: ", e);
            throw new RuntimeException(
                "Lỗi nhập kho hàng loạt: " + com.pharmacy.common.UserMessages.safeMessage(e, null),
                e
            );
        }
    }

    private Medicine insertNewMedicine(
        Session session,
        BatchImportItemDTO item,
        Map<String, MedicineCategory> categoryCache,
        long[] nextCodeNumber,
        Map<BatchImportItemDTO, String> allocatedCodes
    ) {
        MedicineCategory category = null;
        if (hasText(item.getCategoryName())) {
            String catName = item.getCategoryName().trim();
            category = categoryCache.get(catName);
            if (category == null) {
                category = findOrCreateCategory(session, catName);
                categoryCache.put(catName, category);
            }
        }

        String code = String.format("MED%03d", nextCodeNumber[0]++);
        allocatedCodes.put(item, code);

        Medicine med = new Medicine();
        med.setCode(code);
        med.setName(item.getMedicineName().trim());
        med.setActiveIngredient(item.getActiveIngredient() != null ? item.getActiveIngredient().trim() : null);
        med.setUnit(item.getUnit() != null ? item.getUnit().trim() : "Hộp");
        med.setCategory(category);
        med.setManufacturer(item.getManufacturer() != null ? item.getManufacturer().trim() : null);
        med.setSellPrice(item.getSellPrice() != null ? item.getSellPrice() : BigDecimal.ZERO);
        med.setRequiresPrescription(item.isRequiresPrescription());
        med.setMinStockThreshold(0);
        med.setStatus(Medicine.Status.ACTIVE);
        session.persist(med);
        return med;
    }

    private static Supplier findOrCreateSupplier(Session session, String name) {
        Supplier existing = session
            .createSelectionQuery("FROM Supplier s WHERE s.name = :name", Supplier.class)
            .setParameter("name", name)
            .setMaxResults(1)
            .uniqueResult();
        if (existing != null) return existing;
        Supplier created = new Supplier(name);
        session.persist(created);
        return created;
    }

    private static MedicineCategory findOrCreateCategory(Session session, String name) {
        MedicineCategory existing = session
            .createSelectionQuery("FROM MedicineCategory mc WHERE mc.name = :name", MedicineCategory.class)
            .setParameter("name", name)
            .setMaxResults(1)
            .uniqueResult();
        if (existing != null) return existing;
        MedicineCategory created = new MedicineCategory(name);
        session.persist(created);
        return created;
    }

    public boolean exportBatchStock(String reason, Integer employeeId, String note, List<BatchExportItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng xuất kho không được rỗng!");
        }
        try {
            HibernateUtil.execute(session -> {
                TransactionGuards.requireActiveEmployee(session, employeeId);

                Map<Long, Integer> requested = new HashMap<>();
                for (BatchExportItemDTO item : items) {
                    requested.merge(item.getLotId(), item.getExportQuantity(), Integer::sum);
                }
                Map<Long, InventoryLot> lockedLots = lockLots(session, requested.keySet());
                for (BatchExportItemDTO item : items) {
                    InventoryLot lot = lockedLots.get(item.getLotId());
                    if (lot == null) {
                        throw new IllegalArgumentException(
                            "Không tìm thấy thông tin lô hàng " + item.getLotNumber() + " trong kho."
                        );
                    }
                    int need = requested.get(item.getLotId());
                    if (lot.getQuantityRemaining() < need) {
                        throw new IllegalArgumentException(
                            "Thuốc " +
                                item.getMedicineName() +
                                " (Lô: " +
                                item.getLotNumber() +
                                ") yêu cầu xuất " +
                                need +
                                " nhưng chỉ còn tồn " +
                                lot.getQuantityRemaining() +
                                "!"
                        );
                    }
                }

                String fullNote = (reason != null ? reason : "") + (hasText(note) ? " - " + note.trim() : "");
                InventoryTransaction tx = new InventoryTransaction();
                tx.setType(InventoryTransaction.Type.EXPORT_DISPOSAL);
                tx.setEmployee(session.getReference(Employee.class, employeeId));
                tx.setNote(fullNote);
                session.persist(tx);

                for (BatchExportItemDTO item : items) {
                    InventoryLot lot = lockedLots.get(item.getLotId());
                    lot.setQuantityRemaining(lot.getQuantityRemaining() - item.getExportQuantity());

                    String itemReason = hasText(item.getReason())
                        ? item.getReason()
                        : hasText(reason)
                          ? reason
                          : "Xuất hủy";
                    InventoryDisposalItem disposalItem = new InventoryDisposalItem();
                    disposalItem.setTransaction(tx);
                    disposalItem.setInventoryLot(lot);
                    disposalItem.setQuantity(item.getExportQuantity());
                    disposalItem.setReason(itemReason);
                    session.persist(disposalItem);
                }
            });
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi thực hiện giao dịch Xuất kho hàng loạt: ", e);
            throw new RuntimeException("Lỗi xuất kho: " + com.pharmacy.common.UserMessages.safeMessage(e, null), e);
        }
    }

    private static Map<Long, InventoryLot> lockLots(Session session, Collection<Long> lotIds) {
        List<InventoryLot> lots = session
            .createSelectionQuery("FROM InventoryLot il WHERE il.id IN (:ids)", InventoryLot.class)
            .setParameter("ids", lotIds)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .list();
        Map<Long, InventoryLot> result = new HashMap<>(lots.size() * 2);
        for (InventoryLot lot : lots) {
            result.put(lot.getId(), lot);
        }
        return result;
    }

    private static SearchFilter importSearch(String keyword) {
        return SearchFilter.of(keyword, "kw")
            .contains("e.fullName", "s.name", "it.note")
            .custom(
                "EXISTS (SELECT 1 FROM InventoryLot ilS JOIN ilS.medicine mS WHERE ilS.transaction.id = it.id" +
                    " AND (mS.name LIKE {p} ESCAPE '!' OR mS.code LIKE {p} ESCAPE '!' OR ilS.lotNumber LIKE {p} ESCAPE '!'))"
            );
    }

    private static SearchFilter exportSearch(String keyword) {
        return SearchFilter.of(keyword, "kw")
            .contains("e.fullName", "it.note")
            .custom(
                "EXISTS (SELECT 1 FROM InventoryDisposalItem idiS JOIN idiS.inventoryLot ilS JOIN ilS.medicine mS" +
                    " WHERE idiS.transaction.id = it.id AND (mS.name LIKE {p} ESCAPE '!' OR mS.code LIKE {p} ESCAPE '!'" +
                    " OR ilS.lotNumber LIKE {p} ESCAPE '!' OR idiS.reason LIKE {p} ESCAPE '!'))"
            )
            .custom(
                "EXISTS (SELECT 1 FROM SalesOrderItem soiS JOIN soiS.medicine mS2 WHERE it.salesOrder IS NOT NULL" +
                    " AND soiS.salesOrder.id = it.salesOrder.id AND (mS2.name LIKE {p} ESCAPE '!' OR mS2.code LIKE {p} ESCAPE '!'))"
            );
    }

    public PageResult<InventoryTransactionDTO> getImportTransactions(
        String keyword,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.openReadOnlySession()) {
            SearchFilter search = importSearch(keyword);
            StringBuilder from = new StringBuilder(
                """
                    FROM InventoryTransaction it
                    LEFT JOIN it.employee e
                    LEFT JOIN it.supplier s
                    WHERE it.type = :importType
                """
            );
            from.append(search.hql());
            appendDateRange(from, fromDate, toDate);

            SelectionQuery<Long> idsQuery = session
                .createSelectionQuery(
                    "SELECT it.id " + from + " ORDER BY it.transactionAt DESC, it.id DESC",
                    Long.class
                )
                .setParameter("importType", InventoryTransaction.Type.IMPORT)
                .setFirstResult((validPage - 1) * validSize)
                .setMaxResults(validSize);
            bindHistoryParams(idsQuery, search, fromDate, toDate);
            List<Long> pageIds = idsQuery.list();
            long total = totalFor(pageIds, validPage, validSize, () -> {
                SelectionQuery<Long> countQuery = session
                    .createSelectionQuery("SELECT COUNT(it.id) " + from, Long.class)
                    .setParameter("importType", InventoryTransaction.Type.IMPORT);
                bindHistoryParams(countQuery, search, fromDate, toDate);
                return toLong(countQuery.uniqueResult());
            });
            if (total == 0 || pageIds.isEmpty()) {
                return PageResult.empty(validPage, validSize);
            }

            List<Object[]> headerRows = session
                .createSelectionQuery(
                    """
                    SELECT it.id, it.type, it.transactionAt, it.note,
                           e.id, e.fullName, s.name
                    FROM InventoryTransaction it
                    LEFT JOIN it.employee e
                    LEFT JOIN it.supplier s
                    WHERE it.id IN (:ids)
                    ORDER BY it.transactionAt DESC, it.id DESC
                    """,
                    Object[].class
                )
                .setParameter("ids", pageIds)
                .list();

            List<Object[]> aggRows = session
                .createSelectionQuery(
                    """
                    SELECT il.transaction.id, COUNT(il.id),
                           COALESCE(SUM(il.quantityIn), 0),
                           COALESCE(SUM(il.quantityIn * il.importPrice), 0)
                    FROM InventoryLot il
                    WHERE il.transaction.id IN (:ids)
                    GROUP BY il.transaction.id
                    """,
                    Object[].class
                )
                .setParameter("ids", pageIds)
                .list();
            Map<Long, Object[]> aggByTxId = new HashMap<>(aggRows.size() * 2);
            for (Object[] a : aggRows) {
                aggByTxId.put(toLongObj(a[0]), a);
            }

            List<InventoryTransactionDTO> result = new ArrayList<>(headerRows.size());
            for (Object[] row : headerRows) {
                Long id = toLongObj(row[0]);
                Object[] agg = aggByTxId.get(id);
                result.add(
                    new InventoryTransactionDTO(
                        id,
                        toStr(row[1]),
                        toLocalDateTime(row[2]),
                        toStr(row[5]),
                        toStr(row[6]),
                        agg != null ? toInt(agg[1]) : 0,
                        agg != null ? toInt(agg[2]) : 0,
                        agg != null ? toBigDecimal(agg[3]) : BigDecimal.ZERO,
                        toStr(row[3])
                    )
                );
            }
            return new PageResult<>(result, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử Nhập kho: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    public PageResult<InventoryTransactionDTO> getExportTransactions(
        String keyword,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
    ) {
        int validPage = Math.max(1, page);
        int validSize = Math.max(1, size);
        try (Session session = HibernateUtil.openReadOnlySession()) {
            SearchFilter search = exportSearch(keyword);
            List<InventoryTransaction.Type> exportTypes = List.of(
                InventoryTransaction.Type.EXPORT_SALE,
                InventoryTransaction.Type.EXPORT_DISPOSAL
            );
            StringBuilder from = new StringBuilder(
                """
                    FROM InventoryTransaction it
                    LEFT JOIN it.employee e
                    WHERE it.type IN (:exportTypes)
                """
            );
            from.append(search.hql());
            appendDateRange(from, fromDate, toDate);

            SelectionQuery<Long> idsQuery = session
                .createSelectionQuery(
                    "SELECT it.id " + from + " ORDER BY it.transactionAt DESC, it.id DESC",
                    Long.class
                )
                .setParameter("exportTypes", exportTypes)
                .setFirstResult((validPage - 1) * validSize)
                .setMaxResults(validSize);
            bindHistoryParams(idsQuery, search, fromDate, toDate);
            List<Long> pageIds = idsQuery.list();
            long total = totalFor(pageIds, validPage, validSize, () -> {
                SelectionQuery<Long> countQuery = session
                    .createSelectionQuery("SELECT COUNT(it.id) " + from, Long.class)
                    .setParameter("exportTypes", exportTypes);
                bindHistoryParams(countQuery, search, fromDate, toDate);
                return toLong(countQuery.uniqueResult());
            });
            if (total == 0 || pageIds.isEmpty()) {
                return PageResult.empty(validPage, validSize);
            }

            List<Object[]> headerRows = session
                .createSelectionQuery(
                    """
                    SELECT it.id, it.type, it.transactionAt, it.note,
                           e.id, e.fullName, it.salesOrder.id
                    FROM InventoryTransaction it
                    LEFT JOIN it.employee e
                    WHERE it.id IN (:ids)
                    ORDER BY it.transactionAt DESC, it.id DESC
                    """,
                    Object[].class
                )
                .setParameter("ids", pageIds)
                .list();

            List<Long> disposalTxIds = new ArrayList<>();
            List<Long> saleOrderIds = new ArrayList<>();
            for (Object[] row : headerRows) {
                InventoryTransaction.Type type = (InventoryTransaction.Type) row[1];
                if (type == InventoryTransaction.Type.EXPORT_DISPOSAL) {
                    disposalTxIds.add(toLongObj(row[0]));
                } else if (type == InventoryTransaction.Type.EXPORT_SALE && row[6] != null) {
                    saleOrderIds.add(toLongObj(row[6]));
                }
            }

            Map<Long, Object[]> disposalAgg = new HashMap<>();
            if (!disposalTxIds.isEmpty()) {
                List<Object[]> rows = session
                    .createSelectionQuery(
                        """
                        SELECT idi.transaction.id, COUNT(idi.id), COALESCE(SUM(idi.quantity), 0)
                        FROM InventoryDisposalItem idi
                        WHERE idi.transaction.id IN (:ids)
                        GROUP BY idi.transaction.id
                        """,
                        Object[].class
                    )
                    .setParameter("ids", disposalTxIds)
                    .list();
                for (Object[] r : rows) disposalAgg.put(toLongObj(r[0]), r);
            }

            Map<Long, Object[]> saleAgg = new HashMap<>();
            if (!saleOrderIds.isEmpty()) {
                List<Object[]> rows = session
                    .createSelectionQuery(
                        """
                        SELECT soi.salesOrder.id, COUNT(soi.id), COALESCE(SUM(soi.quantity), 0), COALESCE(SUM(soi.lineTotal), 0)
                        FROM SalesOrderItem soi
                        WHERE soi.salesOrder.id IN (:orderIds)
                        GROUP BY soi.salesOrder.id
                        """,
                        Object[].class
                    )
                    .setParameter("orderIds", saleOrderIds)
                    .list();
                for (Object[] r : rows) saleAgg.put(toLongObj(r[0]), r);
            }

            List<InventoryTransactionDTO> result = new ArrayList<>(headerRows.size());
            for (Object[] row : headerRows) {
                Long id = toLongObj(row[0]);
                InventoryTransaction.Type typeEnum = (InventoryTransaction.Type) row[1];
                String type = typeEnum != null ? typeEnum.name() : null;
                int itemCount = 0;
                int totalQty = 0;
                BigDecimal totalAmount = BigDecimal.ZERO;

                if (typeEnum == InventoryTransaction.Type.EXPORT_DISPOSAL) {
                    Object[] agg = disposalAgg.get(id);
                    if (agg != null) {
                        itemCount = toInt(agg[1]);
                        totalQty = toInt(agg[2]);
                    }
                } else if (typeEnum == InventoryTransaction.Type.EXPORT_SALE && row[6] != null) {
                    Object[] agg = saleAgg.get(toLongObj(row[6]));
                    if (agg != null) {
                        itemCount = toInt(agg[1]);
                        totalQty = toInt(agg[2]);
                        totalAmount = toBigDecimal(agg[3]);
                    }
                }

                InventoryTransactionDTO dto = new InventoryTransactionDTO(
                    id,
                    type,
                    toLocalDateTime(row[2]),
                    toStr(row[5]),
                    null,
                    itemCount,
                    totalQty,
                    totalAmount,
                    toStr(row[3])
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
            return new PageResult<>(result, total, validPage, validSize);
        } catch (Exception e) {
            log.error("Lỗi khi lấy lịch sử Xuất kho: ", e);
            return PageResult.empty(validPage, validSize);
        }
    }

    // Tổng số dòng: trang chưa đầy thì suy ra từ chính trang đó, chỉ COUNT khi thật sự cần
    private static long totalFor(List<Long> pageIds, int page, int size, java.util.function.LongSupplier counter) {
        if (pageIds.size() < size && (page == 1 || !pageIds.isEmpty())) {
            return (long) (page - 1) * size + pageIds.size();
        }
        return counter.getAsLong();
    }

    private static void appendDateRange(StringBuilder hql, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null) hql.append(" AND it.transactionAt >= :fromDt ");
        if (toDate != null) hql.append(" AND it.transactionAt < :toDt ");
    }

    private static void bindHistoryParams(
        SelectionQuery<?> query,
        SearchFilter search,
        LocalDate fromDate,
        LocalDate toDate
    ) {
        search.bind(query);
        if (fromDate != null) query.setParameter("fromDt", fromDate.atStartOfDay());
        if (toDate != null) query.setParameter("toDt", toDate.plusDays(1).atStartOfDay());
    }

    public List<InventoryTransactionDetailDTO> getTransactionDetails(Long transactionId, String type) {
        if (transactionId == null) return Collections.emptyList();
        String hql;
        if (type == null || "IMPORT".equalsIgnoreCase(type)) {
            hql = """
                SELECT il.id, il.transaction.id, m.id, m.code, m.name,
                       COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                       il.lotNumber, il.expiryDate, il.quantityIn,
                       il.importPrice, (il.quantityIn * il.importPrice),
                       ''
                FROM InventoryLot il
                JOIN il.medicine m
                LEFT JOIN m.category mc
                WHERE il.transaction.id = :txId
                ORDER BY il.id ASC
            """;
        } else if ("EXPORT_DISPOSAL".equalsIgnoreCase(type)) {
            hql = """
                SELECT idi.id, idi.transaction.id, m.id, m.code, m.name,
                       COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                       il.lotNumber, il.expiryDate, idi.quantity,
                       il.importPrice, (idi.quantity * il.importPrice),
                       COALESCE(idi.reason, 'Xuất hủy')
                FROM InventoryDisposalItem idi
                JOIN idi.inventoryLot il
                JOIN il.medicine m
                LEFT JOIN m.category mc
                WHERE idi.transaction.id = :txId
                ORDER BY idi.id ASC
            """;
        } else {
            hql = """
                SELECT soi.id, it.id, m.id, m.code, m.name,
                       COALESCE(mc.name, 'Chưa phân loại'), m.unit, m.manufacturer,
                       il.lotNumber, il.expiryDate, soi.quantity,
                       soi.unitPrice, soi.lineTotal,
                       'Xuất bán hóa đơn'
                FROM InventoryTransaction it
                JOIN SalesOrderItem soi ON soi.salesOrder.id = it.salesOrder.id
                JOIN soi.inventoryLot il
                JOIN soi.medicine m
                LEFT JOIN m.category mc
                WHERE it.id = :txId
                ORDER BY soi.id ASC
            """;
        }
        try (Session session = HibernateUtil.openReadOnlySession()) {
            List<Object[]> rows = session
                .createSelectionQuery(hql, Object[].class)
                .setParameter("txId", transactionId)
                .list();
            List<InventoryTransactionDetailDTO> list = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                list.add(
                    new InventoryTransactionDetailDTO(
                        toStr(row[3]),
                        toStr(row[4]),
                        toStr(row[5]),
                        toStr(row[6]),
                        toStr(row[7]),
                        toStr(row[8]),
                        toLocalDate(row[9]),
                        toInt(row[10]),
                        toBigDecimal(row[11]),
                        toBigDecimal(row[12]),
                        toStr(row[13])
                    )
                );
            }
            return list;
        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết giao dịch kho #{}: ", transactionId, e);
            return Collections.emptyList();
        }
    }
}
