package com.pharmacy.sales.dto;

import com.pharmacy.common.FormatUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho một đơn bán hàng trong danh sách Lịch sử bán hàng.
 */
public class SalesOrderDTO {

    private Long id;
    private String code;
    private LocalDateTime createdAt;
    private Integer employeeId;
    private String employeeName;
    private String employeeCode;
    private String customerPhone;
    private int itemCount;
    private int totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String paymentMethod; // "CASH", "TRANSFER", "CARD"

    public SalesOrderDTO() {}

    public SalesOrderDTO(Long id, String code, LocalDateTime createdAt, Integer employeeId,
                         String employeeName, String employeeCode, String customerPhone,
                         int itemCount, int totalQuantity, BigDecimal subtotal,
                         BigDecimal discount, BigDecimal totalAmount, String paymentMethod) {
        this.id = id;
        this.code = code;
        this.createdAt = createdAt;
        this.employeeId = employeeId;
        this.employeeName = employeeName != null ? employeeName : "—";
        this.employeeCode = employeeCode != null ? employeeCode : "";
        this.customerPhone = customerPhone != null && !customerPhone.isBlank() ? customerPhone : "Khách lẻ";
        this.itemCount = itemCount;
        this.totalQuantity = totalQuantity;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeCode() { return employeeCode; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    public BigDecimal getSubtotal() { return subtotal; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    // Helpers cho TableView
    public String getFormattedTime() {
        return FormatUtils.formatDateTime(createdAt);
    }

    public String getFormattedSubtotal() {
        return FormatUtils.formatCurrency(subtotal) + "đ";
    }

    public String getFormattedDiscount() {
        if (discount == null || discount.compareTo(BigDecimal.ZERO) == 0) {
            return "0đ";
        }
        return "-" + FormatUtils.formatCurrency(discount) + "đ";
    }

    public String getFormattedTotal() {
        return FormatUtils.formatCurrency(totalAmount) + "đ";
    }

    public String getFormattedPaymentMethod() {
        if ("TRANSFER".equalsIgnoreCase(paymentMethod)) {
            return "Chuyển khoản";
        } else if ("CARD".equalsIgnoreCase(paymentMethod)) {
            return "Thẻ";
        }
        return "Tiền mặt";
    }

    public String getFormattedItemSummary() {
        return itemCount + " loại (Tổng SL: " + totalQuantity + ")";
    }

    public String getEmployeeInitials() {
        return FormatUtils.getInitials(employeeName);
    }

    public String getTimeOnly() {
        return createdAt != null ? createdAt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
    }

    public String getPaymentBadgeClass() {
        if ("TRANSFER".equalsIgnoreCase(paymentMethod)) return "badge-transfer";
        if ("CARD".equalsIgnoreCase(paymentMethod)) return "badge-card";
        return "badge-cash";
    }
}
