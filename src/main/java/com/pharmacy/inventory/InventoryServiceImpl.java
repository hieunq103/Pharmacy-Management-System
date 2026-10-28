package com.pharmacy.inventory;

import com.pharmacy.common.PageResult;
import com.pharmacy.common.UserSession;
import com.pharmacy.inventory.dto.BatchExportItemDTO;
import com.pharmacy.inventory.dto.BatchImportItemDTO;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDTO;
import com.pharmacy.inventory.dto.InventoryTransactionDetailDTO;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl() {
        this.inventoryRepository = new InventoryRepository();
    }

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
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
    public List<InventoryAlertDTO> getInventoryAlerts(int limit) {
        return inventoryRepository.getInventoryAlerts(limit);
    }

    @Override
    public List<MedicineInventoryDTO> getMedicinesForPicker() {
        return inventoryRepository.getMedicinesForPicker();
    }

    @Override
    public boolean updateMedicine(int medicineId, String name, String activeIngredient, String manufacturer, BigDecimal sellPrice) {
        UserSession.requireManager();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên thuốc không được để trống.");
        }
        if (sellPrice == null || sellPrice.signum() < 0) {
            throw new IllegalArgumentException("Giá bán phải là số không âm.");
        }

        if (!inventoryRepository.existsMedicineById(medicineId)) {
            throw new IllegalArgumentException("Thuốc không tồn tại trong hệ thống (ID: " + medicineId + ").");
        }
        return inventoryRepository.updateMedicine(medicineId, name.trim(), activeIngredient, manufacturer, sellPrice);
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
    public List<InventoryTransactionDTO> getImportTransactions(String keyword, LocalDate fromDate, LocalDate toDate) {
        return inventoryRepository.getImportTransactions(keyword, fromDate, toDate);
    }

    @Override
    public List<InventoryTransactionDTO> getExportTransactions(String keyword, LocalDate fromDate, LocalDate toDate) {
        return inventoryRepository.getExportTransactions(keyword, fromDate, toDate);
    }

    @Override
    public List<InventoryTransactionDetailDTO> getTransactionDetails(Long transactionId, String type) {
        return inventoryRepository.getTransactionDetails(transactionId, type);
    }

    @Override
    public boolean importBatchStock(String supplierName, Integer employeeId, String note, List<BatchImportItemDTO> items) {
        UserSession.requireManager();
        validateImportItems(items);
        for (BatchImportItemDTO item : items) {
            if (isNewMedicine(item) && inventoryRepository.existsMedicineByName(item.getMedicineName())) {
                throw new IllegalArgumentException("Tên thuốc \"" + item.getMedicineName().trim() + "\" đã tồn tại trong danh mục thuốc.");
            }
        }
        return inventoryRepository.importBatchStock(supplierName, resolveEmployeeId(employeeId), note, items);
    }

    @Override
    public boolean exportBatchStock(String reason, Integer employeeId, String note, List<BatchExportItemDTO> items) {
        UserSession.requireManager();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng xuất kho không được rỗng!");
        }
        for (BatchExportItemDTO item : items) {
            if (item.getExportQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng xuất của lô " + item.getLotNumber() + " phải lớn hơn 0.");
            }
        }
        return inventoryRepository.exportBatchStock(reason, resolveEmployeeId(employeeId), note, items);
    }

    private static Integer resolveEmployeeId(Integer requested) {
        Integer current = UserSession.currentEmployeeId();
        return current != null ? current : requested;
    }

    private static boolean isNewMedicine(BatchImportItemDTO item) {
        return item.isNewMedicine() || item.getExistingMedicineId() == null || item.getExistingMedicineId() <= 0;
    }

    static void validateImportItems(List<BatchImportItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách mặt hàng nhập kho không được rỗng!");
        }
        LocalDate today = LocalDate.now();
        Set<String> newNames = new HashSet<>();
        for (BatchImportItemDTO item : items) {
            String label = item.getMedicineName() != null ? item.getMedicineName() : "(chưa có tên)";
            if (item.getLotNumber() == null || item.getLotNumber().isBlank()) {
                throw new IllegalArgumentException("Thuốc " + label + ": số lô không được để trống.");
            }
            if (item.getQuantityIn() <= 0) {
                throw new IllegalArgumentException("Thuốc " + label + ": số lượng nhập phải lớn hơn 0.");
            }
            if (item.getImportPrice() != null && item.getImportPrice().signum() < 0) {
                throw new IllegalArgumentException("Thuốc " + label + ": giá nhập không được âm.");
            }
            if (item.getExpiryDate() == null || !item.getExpiryDate().isAfter(today)) {
                throw new IllegalArgumentException("Thuốc " + label + ": hạn sử dụng phải sau ngày hôm nay.");
            }
            if (isNewMedicine(item)) {
                if (item.getMedicineName() == null || item.getMedicineName().isBlank()) {
                    throw new IllegalArgumentException("Tên thuốc mới không được để trống.");
                }
                if (item.getSellPrice() != null && item.getSellPrice().signum() < 0) {
                    throw new IllegalArgumentException("Thuốc " + label + ": giá bán không được âm.");
                }
                if (!newNames.add(item.getMedicineName().trim().toLowerCase())) {
                    throw new IllegalArgumentException("Thuốc mới \"" + label + "\" bị nhập trùng trong cùng phiếu.");
                }
            }
        }
    }
}
