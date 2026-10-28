package com.pharmacy.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InventoryTransactionDTO {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    private Long id;
    private String type;
    private LocalDateTime transactionAt;
    private Integer employeeId;
    private String employeeName;
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
                                  String supplierName,
                                  int itemCount, int totalQuantity, BigDecimal totalAmount,
                                  String note) {
        this.id = id;
        this.type = type;
        this.transactionAt = transactionAt;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
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

    public String getTypeDisplayName() {
        if (type == null) return "Không xác định";
        return switch (type.toUpperCase()) {
            case "IMPORT" -> "Nhập kho";
            case "EXPORT_DISPOSAL" -> "Xuất hủy";
            case "EXPORT_SALE" -> "Xuất bán lẻ";
            default -> type;
        };
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName != null && !employeeName.isBlank() ? employeeName : "N/A";
    }

    public String getSupplierName() {
        return supplierName != null && !supplierName.isBlank() ? supplierName : "—";
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public String getNote() {
        return note != null && !note.isBlank() ? note : "—";
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
