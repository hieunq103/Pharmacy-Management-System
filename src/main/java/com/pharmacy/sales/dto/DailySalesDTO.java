package com.pharmacy.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailySalesDTO {
    private final LocalDate date;
    private final String dayOfWeekLabel;
    private final BigDecimal amount;
    private final long orderCount;
    private final long totalQuantity;

    public DailySalesDTO(LocalDate date, String dayOfWeekLabel, BigDecimal amount, long orderCount) {
        this(date, dayOfWeekLabel, amount, orderCount, 0L);
    }

    public DailySalesDTO(LocalDate date, String dayOfWeekLabel, BigDecimal amount, long orderCount, long totalQuantity) {
        this.date = date;
        this.dayOfWeekLabel = dayOfWeekLabel;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.orderCount = orderCount;
        this.totalQuantity = totalQuantity;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDayOfWeekLabel() {
        return dayOfWeekLabel;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getOrderCount() {
        return orderCount;
    }

    public long getTotalQuantity() {
        return totalQuantity;
    }
}
