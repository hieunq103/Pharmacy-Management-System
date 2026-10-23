package com.pharmacy.sales.dto;

import java.math.BigDecimal;

public class CartItem {

    private final Integer medicineId;
    private final String medicineCode;
    private final String medicineName;
    private final String unit;
    private final Boolean requiresPrescription;
    private final BigDecimal sellPrice;
    private int quantity;
    private final int maxStock;

    public CartItem(Integer medicineId, String medicineCode, String medicineName, String unit,
                    Boolean requiresPrescription, BigDecimal sellPrice, int quantity, int maxStock) {
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.unit = unit != null ? unit : "hộp";
        this.requiresPrescription = Boolean.TRUE.equals(requiresPrescription);
        this.sellPrice = sellPrice != null ? sellPrice : BigDecimal.ZERO;
        this.quantity = quantity;
        this.maxStock = maxStock;
    }

    public Integer getMedicineId() { return medicineId; }
    public String getMedicineCode() { return medicineCode; }
    public String getMedicineName() { return medicineName; }
    public String getUnit() { return unit; }
    public Boolean isRequiresPrescription() { return requiresPrescription; }
    public BigDecimal getSellPrice() { return sellPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getMaxStock() { return maxStock; }

    public BigDecimal getLineTotal() {
        return sellPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
