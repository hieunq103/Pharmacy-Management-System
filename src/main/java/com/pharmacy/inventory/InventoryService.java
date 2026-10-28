package com.pharmacy.inventory;

import com.pharmacy.common.PageResult;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;

import java.math.BigDecimal;
import java.util.List;

public interface InventoryService {
    PageResult<MedicineInventoryDTO> getInventoryPage(int page, int pageSize, String keyword, String stockFilter, String expiryFilter);

    InventoryRepository.InventorySummary getInventorySummary();

    List<InventoryAlertDTO> getInventoryAlerts(int limit);

    List<MedicineInventoryDTO> getMedicinesForPicker();

    boolean updateMedicine(int medicineId, String name, String activeIngredient, String manufacturer, BigDecimal sellPrice);

    List<MedicineLotDTO> getLotsByMedicineId(int medicineId);

    InventoryRepository getRepository();

    List<String> getAllSupplierNames();

    List<String> getAllCategoryNames();

    String getNextMedicineCode();

    List<com.pharmacy.inventory.dto.InventoryTransactionDTO> getImportTransactions(String keyword, java.time.LocalDate fromDate, java.time.LocalDate toDate);

    List<com.pharmacy.inventory.dto.InventoryTransactionDTO> getExportTransactions(String keyword, java.time.LocalDate fromDate, java.time.LocalDate toDate);

    List<com.pharmacy.inventory.dto.InventoryTransactionDetailDTO> getTransactionDetails(Long transactionId, String type);

    boolean importBatchStock(
            String supplierName,
            Integer employeeId,
            String note,
            List<com.pharmacy.inventory.dto.BatchImportItemDTO> items
    );

    boolean exportBatchStock(
            String reason,
            Integer employeeId,
            String note,
            List<com.pharmacy.inventory.dto.BatchExportItemDTO> items
    );
}
