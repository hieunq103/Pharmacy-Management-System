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

    public int getItemCount() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public BigDecimal getSubtotal() {
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
        return items.stream().anyMatch(CartItem::isRequiresPrescription);
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
