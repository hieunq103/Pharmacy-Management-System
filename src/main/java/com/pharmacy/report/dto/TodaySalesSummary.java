package com.pharmacy.report.dto;

import java.math.BigDecimal;

public record TodaySalesSummary(BigDecimal revenue, long orderCount) {
    public static final TodaySalesSummary EMPTY = new TodaySalesSummary(BigDecimal.ZERO, 0L);

    public TodaySalesSummary {
        revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }
}
