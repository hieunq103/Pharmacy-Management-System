package com.pharmacy.inventory.dto;

public class InventoryAlertDTO {
    public enum AlertType {
        EXPIRY,
        LOW_STOCK
    }

    private final int medicineId;
    private final String title;
    private final String subtitle;
    private final int remainingQuantity;
    private final String unit;
    private final AlertType alertType;

    public InventoryAlertDTO(int medicineId, String title, String subtitle,
                             int remainingQuantity, String unit, AlertType alertType) {
        this.medicineId = medicineId;
        this.title = title;
        this.subtitle = subtitle;
        this.remainingQuantity = remainingQuantity;
        this.unit = unit != null ? unit : "hộp";
        this.alertType = alertType;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getUnit() {
        return unit;
    }

    public AlertType getAlertType() {
        return alertType;
    }

    public String getQuantityBadgeText() {
        return "Còn " + remainingQuantity + " " + unit;
    }

    public boolean isExpiryAlert() {
        return alertType == AlertType.EXPIRY;
    }
}
