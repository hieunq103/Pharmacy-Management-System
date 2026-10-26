package com.pharmacy.inventory;

import com.pharmacy.common.PageResult;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service xử lý nghiệp vụ quản lý kho thuốc và lô hàng.
 */
public interface InventoryService {

    /**
     * Lấy danh sách thuốc phân trang hỗ trợ lọc độc lập cả Mức tồn kho và Hạn dùng.
     */
    PageResult<MedicineInventoryDTO> getInventoryPage(int page, int pageSize, String keyword, String stockFilter, String expiryFilter);

    /**
     * Lấy dữ liệu thống kê tổng số thuốc, còn hàng, sắp hết hàng và cận hạn.
     */
    InventoryRepository.InventorySummary getInventorySummary();

    /**
     * Lấy danh sách cảnh báo kho (lô cận hạn và thuốc sắp hết hàng) phục vụ Dashboard / Báo cáo.
     */
    List<InventoryAlertDTO> getInventoryAlerts(int limit);

    /**
     * Lấy danh sách thuốc phục vụ cho ô tìm kiếm/chọn nhanh (Picker).
     */
    List<MedicineInventoryDTO> getMedicinesForPicker();

    /**
     * Cập nhật thông tin nhanh của một đầu thuốc.
     */
    boolean updateMedicine(int medicineId, String name, String activeIngredient, String manufacturer, BigDecimal sellPrice);

    /**
     * Lấy danh sách toàn bộ các lô hàng của một loại thuốc theo ID thuốc.
     */
    List<MedicineLotDTO> getLotsByMedicineId(int medicineId);

    /**
     * Truy xuất trực tiếp Repository (dành cho các Dialog cần thao tác giao dịch phức tạp).
     */
    InventoryRepository getRepository();

    List<String> getAllSupplierNames();

    List<String> getAllCategoryNames();

    String getNextMedicineCode();

    List<com.pharmacy.inventory.dto.InventoryTransactionDTO> getImportTransactions(String keyword, java.time.LocalDate fromDate, java.time.LocalDate toDate);

    List<com.pharmacy.inventory.dto.InventoryTransactionDTO> getExportTransactions(String keyword, java.time.LocalDate fromDate, java.time.LocalDate toDate);

    List<com.pharmacy.inventory.dto.InventoryTransactionDetailDTO> getTransactionDetails(Long transactionId, String type);

    /**
     * Lập phiếu nhập kho nhiều lô (chỉ Quản lý). Nhân viên thực hiện lấy từ phiên đăng nhập.
     */
    boolean importBatchStock(
            String supplierName,
            Integer employeeId,
            String note,
            List<com.pharmacy.inventory.dto.BatchImportItemDTO> items
    );

    /**
     * Lập phiếu xuất hủy nhiều lô (chỉ Quản lý). Nhân viên thực hiện lấy từ phiên đăng nhập.
     */
    boolean exportBatchStock(
            String reason,
            Integer employeeId,
            String note,
            List<com.pharmacy.inventory.dto.BatchExportItemDTO> items
    );
}
