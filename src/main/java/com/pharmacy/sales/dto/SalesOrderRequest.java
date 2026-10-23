package com.pharmacy.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public class SalesOrderRequest {

    private Integer employeeId;
    private String customerPhone;
    private BigDecimal discount;
    private String paymentMethod; // "CASH" or "TRANSFER"
    private boolean prescriptionConfirmed;
    private List<CartItem> items;
    private String orderCode;

    public SalesOrderRequest() {}

    public SalesOrderRequest(Integer employeeId, String customerPhone, BigDecimal discount,
                             String paymentMethod, boolean prescriptionConfirmed, List<CartItem> items) {
        this.employeeId = employeeId;
        this.customerPhone = customerPhone;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.paymentMethod = paymentMethod != null ? paymentMethod : "CASH";
        this.prescriptionConfirmed = prescriptionConfirmed;
        this.items = items;
    }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public BigDecimal getDiscount() { 
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal sub = getSubtotal();
        if (discount.compareTo(sub) > 0) {
            return sub;
        }
        return discount; 
    }

    public void setDiscount(BigDecimal discount) { 
        if (discount == null || discount.compareTo(BigDecimal.ZERO) <= 0) {
            this.discount = BigDecimal.ZERO;
        } else {
            BigDecimal sub = getSubtotal();
            if (discount.compareTo(sub) > 0) {
                this.discount = sub;
            } else {
                this.discount = discount;
            }
        }
    }

    public String getPaymentMethod() { return paymentMethod != null ? paymentMethod : "CASH"; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isPrescriptionConfirmed() { return prescriptionConfirmed; }
    public void setPrescriptionConfirmed(boolean prescriptionConfirmed) { this.prescriptionConfirmed = prescriptionConfirmed; }

    public List<CartItem> items() { return items; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public BigDecimal getSubtotal() {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (CartItem item : items) {
            sum = sum.add(item.getLineTotal());
        }
        return sum;
    }

    public BigDecimal getTotalAmount() {
        BigDecimal sub = getSubtotal();
        BigDecimal disc = getDiscount();
        BigDecimal total = sub.subtract(disc);
        return total.compareTo(BigDecimal.ZERO) > 0 ? total : BigDecimal.ZERO;
    }

    public boolean hasPrescriptionItem() {
        if (items == null) return false;
        return items.stream().anyMatch(CartItem::isRequiresPrescription);
    }
}
