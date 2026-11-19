package com.pharmacy.inventory.dto;

import com.pharmacy.common.FormatUtils;
import java.time.LocalDate;

public class BatchExportItemDTO {

    private String medicineCode;
    private String medicineName;
    private String categoryName;

    private long lotId;
    private String lotNumber;
    private LocalDate expiryDate;
    private String unit;
    private int stockRemaining;
    private int exportQuantity;
    private String reason;

    public BatchExportItemDTO() {}

    public static BatchExportItemDTO of(
        String code,
        String name,
        String categoryName,
        long lotId,
        String lotNumber,
        LocalDate expiryDate,
        String unit,
        int stockRemaining,
        int exportQuantity,
        String reason
    ) {
        BatchExportItemDTO item = new BatchExportItemDTO();
        item.setMedicineCode(code);
        item.setMedicineName(name);
        item.setCategoryName(categoryName);
        item.setLotId(lotId);
        item.setLotNumber(lotNumber);
        item.setExpiryDate(expiryDate);
        item.setUnit(unit != null && !unit.isBlank() ? unit : "Hộp");
        item.setStockRemaining(stockRemaining);
        item.setExportQuantity(exportQuantity);
        item.setReason(reason);
        return item;
    }

    public String getFormattedExpiryDate() {
        return expiryDate != null ? expiryDate.format(FormatUtils.DATE_FORMATTER) : "";
    }

    public String getMedicineCode() {
        return medicineCode;
    }

    public void setMedicineCode(String medicineCode) {
        this.medicineCode = medicineCode;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public long getLotId() {
        return lotId;
    }

    public void setLotId(long lotId) {
        this.lotId = lotId;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getStockRemaining() {
        return stockRemaining;
    }

    public void setStockRemaining(int stockRemaining) {
        this.stockRemaining = stockRemaining;
    }

    public int getExportQuantity() {
        return exportQuantity;
    }

    public void setExportQuantity(int exportQuantity) {
        this.exportQuantity = exportQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
