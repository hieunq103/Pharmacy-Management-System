package com.pharmacy.inventory.dto;

import com.pharmacy.common.FormatUtils;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO đại diện cho một dòng chi tiết thuốc trong giao dịch xuất hoặc nhập kho (Detail record).
 */
public class InventoryTransactionDetailDTO {


    private Long id;
    private Integer medicineId;
    private String medicineCode;
    private String medicineName;
    private String categoryName;
    private String unit;
    private String manufacturer;
    private String lotNumber;
    private LocalDate expiryDate;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String reason;

    public InventoryTransactionDetailDTO() {
    }

    public InventoryTransactionDetailDTO(Long id, Integer medicineId,
                                        String medicineCode, String medicineName,
                                        String categoryName, String unit, String manufacturer,
                                        String lotNumber, LocalDate expiryDate,
                                        int quantity, BigDecimal unitPrice, BigDecimal totalPrice,
                                        String reason) {
        this.id = id;
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.categoryName = categoryName;
        this.unit = unit;
        this.manufacturer = manufacturer;
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.reason = reason;
    }

    public String getFormattedExpiryDate() {
        if (expiryDate == null) return "N/A";
        return expiryDate.format(FormatUtils.DATE_FORMATTER);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Integer medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineCode() {
        return medicineCode != null ? medicineCode : "—";
    }

    public void setMedicineCode(String medicineCode) {
        this.medicineCode = medicineCode;
    }

    public String getMedicineName() {
        return medicineName != null ? medicineName : "—";
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getCategoryName() {
        return categoryName != null && !categoryName.isBlank() ? categoryName : "Chưa phân loại";
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnit() {
        return unit != null && !unit.isBlank() ? unit : "Hộp";
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getManufacturer() {
        return manufacturer != null && !manufacturer.isBlank() ? manufacturer : "—";
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getLotNumber() {
        return lotNumber != null ? lotNumber : "—";
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice != null ? unitPrice : BigDecimal.ZERO;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        if (totalPrice != null) return totalPrice;
        if (unitPrice != null) return unitPrice.multiply(BigDecimal.valueOf(quantity));
        return BigDecimal.ZERO;
    }

    public String getReason() {
        return reason != null && !reason.isBlank() ? reason : "—";
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
