package com.pharmacy.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public class SalesOrderRequest {
    private Integer employeeId;
    private String customerPhone;
    private BigDecimal discount;
    private String paymentMethod;
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

    public BigDecimal getDiscount() {
        return CartTotals.clampDiscount(discount, getSubtotal());
    }

    public String getPaymentMethod() { return paymentMethod != null ? paymentMethod : "CASH"; }

    public boolean isPrescriptionConfirmed() { return prescriptionConfirmed; }

    public List<CartItem> getItems() { return items; }

    public BigDecimal getSubtotal() {
        return CartTotals.subtotal(items);
    }

    public boolean hasPrescriptionItem() {
        return CartTotals.hasPrescriptionItem(items);
    }
}
