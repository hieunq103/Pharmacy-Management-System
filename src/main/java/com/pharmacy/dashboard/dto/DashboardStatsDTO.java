package com.pharmacy.dashboard.dto;

import com.pharmacy.report.dto.TodaySalesSummary;

public record DashboardStatsDTO(TodaySalesSummary todaySales, long lowStockCount, long nearExpiryCount) {
    public DashboardStatsDTO {
        todaySales = todaySales != null ? todaySales : TodaySalesSummary.EMPTY;
    }
}
