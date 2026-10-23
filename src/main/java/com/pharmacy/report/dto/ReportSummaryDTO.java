package com.pharmacy.report.dto;

import java.math.BigDecimal;

public record ReportSummaryDTO(
        long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal cashRevenue,
        BigDecimal transferRevenue,
        long totalMedicinesSold
) {
    public static ReportSummaryDTO empty() {
        return new ReportSummaryDTO(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }
}
