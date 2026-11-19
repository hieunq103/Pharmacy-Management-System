package com.pharmacy.report.dto;

import java.math.BigDecimal;

public record RevenueTimelineDTO(String timeLabel, BigDecimal revenue, String tooltipText) {}
