package com.pharmacy.inventory;

import com.pharmacy.common.PageResult;
import com.pharmacy.inventory.dto.BatchImportItemDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDetailDTO;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;
import com.pharmacy.support.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InventoryFilterTest {
    private final InventoryRepository repo = new InventoryRepository();

    @BeforeAll
    static void requireDb() {
        DbTestSupport.assumeDatabase();
    }

    @AfterEach
    void cleanSession() {
        DbTestSupport.logout();
    }

    private List<MedicineInventoryDTO> allMedicines() {
        return repo.getInventoryPageResult(1, 10_000, null, null, null).getItems();
    }

    @Test
    void testInventoryStatusLabels() {
        List<MedicineInventoryDTO> list = allMedicines();
        assertFalse(list.isEmpty(), "Danh sách thuốc không được rỗng");
        for (String st : new String[]{"Bình thường", "Sắp hết hàng", "Hết hàng", "Có lô hết hạn", "Có lô cận hạn"}) {
            long count = list.stream().filter(d -> d.getStatus() != null && d.getStatus().getLabel().equalsIgnoreCase(st)).count();
            assertTrue(count > 0, "Phải tìm thấy ít nhất 1 thuốc với trạng thái: " + st);
        }
    }

    @Test
    void testLotsQuery() {
        MedicineInventoryDTO first = allMedicines().get(0);
        List<MedicineLotDTO> lots = repo.getLotsByMedicineId(first.getMedicineId());
        assertFalse(lots.isEmpty(), "Thuốc đầu tiên phải có ít nhất 1 lô hàng");
        assertTrue(lots.get(0).getLotNumber() != null && !lots.get(0).getLotNumber().isBlank());
    }

    @Test
    void testInventoryPageResultAndFilters() {
        PageResult<MedicineInventoryDTO> page = repo.getInventoryPageResult(1, 7, null, null, null);
        assertTrue(page.getTotalElements() > 0, "Tổng số thuốc phải > 0");
        assertEquals(7, page.getItems().size());

        for (String stockSt : new String[]{"Bình thường", "Sắp hết hàng", "Hết hàng", "Sắp hết hàng,Hết hàng"}) {
            PageResult<MedicineInventoryDTO> res = repo.getInventoryPageResult(1, 7, null, stockSt, null);
            assertTrue(res.getTotalElements() > 0, "Lọc tồn kho " + stockSt + " phải có kết quả");
        }
        for (String expSt : new String[]{"Còn hạn", "Có lô cận hạn", "Có lô hết hạn", "Có lô cận hạn,Có lô hết hạn"}) {
            PageResult<MedicineInventoryDTO> res = repo.getInventoryPageResult(1, 7, null, null, expSt);
            assertTrue(res.getTotalElements() > 0, "Lọc hạn dùng " + expSt + " phải có kết quả");
        }
        assertTrue(repo.getInventoryPageResult(1, 7, "Augmentin", null, null).getTotalElements() > 0);
    }

    @Test
    void testFiltersMatchRowStatus() {
        List<MedicineInventoryDTO> all = allMedicines();
        long outOfStock = all.stream().filter(d -> d.getStockStatus() == MedicineInventoryDTO.StockStatus.OUT_OF_STOCK).count();
        long lowStock = all.stream().filter(d -> d.getStockStatus() == MedicineInventoryDTO.StockStatus.LOW_STOCK).count();
        long expired = all.stream().filter(d -> d.getExpiryStatus() == MedicineInventoryDTO.ExpiryStatus.EXPIRED).count();

        assertEquals(outOfStock, repo.getInventoryPageResult(1, 7, null, "Hết hàng", null).getTotalElements());
        assertEquals(lowStock, repo.getInventoryPageResult(1, 7, null, "Sắp hết hàng", null).getTotalElements());
        assertEquals(expired, repo.getInventoryPageResult(1, 7, null, null, "Có lô hết hạn").getTotalElements());
        assertEquals(all.size(), repo.getInventoryPageResult(1, 7, null, "Bình thường,Sắp hết hàng,Hết hàng", null).getTotalElements());
    }

    @Test
    void testPickerAndSummary() {
        List<MedicineInventoryDTO> picker = repo.getMedicinesForPicker();
        assertEquals(allMedicines().size(), picker.size());
        InventoryRepository.InventorySummary summary = repo.getInventorySummary();
        assertEquals(picker.size(), summary.totalMedicines());
        assertTrue(summary.inStockMedicines() <= summary.totalMedicines());
    }

    @Test
    void testInventoryHistory() {
        List<InventoryTransactionDTO> imports = repo.getImportTransactions(null, null, null, 1, 50).getItems();
        assertFalse(imports.isEmpty(), "Phải có ít nhất 1 giao dịch nhập kho");
        InventoryTransactionDTO firstImport = imports.get(0);
        List<InventoryTransactionDetailDTO> details = repo.getTransactionDetails(firstImport.getId(), "IMPORT");
        assertEquals(firstImport.getItemCount(), details.size(), "Số dòng chi tiết phải khớp số mặt hàng tổng hợp");

        List<InventoryTransactionDTO> exports = repo.getExportTransactions(null, null, null, 1, 200).getItems();
        assertFalse(exports.isEmpty(), "Phải có giao dịch xuất bán từ dữ liệu seed");
        InventoryTransactionDTO sale = exports.stream().filter(t -> "EXPORT_SALE".equals(t.getType())).findFirst().orElseThrow();
        assertEquals(sale.getItemCount(), repo.getTransactionDetails(sale.getId(), "EXPORT_SALE").size());
    }

    @Test
    void testBatchImportStockWithTwoNewMedicines() {
        Integer managerEmployeeId = DbTestSupport.loginAsManager().getEmployee().getId();
        InventoryService service = new InventoryServiceImpl(repo);
        String suffix = String.valueOf(System.nanoTime());
        List<BatchImportItemDTO> items = new ArrayList<>();
        items.add(BatchImportItemDTO.fromExisting(1, "MED001", "Augmentin 635mg", "Kháng sinh", "GSK",
                "LOT-BATCH-" + suffix, LocalDate.now().plusYears(2), "Hộp", 30, new BigDecimal("180000")));

        items.add(BatchImportItemDTO.fromNew(null, "Thuốc Test A " + suffix, "Vitamin & Khoáng chất", "Vitamin C",
                "DHG", new BigDecimal("95000"), false, "LOT-NEW-A-" + suffix, LocalDate.now().plusYears(1), "Lọ", 50, new BigDecimal("60000")));
        items.add(BatchImportItemDTO.fromNew(null, "Thuốc Test B " + suffix, "Vitamin & Khoáng chất", "Vitamin D",
                "DHG", new BigDecimal("85000"), false, "LOT-NEW-B-" + suffix, LocalDate.now().plusYears(1), "Lọ", 20, new BigDecimal("50000")));

        assertTrue(service.importBatchStock("Công ty Test " + suffix, null, "Phiếu test " + suffix, items));

        List<InventoryTransactionDTO> imports = repo.getImportTransactions("Phiếu test " + suffix, null, null, 1, 10).getItems();
        assertEquals(1, imports.size());
        assertEquals(3, imports.get(0).getItemCount());
        assertEquals(100, imports.get(0).getTotalQuantity());
        assertEquals(managerEmployeeId, imports.get(0).getEmployeeId(), "Người nhập phải là nhân viên đang đăng nhập");
        List<InventoryTransactionDetailDTO> details = repo.getTransactionDetails(imports.get(0).getId(), "IMPORT");
        assertNotEquals(details.get(1).getMedicineCode(), details.get(2).getMedicineCode(), "Hai thuốc mới phải có mã khác nhau");
    }

    private static BatchImportItemDTO newMedicineItem(String tag, int quantity) {
        return BatchImportItemDTO.fromNew(null, "Thuốc Đồng thời " + tag, "Vitamin & Khoáng chất", "Vitamin C",
                "DHG", new BigDecimal("10000"), false, "LOT-CC-" + tag, LocalDate.now().plusYears(1), "Hộp", quantity, new BigDecimal("5000"));
    }

    private static int codeNumber(String code) {
        assertTrue(code.matches("MED\\d{3,}"), "Sai định dạng mã thuốc: " + code);
        return Integer.parseInt(code.substring(3));
    }

    @Test
    void concurrentImportsOfNewMedicinesGetDistinctCodes() throws Exception {
        Integer employeeId = DbTestSupport.loginAsManager().getEmployee().getId();
        int threads = 6;
        String suffix = String.valueOf(System.nanoTime());
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var start = new java.util.concurrent.CountDownLatch(1);
        List<java.util.concurrent.Future<List<BatchImportItemDTO>>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final String tag = suffix + "-" + i;
            results.add(pool.submit(() -> {
                List<BatchImportItemDTO> items = List.of(newMedicineItem(tag + "a", 5), newMedicineItem(tag + "b", 5));
                start.await();
                assertTrue(repo.importBatchStock(null, employeeId, "Phiếu đồng thời " + tag, items));
                return items;
            }));
        }
        start.countDown();
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (var f : results) {
            for (BatchImportItemDTO it : f.get(60, java.util.concurrent.TimeUnit.SECONDS)) {
                codeNumber(it.getMedicineCode());
                codes.add(it.getMedicineCode());
            }
        }
        pool.shutdown();
        assertEquals(threads * 2, codes.size(), "Mỗi thuốc mới phải có mã khác nhau: " + codes);
    }

    @Test
    void failedImportDoesNotConsumeMedicineCode() {
        Integer employeeId = DbTestSupport.loginAsManager().getEmployee().getId();
        String suffix = String.valueOf(System.nanoTime());
        BatchImportItemDTO first = newMedicineItem(suffix + "-1", 5);
        assertTrue(repo.importBatchStock(null, employeeId, null, List.of(first)));

        BatchImportItemDTO failing = newMedicineItem(suffix + "-2", 0);
        assertThrows(RuntimeException.class, () -> repo.importBatchStock(null, employeeId, null, List.of(failing)),
                "Số lượng 0 vi phạm CHECK nên cả phiếu phải rollback");
        assertFalse(failing.getMedicineCode().startsWith("MED"), "Phiếu lỗi không được ghi mã vào dòng thuốc");

        BatchImportItemDTO next = newMedicineItem(suffix + "-3", 5);
        assertTrue(repo.importBatchStock(null, employeeId, null, List.of(next)));
        assertEquals(codeNumber(first.getMedicineCode()) + 1, codeNumber(next.getMedicineCode()),
                "Phiếu lỗi không được làm mất số thứ tự mã thuốc");
    }

    @Test
    void testImportRequiresManager() {
        DbTestSupport.loginAs(DbTestSupport.STAFF_USERNAME);
        InventoryService service = new InventoryServiceImpl(repo);
        List<BatchImportItemDTO> items = List.of(BatchImportItemDTO.fromExisting(1, "MED001", "x", null, null,
                "LOT-X", LocalDate.now().plusYears(1), "Hộp", 1, BigDecimal.ONE));
        assertThrows(SecurityException.class, () -> service.importBatchStock(null, null, null, items));
    }

    @Test
    void testExportRejectsOverQuantity() {
        DbTestSupport.loginAsManager();
        InventoryService service = new InventoryServiceImpl(repo);
        MedicineLotDTO lot = allMedicines().stream()
                .filter(m -> m.getTotalQuantity() > 0)
                .flatMap(m -> repo.getLotsByMedicineId(m.getMedicineId()).stream())
                .filter(l -> l.getQuantityRemaining() > 0)
                .findFirst().orElseThrow();
        var item = com.pharmacy.inventory.dto.BatchExportItemDTO.of(lot.getMedicineId(), "x", "x", null, null, lot.getId(),
                lot.getLotNumber(), lot.getExpiryDate(), "Hộp", lot.getQuantityRemaining(), lot.getQuantityRemaining() + 1, BigDecimal.ONE, "Hỏng");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.exportBatchStock("Hỏng", null, null, List.of(item)));
        assertTrue(ex.getMessage().contains("chỉ còn tồn"), ex.getMessage());
        int after = repo.getLotsByMedicineId(lot.getMedicineId()).stream()
                .filter(l -> l.getId().equals(lot.getId())).findFirst().orElseThrow().getQuantityRemaining();
        assertEquals(lot.getQuantityRemaining(), after, "Tồn kho không được thay đổi khi lỗi");
    }

    @Test
    void testHistorySearch() {
        for (String kw : new String[]{"a", "aug", "para", "vit", "1", "", "pnk"}) {
            assertNotNull(repo.getImportTransactions(kw, null, null, 1, 7).getItems());
            assertNotNull(repo.getExportTransactions(kw, null, null, 1, 7).getItems());
        }
    }

    @Test
    void testDashboardCountsMatchInventoryFilters() {
        InventoryRepository.InventorySummary summary = repo.getInventorySummary();
        assertEquals(summary.lowStockMedicines(),
                repo.getInventoryPageResult(1, 7, null, "Sắp hết hàng,Hết hàng", null).getTotalElements());
        assertEquals(summary.nearExpiryMedicines(),
                repo.getInventoryPageResult(1, 7, null, null, "Có lô cận hạn").getTotalElements());
    }

    @Test
    void testHistoryServerSidePaging() {
        var all = repo.getExportTransactions(null, null, null, 1, 100_000);
        var p1 = repo.getExportTransactions(null, null, null, 1, 3);
        var p2 = repo.getExportTransactions(null, null, null, 2, 3);
        assertEquals(all.getItems().size(), all.getTotalElements());
        assertEquals(all.getTotalElements(), p1.getTotalElements());
        assertEquals(all.getItems().subList(0, Math.min(3, all.getItems().size())).stream().map(InventoryTransactionDTO::getId).toList(),
                p1.getItems().stream().map(InventoryTransactionDTO::getId).toList());
        if (all.getItems().size() > 3) {
            assertEquals(all.getItems().get(3).getId(), p2.getItems().get(0).getId());
        }
        var imports = repo.getImportTransactions(null, null, null, 1, 100_000);
        assertEquals(imports.getItems().size(), imports.getTotalElements());
    }
}
