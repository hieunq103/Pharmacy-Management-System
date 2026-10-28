package com.pharmacy.dashboard.dto;

import com.pharmacy.sales.dto.TodaySalesSummary;

/** 4 chỉ số trên các thẻ trang Tổng quan. */
public record DashboardStatsDTO(TodaySalesSummary todaySales, long lowStockCount, long nearExpiryCount) {

    public DashboardStatsDTO {
        todaySales = todaySales != null ? todaySales : TodaySalesSummary.EMPTY;
    }
}
