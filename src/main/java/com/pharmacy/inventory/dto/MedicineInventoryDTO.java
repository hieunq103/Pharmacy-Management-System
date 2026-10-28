package com.pharmacy.inventory.dto;

import java.math.BigDecimal;

public class MedicineInventoryDTO {
    private Integer medicineId;
    private String medicineCode;
    private String medicineName;
    private String activeIngredient;
    private String categoryName;
    private String manufacturer;
    private Boolean requiresPrescription;
    private String unit;
    private BigDecimal sellPrice;

    private Integer totalQuantity;

    private InventoryStatus status;

    private StockStatus stockStatus;
    private ExpiryStatus expiryStatus;

    public enum StockStatus {
        NORMAL("Bình thường"),
        LOW_STOCK("Sắp hết hàng"),
        OUT_OF_STOCK("Hết hàng");

        private final String label;
        StockStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum ExpiryStatus {
        NORMAL("Còn hạn"),
        NEAR_EXPIRY("Có lô cận hạn"),
        EXPIRED("Có lô hết hạn");

        private final String label;
        ExpiryStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum InventoryStatus {
        NORMAL("Bình thường"),
        LOW_STOCK("Sắp hết hàng"),
        HAS_NEAR_EXPIRY_LOT("Có lô cận hạn"),
        HAS_EXPIRED_LOT("Có lô hết hạn"),
        OUT_OF_STOCK("Hết hàng");

        private String label;

        InventoryStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public MedicineInventoryDTO() {
    }

    public MedicineInventoryDTO(Integer medicineId, String medicineCode, String medicineName,
                                String categoryName, Boolean requiresPrescription, String unit,
                                BigDecimal sellPrice, Integer totalQuantity,
                                InventoryStatus status) {
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.categoryName = categoryName;
        this.requiresPrescription = requiresPrescription;
        this.unit = unit;
        this.sellPrice = sellPrice;
        this.totalQuantity = totalQuantity != null ? totalQuantity : 0;
        this.status = status;
    }

    public Integer getMedicineId() { return medicineId; }
    public void setMedicineId(Integer medicineId) { this.medicineId = medicineId; }

    public String getMedicineCode() { return medicineCode; }
    public void setMedicineCode(String medicineCode) { this.medicineCode = medicineCode; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getActiveIngredient() { return activeIngredient; }
    public void setActiveIngredient(String activeIngredient) { this.activeIngredient = activeIngredient; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public Boolean getRequiresPrescription() { return requiresPrescription; }
    public void setRequiresPrescription(Boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getSellPrice() { return sellPrice; }
    public void setSellPrice(BigDecimal sellPrice) { this.sellPrice = sellPrice; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public InventoryStatus getStatus() { return status; }
    public void setStatus(InventoryStatus status) { this.status = status; }

    public StockStatus getStockStatus() { return stockStatus; }
    public void setStockStatus(StockStatus stockStatus) { this.stockStatus = stockStatus; }

    public ExpiryStatus getExpiryStatus() { return expiryStatus; }
    public void setExpiryStatus(ExpiryStatus expiryStatus) { this.expiryStatus = expiryStatus; }
}
