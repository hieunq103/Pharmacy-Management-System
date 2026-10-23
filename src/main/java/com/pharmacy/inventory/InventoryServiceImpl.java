package com.pharmacy.inventory;

import com.pharmacy.common.PageResult;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;

import java.math.BigDecimal;
import java.util.List;

public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl() {
        this.inventoryRepository = new InventoryRepository();
    }

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public PageResult<MedicineInventoryDTO> getInventoryPage(int page, int pageSize, String keyword, String statusFilter) {
        return inventoryRepository.getInventoryPageResult(page, pageSize, keyword, statusFilter);
    }

    @Override
    public PageResult<MedicineInventoryDTO> getInventoryPage(int page, int pageSize, String keyword, String stockFilter, String expiryFilter) {
        return inventoryRepository.getInventoryPageResult(page, pageSize, keyword, stockFilter, expiryFilter);
    }

    @Override
    public InventoryRepository.InventorySummary getInventorySummary() {
        return inventoryRepository.getInventorySummary();
    }

    @Override
    public List<com.pharmacy.inventory.dto.InventoryAlertDTO> getInventoryAlerts(int limit) {
        return inventoryRepository.getInventoryAlerts(limit);
    }

    @Override
    public List<MedicineInventoryDTO> getMedicinesForPicker() {
        return inventoryRepository.getMedicinesForPicker();
    }

    @Override
    public boolean updateMedicine(int medicineId, String name, String activeIngredient, String manufacturer, BigDecimal sellPrice) {
        // DB validation: Kiểm tra thuốc có tồn tại trong CSDL trước khi cập nhật
        if (!inventoryRepository.existsMedicineById(medicineId)) {
            throw new IllegalArgumentException("Thuốc không tồn tại trong hệ thống (ID: " + medicineId + ").");
        }
        return inventoryRepository.updateMedicine(medicineId, name, activeIngredient, manufacturer, sellPrice);
    }

    @Override
    public List<MedicineLotDTO> getLotsByMedicineId(int medicineId) {
        return inventoryRepository.getLotsByMedicineId(medicineId);
    }

    @Override
    public InventoryRepository getRepository() {
        return this.inventoryRepository;
    }

    @Override
    public boolean importStock(
            Integer existingMedicineId,
            String newCode, String newName, String newActiveIngredient,
            String newCategoryName, String newManufacturer, BigDecimal newSellPrice,
            boolean newRequiresPrescription,
            String lotNumber, String supplierName, java.time.LocalDate expiryDate,
            int quantityIn, String unit, BigDecimal importPrice, String note,
            Integer employeeId
    ) {
        // 1. DB Validation: Nếu là nhập cho thuốc đã có, kiểm tra ID thuốc tồn tại trong CSDL
        if (existingMedicineId != null && existingMedicineId > 0) {
            if (!inventoryRepository.existsMedicineById(existingMedicineId)) {
                throw new IllegalArgumentException("Thuốc đã chọn không tồn tại trong hệ thống (ID: " + existingMedicineId + ").");
            }
        } else {
            // 2. DB Validation: Nếu là loại thuốc mới, kiểm tra tên thuốc đã tồn tại trong CSDL chưa
            if (newName != null && inventoryRepository.existsMedicineByName(newName)) {
                throw new IllegalArgumentException("Tên thuốc \"" + newName.trim() + "\" đã tồn tại trong danh mục thuốc.");
            }
            // 3. DB Validation: Kiểm tra mã thuốc đã tồn tại trong CSDL chưa
            if (newCode != null && inventoryRepository.existsMedicineByCode(newCode)) {
                throw new IllegalArgumentException("Mã thuốc \"" + newCode.trim() + "\" đã tồn tại trong hệ thống.");
            }
        }

        return inventoryRepository.importStock(
                existingMedicineId, newCode, newName, newActiveIngredient,
                newCategoryName, newManufacturer, newSellPrice, newRequiresPrescription,
                lotNumber, supplierName, expiryDate, quantityIn, unit, importPrice, note, employeeId
        );
    }

    @Override
    public boolean exportDisposalStock(
            int medicineId,
            long lotId,
            int quantity,
            String reason,
            String note,
            Integer employeeId
    ) {
        // 1. DB Validation: Kiểm tra thuốc có tồn tại trong CSDL không
        if (!inventoryRepository.existsMedicineById(medicineId)) {
            throw new IllegalArgumentException("Thuốc không tồn tại trong hệ thống.");
        }

        // 2. DB Validation: Kiểm tra lô hàng tồn tại và số lượng xuất không vượt quá tồn kho thực tế trong CSDL
        Integer remaining = inventoryRepository.getLotRemainingQuantity(lotId);
        if (remaining == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin lô hàng trong cơ sở dữ liệu.");
        }
        if (quantity > remaining) {
            throw new IllegalArgumentException("Số lượng xuất (" + quantity + ") vượt quá tồn khả dụng trong kho (" + remaining + ")!");
        }

        return inventoryRepository.exportDisposalStock(medicineId, lotId, quantity, reason, note, employeeId);
    }

    @Override
    public List<String> getAllSupplierNames() {
        return inventoryRepository.getAllSupplierNames();
    }

    @Override
    public List<String> getAllCategoryNames() {
        return inventoryRepository.getAllCategoryNames();
    }

    @Override
    public String getNextMedicineCode() {
        return inventoryRepository.getNextMedicineCode();
    }

    @Override
    public List<com.pharmacy.inventory.dto.InventoryTransactionDTO> getImportTransactions(String keyword, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        return inventoryRepository.getImportTransactions(keyword, fromDate, toDate);
    }

    @Override
    public List<com.pharmacy.inventory.dto.InventoryTransactionDTO> getExportTransactions(String keyword, java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        return inventoryRepository.getExportTransactions(keyword, fromDate, toDate);
    }

    @Override
    public List<com.pharmacy.inventory.dto.InventoryTransactionDetailDTO> getTransactionDetails(Long transactionId, String type) {
        return inventoryRepository.getTransactionDetails(transactionId, type);
    }

    @Override
    public boolean importBatchStock(String supplierName, Integer employeeId, String note, List<com.pharmacy.inventory.dto.BatchImportItemDTO> items) {
        return inventoryRepository.importBatchStock(supplierName, employeeId, note, items);
    }

    @Override
    public boolean exportBatchStock(String reason, Integer employeeId, String note, List<com.pharmacy.inventory.dto.BatchExportItemDTO> items) {
        return inventoryRepository.exportBatchStock(reason, employeeId, note, items);
    }
}
