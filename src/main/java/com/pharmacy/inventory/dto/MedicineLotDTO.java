package com.pharmacy.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object đại diện cho thông tin một Lô hàng (Inventory Lot) của thuốc
 */
public class MedicineLotDTO {

    private Long id;
    private Integer medicineId;
    private String lotNumber;
    private LocalDate expiryDate;
    private BigDecimal importPrice;
    private Integer quantityIn;
    private Integer quantityRemaining;
    private LocalDateTime transactionAt;
    private String transactionType;
    private String supplierName;
    private String manufacturer;
    private String note;

    public enum LotStatus {
        ACTIVE("Còn hạn", "lot-badge-active"),
        NEAR_EXPIRY("Cận hạn", "lot-badge-near-expiry"),
        EXPIRED("Hết hạn", "lot-badge-expired"),
        DEPLETED("Hết hàng", "lot-badge-depleted");

        private final String label;
        private final String styleClass;

        LotStatus(String label, String styleClass) {
            this.label = label;
            this.styleClass = styleClass;
        }

        public String getLabel() {
            return label;
        }

        public String getStyleClass() {
            return styleClass;
        }
    }

    public MedicineLotDTO() {
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

    public BigDecimal getImportPrice() {
        return importPrice;
    }

    public void setImportPrice(BigDecimal importPrice) {
        this.importPrice = importPrice;
    }

    public Integer getQuantityIn() {
        return quantityIn != null ? quantityIn : 0;
    }

    public void setQuantityIn(Integer quantityIn) {
        this.quantityIn = quantityIn;
    }

    public Integer getQuantityRemaining() {
        return quantityRemaining != null ? quantityRemaining : 0;
    }

    public void setQuantityRemaining(Integer quantityRemaining) {
        this.quantityRemaining = quantityRemaining;
    }

    public LocalDateTime getTransactionAt() {
        return transactionAt;
    }

    public void setTransactionAt(LocalDateTime transactionAt) {
        this.transactionAt = transactionAt;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSupplierName() {
        return supplierName != null && !supplierName.isBlank() ? supplierName : "—";
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /**
     * Xác định trạng thái của lô thuốc
     */
    public LotStatus getStatus() {
        if (getQuantityRemaining() <= 0) {
            return LotStatus.DEPLETED;
        }
        if (expiryDate == null) {
            return LotStatus.ACTIVE;
        }
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today) || expiryDate.isEqual(today)) {
            return LotStatus.EXPIRED;
        }
        if (expiryDate.isBefore(today.plusDays(90))) {
            return LotStatus.NEAR_EXPIRY;
        }
        return LotStatus.ACTIVE;
    }

    public String getStatusLabel() {
        return getStatus().getLabel();
    }

    public String getStatusStyleClass() {
        return getStatus().getStyleClass();
    }

    public String getFormattedExpiryDate() {
        return expiryDate != null ? com.pharmacy.common.FormatUtils.formatDate(expiryDate) : "—";
    }

    public String getFormattedTransactionDate() {
        if (transactionAt != null) {
            return com.pharmacy.common.FormatUtils.formatDate(transactionAt.toLocalDate());
        }
        return "—";
    }

    public String getFormattedImportPrice() {
        if (importPrice != null) {
            return com.pharmacy.common.FormatUtils.formatCurrency(importPrice) + " đ";
        }
        return "0 đ";
    }

    public BigDecimal getTotalRemainingValue() {
        if (importPrice == null || getQuantityRemaining() <= 0) {
            return BigDecimal.ZERO;
        }
        return importPrice.multiply(BigDecimal.valueOf(getQuantityRemaining()));
    }

    public String getFormattedTotalValue() {
        return com.pharmacy.common.FormatUtils.formatCurrency(getTotalRemainingValue()) + " đ";
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
}
