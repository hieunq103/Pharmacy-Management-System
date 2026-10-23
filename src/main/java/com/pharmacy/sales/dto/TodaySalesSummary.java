package com.pharmacy.sales.dto;

import com.pharmacy.common.FormatUtils;
import java.math.BigDecimal;

public record TodaySalesSummary(
        BigDecimal todayRevenue,
        BigDecimal yesterdayRevenue,
        double revenueGrowthPercent,
        long todayOrderCount,
        long yesterdayOrderCount,
        long orderCountDiff
) {
    public static TodaySalesSummary of(BigDecimal todayRevenue, BigDecimal yesterdayRevenue,
                                       long todayOrderCount, long yesterdayOrderCount) {
        BigDecimal tRev = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;
        BigDecimal yRev = yesterdayRevenue != null ? yesterdayRevenue : BigDecimal.ZERO;

        double growth = 0.0;
        if (yRev.compareTo(BigDecimal.ZERO) > 0) {
            growth = tRev.subtract(yRev).doubleValue() / yRev.doubleValue() * 100.0;
        } else if (tRev.compareTo(BigDecimal.ZERO) > 0) {
            growth = 100.0;
        }

        long diff = todayOrderCount - yesterdayOrderCount;
        return new TodaySalesSummary(tRev, yRev, growth, todayOrderCount, yesterdayOrderCount, diff);
    }

    public String getFormattedTodayRevenue() {
        return FormatUtils.formatCurrency(todayRevenue) + "đ";
    }

    public String getRevenueGrowthText() {
        if (revenueGrowthPercent > 0) return String.format("+%.1f%% so với hôm qua", revenueGrowthPercent);
        if (revenueGrowthPercent < 0) return String.format("%.1f%% so với hôm qua", revenueGrowthPercent);
        return "0.0% so với hôm qua";
    }

    public boolean isRevenueGrowthPositive() {
        return revenueGrowthPercent >= 0;
    }

    public String getOrderGrowthText() {
        if (orderCountDiff > 0) return String.format("+%d hóa đơn", orderCountDiff);
        if (orderCountDiff < 0) return String.format("%d hóa đơn", orderCountDiff);
        return "Bằng hôm qua";
    }

    public boolean isOrderGrowthPositive() {
        return orderCountDiff >= 0;
    }
}
