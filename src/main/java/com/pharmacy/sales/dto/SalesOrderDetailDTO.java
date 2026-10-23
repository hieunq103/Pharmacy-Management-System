package com.pharmacy.sales.dto;

import com.pharmacy.common.FormatUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO đại diện cho một mặt hàng trong chi tiết đơn bán hàng.
 */
public class SalesOrderDetailDTO {

    private Long id;
    private Integer medicineId;
    private String medicineCode;
    private String medicineName;
    private String categoryName;
    private String activeIngredient;
    private String unit;
    private String lotNumber;
    private LocalDate expiryDate;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public SalesOrderDetailDTO() {
    }

    public SalesOrderDetailDTO(Long id, Integer medicineId, String medicineCode, String medicineName,
            String categoryName, String activeIngredient, String unit,
            String lotNumber, LocalDate expiryDate, int quantity,
            BigDecimal unitPrice, BigDecimal lineTotal) {
        this.id = id;
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.categoryName = categoryName != null ? categoryName : "—";
        this.activeIngredient = activeIngredient != null ? activeIngredient : "—";
        this.unit = unit != null ? unit : "Hộp";
        this.lotNumber = lotNumber != null ? lotNumber : "—";
        this.expiryDate = expiryDate;
        this.quantity = quantity;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.lineTotal = lineTotal != null ? lineTotal : BigDecimal.ZERO;
    }

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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
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

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    // Helpers cho TableView
    public String getFormattedExpiryDate() {
        return expiryDate != null ? FormatUtils.formatDate(expiryDate) : "—";
    }

    public String getFormattedUnitPrice() {
        return FormatUtils.formatCurrency(unitPrice) + "đ";
    }

    public String getFormattedLineTotal() {
        return FormatUtils.formatCurrency(lineTotal) + "đ";
    }
}
