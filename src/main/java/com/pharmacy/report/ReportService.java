package com.pharmacy.report;

import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    ReportSummaryDTO getSummaryToday();
    ReportSummaryDTO getSummaryThisMonth();
    ReportSummaryDTO getSummary(LocalDate from, LocalDate to);
    ReportSummaryDTO getSummary(LocalDate from, LocalDate to, Integer employeeId);

    List<SoldMedicineDTO> getSoldMedicinesToday(String keyword);
    List<SoldMedicineDTO> getSoldMedicinesThisMonth(String keyword);
    List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword);
    List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword, Integer employeeId);

    List<RevenueTimelineDTO> getLast7DaysRevenueTimeline();
    List<RevenueTimelineDTO> get7DaysRevenueTimelineUpTo(LocalDate endDate);
    List<RevenueTimelineDTO> get7DaysRevenueTimelineUpTo(LocalDate endDate, Integer employeeId);
    List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year);
    List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId);
    List<RevenueTimelineDTO> getRevenueTimeline(LocalDate from, LocalDate to);
    List<RevenueTimelineDTO> getRevenueTimeline(LocalDate from, LocalDate to, Integer employeeId);

    List<MedicineTimelineDTO> getLast7DaysMedicineTimeline();
    List<MedicineTimelineDTO> get7DaysMedicineTimelineUpTo(LocalDate endDate);
    List<MedicineTimelineDTO> get7DaysMedicineTimelineUpTo(LocalDate endDate, Integer employeeId);
    List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year);
    List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId);
    List<MedicineTimelineDTO> getMedicineTimeline(LocalDate from, LocalDate to);
    List<MedicineTimelineDTO> getMedicineTimeline(LocalDate from, LocalDate to, Integer employeeId);
}
