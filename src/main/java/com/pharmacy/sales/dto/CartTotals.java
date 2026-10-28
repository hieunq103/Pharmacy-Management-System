package com.pharmacy.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public final class CartTotals {
    private CartTotals() {
    }

    public static BigDecimal subtotal(List<CartItem> items) {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null) {
            for (CartItem item : items) {
                sum = sum.add(item.getLineTotal());
            }
        }
        return sum;
    }

    public static BigDecimal clampDiscount(BigDecimal discount, BigDecimal subtotal) {
        if (discount == null || discount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return discount.min(subtotal);
    }

    public static BigDecimal total(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.subtract(discount).max(BigDecimal.ZERO);
    }

    public static boolean hasPrescriptionItem(List<CartItem> items) {
        return items != null && items.stream().anyMatch(CartItem::isRequiresPrescription);
    }
}
