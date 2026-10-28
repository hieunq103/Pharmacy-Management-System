package com.pharmacy.inventory.dto;

import com.pharmacy.common.FormatUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO đại diện cho một mặt hàng thuốc trong danh sách lập phiếu nhập kho hàng loạt.
 */
public class BatchImportItemDTO {


    private boolean isNewMedicine;
    private Integer existingMedicineId;
    private String medicineCode;
    private String medicineName;
    private String categoryName;
    private String activeIngredient;
    private String manufacturer;
    private BigDecimal sellPrice;
    private boolean requiresPrescription;

    private String lotNumber;
    private LocalDate expiryDate;
    private String unit;
    private int quantityIn;
    private BigDecimal importPrice;
    private BigDecimal lineTotal;

    public BatchImportItemDTO() {
    }

    public static BatchImportItemDTO fromExisting(
            Integer medicineId, String code, String name, String categoryName,
            String manufacturer, String lotNumber, LocalDate expiryDate,
            String unit, int quantityIn, BigDecimal importPrice
    ) {
        BatchImportItemDTO item = new BatchImportItemDTO();
        item.setNewMedicine(false);
        item.setExistingMedicineId(medicineId);
        item.setMedicineCode(code);
        item.setMedicineName(name);
        item.setCategoryName(categoryName);
        item.setManufacturer(manufacturer);
        item.setLotNumber(lotNumber);
        item.setExpiryDate(expiryDate);
        item.setUnit(unit != null && !unit.isBlank() ? unit : "Hộp");
        item.setQuantityIn(quantityIn);
        item.setImportPrice(importPrice != null ? importPrice : BigDecimal.ZERO);
        item.setLineTotal(item.getImportPrice().multiply(BigDecimal.valueOf(quantityIn)));
        return item;
    }

    public static BatchImportItemDTO fromNew(
            String code, String name, String categoryName, String activeIngredient,
            String manufacturer, BigDecimal sellPrice, boolean requiresPrescription,
            String lotNumber, LocalDate expiryDate, String unit, int quantityIn, BigDecimal importPrice
    ) {
        BatchImportItemDTO item = new BatchImportItemDTO();
        item.setNewMedicine(true);
        item.setMedicineCode(code != null ? code : "Tự động");
        item.setMedicineName(name);
        item.setCategoryName(categoryName);
        item.setActiveIngredient(activeIngredient);
        item.setManufacturer(manufacturer);
        item.setSellPrice(sellPrice != null ? sellPrice : BigDecimal.ZERO);
        item.setRequiresPrescription(requiresPrescription);
        item.setLotNumber(lotNumber);
        item.setExpiryDate(expiryDate);
        item.setUnit(unit != null && !unit.isBlank() ? unit : "Hộp");
        item.setQuantityIn(quantityIn);
        item.setImportPrice(importPrice != null ? importPrice : BigDecimal.ZERO);
        item.setLineTotal(item.getImportPrice().multiply(BigDecimal.valueOf(quantityIn)));
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

    // Getters and Setters
    public boolean isNewMedicine() {
        return isNewMedicine;
    }

    public void setNewMedicine(boolean newMedicine) {
        isNewMedicine = newMedicine;
    }

    public Integer getExistingMedicineId() {
        return existingMedicineId;
    }

    public void setExistingMedicineId(Integer existingMedicineId) {
        this.existingMedicineId = existingMedicineId;
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

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public void setActiveIngredient(String activeIngredient) {
        this.activeIngredient = activeIngredient;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }

    public boolean isRequiresPrescription() {
        return requiresPrescription;
    }

    public void setRequiresPrescription(boolean requiresPrescription) {
        this.requiresPrescription = requiresPrescription;
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

    public int getQuantityIn() {
        return quantityIn;
    }

    public void setQuantityIn(int quantityIn) {
        this.quantityIn = quantityIn;
        if (this.importPrice != null) {
            this.lineTotal = this.importPrice.multiply(BigDecimal.valueOf(quantityIn));
        }
    }

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
        if (importPrice != null) {
            this.lineTotal = importPrice.multiply(BigDecimal.valueOf(this.quantityIn));
        }
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
