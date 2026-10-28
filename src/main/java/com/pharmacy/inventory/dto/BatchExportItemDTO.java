package com.pharmacy.inventory.dto;

import com.pharmacy.common.FormatUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BatchExportItemDTO {
    private int medicineId;
    private String medicineCode;
    private String medicineName;
    private String categoryName;
    private String manufacturer;

    private long lotId;
    private String lotNumber;
    private LocalDate expiryDate;
    private String unit;
    private int stockRemaining;
    private int exportQuantity;
    private BigDecimal importPrice;
    private BigDecimal lineTotal;
    private String reason;

    public BatchExportItemDTO() {
    }

    public static BatchExportItemDTO of(
            int medicineId, String code, String name, String categoryName, String manufacturer,
            long lotId, String lotNumber, LocalDate expiryDate, String unit,
            int stockRemaining, int exportQuantity, BigDecimal importPrice, String reason
    ) {
        BatchExportItemDTO item = new BatchExportItemDTO();
        item.setMedicineId(medicineId);
        item.setMedicineCode(code);
        item.setMedicineName(name);
        item.setCategoryName(categoryName);
        item.setManufacturer(manufacturer);
        item.setLotId(lotId);
        item.setLotNumber(lotNumber);
        item.setExpiryDate(expiryDate);
        item.setUnit(unit != null && !unit.isBlank() ? unit : "Hộp");
        item.setStockRemaining(stockRemaining);
        item.setExportQuantity(exportQuantity);
        item.setImportPrice(importPrice != null ? importPrice : BigDecimal.ZERO);
        item.setLineTotal(item.getImportPrice().multiply(BigDecimal.valueOf(exportQuantity)));
        item.setReason(reason);
        return item;
    }

    public String getFormattedExpiryDate() {
        return expiryDate != null ? expiryDate.format(FormatUtils.DATE_FORMATTER) : "";
    }

    public String getFormattedImportPrice() {
        return FormatUtils.formatCurrency(importPrice);
    }

    public String getFormattedLineTotal() {
        return FormatUtils.formatCurrency(lineTotal);
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
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

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
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

    public LocalDate getExpiryDate() {
        return expiryDate;
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
        if (this.importPrice != null) {
            this.lineTotal = this.importPrice.multiply(BigDecimal.valueOf(exportQuantity));
        }
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
        if (importPrice != null) {
            this.lineTotal = importPrice.multiply(BigDecimal.valueOf(this.exportQuantity));
        }
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
