package com.pharmacy.sales.dto;

import com.pharmacy.common.FormatUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public String getMedicineCode() {
        return medicineCode;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getActiveIngredient() {
        return activeIngredient;
    }

    public String getUnit() {
        return unit;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public int getQuantity() {
        return quantity;
    }

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
