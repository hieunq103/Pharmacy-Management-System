package com.pharmacy.dashboard.dto;

import com.pharmacy.common.FormatUtils;
import java.math.BigDecimal;

public class DashboardStatsDTO {

    private final BigDecimal todayRevenue;
    private final BigDecimal yesterdayRevenue;
    private final double revenueGrowthPercent;
    private final long todayOrderCount;
    private final long yesterdayOrderCount;
    private final long orderCountDiff;
    private final long lowStockCount;
    private final long nearExpiryCount;

    public DashboardStatsDTO(com.pharmacy.sales.dto.TodaySalesSummary salesSummary,
                             long lowStockCount, long nearExpiryCount) {
        this(
            salesSummary != null ? salesSummary.todayRevenue() : BigDecimal.ZERO,
            salesSummary != null ? salesSummary.yesterdayRevenue() : BigDecimal.ZERO,
            salesSummary != null ? salesSummary.todayOrderCount() : 0L,
            salesSummary != null ? salesSummary.yesterdayOrderCount() : 0L,
            lowStockCount,
            nearExpiryCount
        );
    }

    public DashboardStatsDTO(BigDecimal todayRevenue, BigDecimal yesterdayRevenue,
                             long todayOrderCount, long yesterdayOrderCount,
                             long lowStockCount, long nearExpiryCount) {
        this.todayRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;
        this.yesterdayRevenue = yesterdayRevenue != null ? yesterdayRevenue : BigDecimal.ZERO;
        this.todayOrderCount = todayOrderCount;
        this.yesterdayOrderCount = yesterdayOrderCount;
        this.orderCountDiff = todayOrderCount - yesterdayOrderCount;
        this.lowStockCount = lowStockCount;
        this.nearExpiryCount = nearExpiryCount;

        if (this.yesterdayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = this.todayRevenue.subtract(this.yesterdayRevenue);
            this.revenueGrowthPercent = diff.doubleValue() / this.yesterdayRevenue.doubleValue() * 100.0;
        } else if (this.todayRevenue.compareTo(BigDecimal.ZERO) > 0) {
            this.revenueGrowthPercent = 100.0;
        } else {
            this.revenueGrowthPercent = 0.0;
        }
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public BigDecimal getYesterdayRevenue() {
        return yesterdayRevenue;
    }

    public double getRevenueGrowthPercent() {
        return revenueGrowthPercent;
    }

    public long getTodayOrderCount() {
        return todayOrderCount;
    }

    public long getYesterdayOrderCount() {
        return yesterdayOrderCount;
    }

    public long getOrderCountDiff() {
        return orderCountDiff;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public long getNearExpiryCount() {
        return nearExpiryCount;
    }

    public String getFormattedTodayRevenue() {
        return FormatUtils.formatCurrency(todayRevenue) + "đ";
    }

    public String getRevenueGrowthText() {
        if (revenueGrowthPercent > 0) {
            return String.format("+%.1f%% so với hôm qua", revenueGrowthPercent);
        } else if (revenueGrowthPercent < 0) {
            return String.format("%.1f%% so với hôm qua", revenueGrowthPercent);
        } else {
            return "0.0% so với hôm qua";
        }
    }

    public boolean isRevenueGrowthPositive() {
        return revenueGrowthPercent >= 0;
    }

    public String getOrderGrowthText() {
        if (orderCountDiff > 0) {
            return String.format("+%d hóa đơn", orderCountDiff);
        } else if (orderCountDiff < 0) {
            return String.format("%d hóa đơn", orderCountDiff);
        } else {
            return "Bằng hôm qua";
        }
    }

    public boolean isOrderGrowthPositive() {
        return orderCountDiff >= 0;
    }
}
