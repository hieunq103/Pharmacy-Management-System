package com.pharmacy.inventory;

import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InventoryFilterTest {

    @Test
    void testInventoryFilterLogic() {
        InventoryRepository repo = new InventoryRepository();
        List<MedicineInventoryDTO> list = repo.getAllInventory();
        assertFalse(list.isEmpty(), "Danh sách thuốc không được rỗng");

        ObservableList<MedicineInventoryDTO> masterData = FXCollections.observableArrayList(list);
        FilteredList<MedicineInventoryDTO> filtered = new FilteredList<>(masterData);

        String[] statuses = new String[]{"Bình thường", "Sắp hết hàng", "Hết hàng", "Có lô hết hạn", "Có lô cận hạn"};

        for (String st : statuses) {
            filtered.setPredicate(dto -> dto != null && dto.getStatus() != null && dto.getStatus().getLabel().trim().equalsIgnoreCase(st.trim()));
            System.out.println("Lọc trạng thái '" + st + "': tìm thấy " + filtered.size() + " thuốc");
            assertTrue(filtered.size() > 0, "Phải tìm thấy ít nhất 1 thuốc với trạng thái: " + st);
        }
    }

    @Test
    void testLotsQuery() {
        InventoryRepository repo = new InventoryRepository();
        List<MedicineInventoryDTO> list = repo.getAllInventory();
        assertFalse(list.isEmpty());

        MedicineInventoryDTO first = list.get(0);
        List<MedicineLotDTO> lots = repo.getLotsByMedicineId(first.getMedicineId());
        System.out.println("Thuốc: " + first.getMedicineName() + " (ID " + first.getMedicineId() + ") có " + lots.size() + " lô hàng.");
        assertFalse(lots.isEmpty(), "Thuốc đầu tiên phải có ít nhất 1 lô hàng");

        MedicineLotDTO lot = lots.get(0);
        System.out.println("Lô mẫu: " + lot.getLotNumber() + ", HSD: " + lot.getFormattedExpiryDate() + ", Tồn: " + lot.getQuantityRemaining() + ", Trạng thái: " + lot.getStatusLabel() + ", NCC: " + lot.getSupplierName());
        assertTrue(lot.getLotNumber() != null && !lot.getLotNumber().isBlank());
    }

    @Test
    void testInventoryPageResult() {
        InventoryRepository repo = new InventoryRepository();
        com.pharmacy.common.PageResult<MedicineInventoryDTO> page = repo.getInventoryPageResult(1, 7, null, null);
        System.out.println("Page total elements: " + page.getTotalElements());
        System.out.println("Page total pages: " + page.getTotalPages());
        System.out.println("Page items count: " + page.getItems().size());
        assertTrue(page.getTotalElements() > 0, "Tổng số thuốc phải > 0");
        assertFalse(page.getItems().isEmpty(), "Items không được rỗng");

        String[] statuses = new String[]{"Bình thường", "Sắp hết hàng", "Hết hàng", "Có lô hết hạn", "Có lô cận hạn"};
        for (String st : statuses) {
            com.pharmacy.common.PageResult<MedicineInventoryDTO> filterPage = repo.getInventoryPageResult(1, 7, null, st);
            System.out.println("Lọc '" + st + "': tìm thấy " + filterPage.getTotalElements() + " thuốc.");
            assertTrue(filterPage.getTotalElements() > 0, "Trạng thái " + st + " phải có kết quả");
        }

        // Test lọc độc lập 2 cột mới: Tồn kho & Hạn dùng (Đơn chọn & Đa chọn kết hợp)
        String[] stockStatuses = new String[]{"Bình thường", "Sắp hết hàng", "Hết hàng", "Sắp hết hàng,Hết hàng"};
        for (String stockSt : stockStatuses) {
            com.pharmacy.common.PageResult<MedicineInventoryDTO> res = repo.getInventoryPageResult(1, 7, null, stockSt, null);
            System.out.println("Lọc Tồn kho '" + stockSt + "': " + res.getTotalElements() + " thuốc.");
            assertTrue(res.getTotalElements() > 0, "Lọc tồn kho " + stockSt + " phải có kết quả");
        }

        String[] expiryStatuses = new String[]{"Còn hạn", "Có lô cận hạn", "Có lô hết hạn", "Có lô cận hạn,Có lô hết hạn"};
        for (String expSt : expiryStatuses) {
            com.pharmacy.common.PageResult<MedicineInventoryDTO> res = repo.getInventoryPageResult(1, 7, null, null, expSt);
            System.out.println("Lọc Hạn dùng '" + expSt + "': " + res.getTotalElements() + " thuốc.");
            assertTrue(res.getTotalElements() > 0, "Lọc hạn dùng " + expSt + " phải có kết quả");
        }

        com.pharmacy.common.PageResult<MedicineInventoryDTO> searchPage = repo.getInventoryPageResult(1, 7, "Augmentin", null);
        System.out.println("Tìm 'Augmentin': " + searchPage.getTotalElements() + " thuốc.");
        assertTrue(searchPage.getTotalElements() > 0, "Tìm Augmentin phải có kết quả");
    }

    @Test
    void testEmployeePageResult() {
        com.pharmacy.employee.EmployeeRepository empRepo = new com.pharmacy.employee.EmployeeRepository();
        com.pharmacy.common.PageResult<com.pharmacy.employee.Employee> page = empRepo.getEmployeesPage(1, 7, null, null, null, null, null);
        System.out.println("Employee total: " + page.getTotalElements());
        System.out.println("Employee count on page 1: " + page.getItems().size());
        assertTrue(page.getTotalElements() > 0, "Tổng số nhân viên phải > 0");
        assertFalse(page.getItems().isEmpty(), "Items nhân viên không được rỗng");

        com.pharmacy.employee.EmployeeRepository.EmployeeSummary summary = empRepo.getEmployeeSummary();
        System.out.println("Summary - total: " + summary.total() + ", active: " + summary.active());
        assertTrue(summary.total() > 0);

        // Kiểm tra tìm kiếm theo CCCD (chỉ số)
        com.pharmacy.employee.Employee firstEmp = page.getItems().get(0);
        String nationalId = firstEmp.getNationalId();
        if (nationalId != null && !nationalId.isBlank()) {
            long t0 = System.currentTimeMillis();
            com.pharmacy.common.PageResult<com.pharmacy.employee.Employee> cccdResult =
                    empRepo.getEmployeesPage(1, 7, nationalId, null, null, null, null);
            long cost = System.currentTimeMillis() - t0;
            System.out.println("Search employee by CCCD '" + nationalId + "' took: " + cost + "ms, found: " + cccdResult.getTotalElements());
            assertTrue(cccdResult.getTotalElements() > 0, "Tìm theo CCCD phải thấy nhân viên");
            assertEquals(nationalId, cccdResult.getItems().get(0).getNationalId());
        }

        // Kiểm tra tìm kiếm theo Họ tên (chữ)
        String name = firstEmp.getFullName();
        if (name != null && !name.isBlank()) {
            long t0 = System.currentTimeMillis();
            com.pharmacy.common.PageResult<com.pharmacy.employee.Employee> nameResult =
                    empRepo.getEmployeesPage(1, 7, name, null, null, null, null);
            long cost = System.currentTimeMillis() - t0;
            System.out.println("Search employee by Name '" + name + "' took: " + cost + "ms, found: " + nameResult.getTotalElements());
            assertTrue(nameResult.getTotalElements() > 0, "Tìm theo tên phải thấy nhân viên");
        }
    }

    @Test
    void testInventoryHistory() {
        InventoryRepository repo = new InventoryRepository();

        // 1. Kiểm tra lịch sử Nhập kho
        List<com.pharmacy.inventory.dto.InventoryTransactionDTO> imports = repo.getImportTransactions(null, null, null);
        System.out.println("Import transactions count: " + imports.size());
        assertFalse(imports.isEmpty(), "Phải có ít nhất 1 giao dịch nhập kho");

        com.pharmacy.inventory.dto.InventoryTransactionDTO firstImport = imports.get(0);
        System.out.println("Phiếu nhập đầu tiên (ID " + firstImport.getId() + "): Người nhập: " + firstImport.getEmployeeName()
                + ", NCC: " + firstImport.getSupplierName() + ", Số lượng: " + firstImport.getTotalQuantity() + ", Tổng tiền: " + firstImport.getTotalAmount());
        assertTrue(firstImport.getId() != null);

        // 2. Kiểm tra chi tiết phiếu nhập
        List<com.pharmacy.inventory.dto.InventoryTransactionDetailDTO> importDetails = repo.getTransactionDetails(firstImport.getId(), "IMPORT");
        System.out.println("Phiếu nhập (ID " + firstImport.getId() + ") có " + importDetails.size() + " mặt hàng chi tiết.");
        assertFalse(importDetails.isEmpty(), "Chi tiết phiếu nhập không được rỗng");
        com.pharmacy.inventory.dto.InventoryTransactionDetailDTO firstDet = importDetails.get(0);
        System.out.println("Thuốc nhập: " + firstDet.getMedicineName() + " (" + firstDet.getMedicineCode() + "), Lô: "
                + firstDet.getLotNumber() + ", HSD: " + firstDet.getFormattedExpiryDate() + ", SL: " + firstDet.getQuantity());

        // 3. Kiểm tra lịch sử Xuất kho
        List<com.pharmacy.inventory.dto.InventoryTransactionDTO> exports = repo.getExportTransactions(null, null, null);
        System.out.println("Export transactions count: " + exports.size());
    }

    @Test
    void testBatchImportStock() {
        InventoryRepository repo = new InventoryRepository();

        // Chuẩn bị 2 mặt hàng nhập chung 1 phiếu
        java.util.List<com.pharmacy.inventory.dto.BatchImportItemDTO> items = new java.util.ArrayList<>();

        // Item 1: Thuốc có sẵn
        items.add(com.pharmacy.inventory.dto.BatchImportItemDTO.fromExisting(
                1, "MED001", "Augmentin 635mg", "Kháng sinh", "GSK",
                "LOT-BATCH-01", java.time.LocalDate.now().plusYears(2), "Hộp", 30, new java.math.BigDecimal("180000")
        ));

        // Item 2: Thuốc mới
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 10000);
        items.add(com.pharmacy.inventory.dto.BatchImportItemDTO.fromNew(
                null, "Thuốc Mới Test " + uniqueSuffix, "Vitamin & Khoáng chất", "Vitamin C 1000mg",
                "Dược Hậu Giang", new java.math.BigDecimal("95000"), false,
                "LOT-BATCH-NEW-" + uniqueSuffix, java.time.LocalDate.now().plusYears(1), "Lọ", 50, new java.math.BigDecimal("60000")
        ));

        // Thực hiện nhập kho hàng loạt
        boolean success = repo.importBatchStock("Công ty Dược Phẩm Test Batch", 1, "Phiếu nhập hàng loạt test", items);
        assertTrue(success, "Lập phiếu nhập kho hàng loạt phải thành công");

        // Kiểm tra phiếu vừa tạo trong lịch sử
        List<com.pharmacy.inventory.dto.InventoryTransactionDTO> imports = repo.getImportTransactions("Phiếu nhập hàng loạt test", null, null);
        assertFalse(imports.isEmpty(), "Phải tìm thấy phiếu vừa tạo trong lịch sử");

        com.pharmacy.inventory.dto.InventoryTransactionDTO latest = imports.get(0);
        System.out.println("Phiếu nhập hàng loạt vừa tạo (ID " + latest.getId() + "): Số mặt hàng: " + latest.getItemCount()
                + ", Tổng SL: " + latest.getTotalQuantity() + ", Tổng tiền: " + latest.getTotalAmount());
        assertEquals(2, latest.getItemCount(), "Phiếu phải có đúng 2 mặt hàng");
        assertEquals(80, latest.getTotalQuantity(), "Tổng số lượng phải là 30 + 50 = 80");

        // Kiểm tra chi tiết 2 mặt hàng trong phiếu
        List<com.pharmacy.inventory.dto.InventoryTransactionDetailDTO> details = repo.getTransactionDetails(latest.getId(), "IMPORT");
        assertEquals(2, details.size(), "Chi tiết phiếu phải trả về đúng 2 mặt hàng");
        System.out.println("Item 1: " + details.get(0).getMedicineName() + " - SL: " + details.get(0).getQuantity());
        System.out.println("Item 2: " + details.get(1).getMedicineName() + " - SL: " + details.get(1).getQuantity());
    }

    @Test
    void testSearchPerformance() {
        InventoryRepository repo = new InventoryRepository();
        // Warmup
        repo.getInventoryPageResult(1, 7, "warmup", null);

        String[] keywords = {"a", "au", "aug", "p", "pa", "para", "vit", "c", "1", "m", "a"};
        for (String kw : keywords) {
            long t0 = System.currentTimeMillis();
            com.pharmacy.common.PageResult<MedicineInventoryDTO> res = repo.getInventoryPageResult(1, 7, kw, null);
            long cost = System.currentTimeMillis() - t0;
            System.out.println("Search '" + kw + "' took: " + cost + "ms (total elements: " + res.getTotalElements() + ")");
            assertTrue(cost < 1000, "Truy vấn tìm kiếm phải hoàn tất dưới 1000ms (thực tế: " + cost + "ms)");
        }
    }

    @Test
    void testHistorySearchPerformance() {
        InventoryRepository repo = new InventoryRepository();
        repo.getImportTransactions("warmup", null, null);
        repo.getExportTransactions("warmup", null, null);

        String[] keywords = {"a", "au", "aug", "p", "pa", "para", "vit", "c", "1", "m", "pnk", "pxk", "000", "", "a"};
        for (String kw : keywords) {
            long t0 = System.currentTimeMillis();
            List<com.pharmacy.inventory.dto.InventoryTransactionDTO> imp = repo.getImportTransactions(kw, null, null);
            long t1 = System.currentTimeMillis();
            List<com.pharmacy.inventory.dto.InventoryTransactionDTO> exp = repo.getExportTransactions(kw, null, null);
            long t2 = System.currentTimeMillis();
            System.out.printf("History kw='%s': import=%dms (%d rows), export=%dms (%d rows), total=%dms%n",
                    kw, (t1 - t0), imp.size(), (t2 - t1), exp.size(), (t2 - t0));
        }
    }
}

