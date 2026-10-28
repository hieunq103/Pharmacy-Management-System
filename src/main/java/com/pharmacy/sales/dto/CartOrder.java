package com.pharmacy.sales.dto;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.math.BigDecimal;

public class CartOrder {

    private final int id;
    private String name;
    private final ObservableList<CartItem> items = FXCollections.observableArrayList();
    private String customerPhone = "";
    private BigDecimal discount = BigDecimal.ZERO;

    public CartOrder(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ObservableList<CartItem> getItems() { return items; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public BigDecimal getDiscount() {
        return CartTotals.clampDiscount(discount, getSubtotal());
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = CartTotals.clampDiscount(discount, getSubtotal());
    }

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getSubtotal() {
        return CartTotals.subtotal(items);
    }

    public BigDecimal getTotalAmount() {
        return CartTotals.total(getSubtotal(), getDiscount());
    }

    public boolean hasPrescriptionItem() {
        return CartTotals.hasPrescriptionItem(items);
    }

    public void addItem(CartItem newItem) {
        for (CartItem existing : items) {
            if (existing.getMedicineId().equals(newItem.getMedicineId())) {
                existing.setQuantity(existing.getQuantity() + newItem.getQuantity());
                return;
            }
        }
        items.add(newItem);
    }

    public void removeItem(CartItem item) {
        items.remove(item);
    }

    public void clear() {
        items.clear();
        customerPhone = "";
        discount = BigDecimal.ZERO;
    }
}
