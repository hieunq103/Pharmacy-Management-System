package com.pharmacy.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO đại diện cho một giao dịch xuất hoặc nhập kho (Master record).
 */
public class InventoryTransactionDTO {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private Long id;
    private String type; // 'IMPORT', 'EXPORT_DISPOSAL', 'EXPORT_SALE', etc.
    private LocalDateTime transactionAt;
    private Integer employeeId;
    private String employeeName;
    private Integer supplierId;
    private String supplierName;
    private int itemCount;
    private int totalQuantity;
    private BigDecimal totalAmount;
    private String note;
    private String exportTypeLabel;

    public InventoryTransactionDTO() {
    }

    public InventoryTransactionDTO(Long id, String type, LocalDateTime transactionAt,
                                  Integer employeeId, String employeeName,
                                  Integer supplierId, String supplierName,
                                  int itemCount, int totalQuantity, BigDecimal totalAmount,
                                  String note) {
        this.id = id;
        this.type = type;
        this.transactionAt = transactionAt;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.itemCount = itemCount;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
        this.note = note;
    }


    public String getFormattedDateTime() {
        if (transactionAt == null) return "N/A";
        return transactionAt.format(DATE_TIME_FORMATTER);
    }

    public String getFormattedDate() {
        if (transactionAt == null) return "N/A";
        return transactionAt.format(DATE_FORMATTER);
    }

    public String getFormattedTime() {
        if (transactionAt == null) return "N/A";
        return transactionAt.format(TIME_FORMATTER);
    }

    public String getTypeDisplayName() {
        if (type == null) return "Không xác định";
        return switch (type.toUpperCase()) {
            case "IMPORT" -> "Nhập kho";
            case "EXPORT_DISPOSAL" -> "Xuất hủy";
            case "EXPORT_SALE" -> "Xuất bán lẻ";
            default -> type;
        };
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTransactionAt() {
        return transactionAt;
    }

    public void setTransactionAt(LocalDateTime transactionAt) {
        this.transactionAt = transactionAt;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName != null && !employeeName.isBlank() ? employeeName : "N/A";
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Integer getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Integer supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName != null && !supplierName.isBlank() ? supplierName : "—";
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNote() {
        return note != null && !note.isBlank() ? note : "—";
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getExportTypeLabel() {
        if (exportTypeLabel != null && !exportTypeLabel.isBlank()) {
            return exportTypeLabel;
        }
        return getTypeDisplayName();
    }

    public void setExportTypeLabel(String exportTypeLabel) {
        this.exportTypeLabel = exportTypeLabel;
    }
}
