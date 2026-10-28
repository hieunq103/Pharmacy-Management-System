package com.pharmacy.report.dto;

public record MedicineTimelineDTO(
        String timeLabel,
        long quantitySold,
        String tooltipText
) {
}
