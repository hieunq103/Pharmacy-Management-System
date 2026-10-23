package com.pharmacy.report;

import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;

import com.pharmacy.sales.SalesService;
import com.pharmacy.sales.SalesServiceImpl;
import com.pharmacy.sales.dto.DailySalesDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;


/**
 * ReportService hoạt động theo mô hình Aggregator / Facade:
 * Tái sử dụng trực tiếp các phương thức nghiệp vụ từ SalesService (như get7DaysSales),
 * kết hợp với ReportRepository cho các thống kê tài chính và sản phẩm chuyên sâu.
 */
public class ReportServiceImpl implements ReportService {

    private final SalesService salesService;
    private final ReportRepository repository;

    public ReportServiceImpl() {
        this(new SalesServiceImpl(), new ReportRepository());
    }

    public ReportServiceImpl(ReportRepository repository) {
        this(new SalesServiceImpl(), repository);
    }

    public ReportServiceImpl(SalesService salesService, ReportRepository repository) {
        this.salesService = salesService;
        this.repository = repository;
    }

    @Override
    public ReportSummaryDTO getSummaryToday() {
        LocalDate today = LocalDate.now();
        return getSummary(today, today);
    }

    @Override
    public ReportSummaryDTO getSummaryThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());
        return getSummary(firstDay, lastDay);
    }

    @Override
    public ReportSummaryDTO getSummary(LocalDate from, LocalDate to) {
        return getSummary(from, to, null);
    }

    @Override
    public ReportSummaryDTO getSummary(LocalDate from, LocalDate to, Integer employeeId) {
        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toTime = to.atTime(LocalTime.MAX);
        return repository.getReportSummary(fromTime, toTime, employeeId);
    }

    @Override
    public List<SoldMedicineDTO> getSoldMedicinesToday(String keyword) {
        LocalDate today = LocalDate.now();
        return getSoldMedicines(today, today, keyword);
    }

    @Override
    public List<SoldMedicineDTO> getSoldMedicinesThisMonth(String keyword) {
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate lastDay = today.with(TemporalAdjusters.lastDayOfMonth());
        return getSoldMedicines(firstDay, lastDay, keyword);
    }

    @Override
    public List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword) {
        return getSoldMedicines(from, to, keyword, null);
    }

    @Override
    public List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword, Integer employeeId) {
        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toTime = to.atTime(LocalTime.MAX);
        return repository.getSoldMedicines(fromTime, toTime, keyword, employeeId);
    }

    private static record CacheKey(LocalDate endDate, Integer employeeId) {}
    private CacheKey lastCacheKey;
    private long lastCacheTimeMs = 0;
    private List<DailySalesDTO> cached7DaysSales = Collections.emptyList();

    private synchronized List<DailySalesDTO> getCached7DaysSales(LocalDate endDate, Integer employeeId) {
        long now = System.currentTimeMillis();
        CacheKey currentKey = new CacheKey(endDate, employeeId);
        if (currentKey.equals(lastCacheKey) && (now - lastCacheTimeMs < 2000)) {
            return cached7DaysSales;
        }
        cached7DaysSales = salesService.get7DaysSalesUpTo(endDate, employeeId);
        lastCacheKey = currentKey;
        lastCacheTimeMs = now;
        return cached7DaysSales;
    }

    @Override
    public List<RevenueTimelineDTO> getLast7DaysRevenueTimeline() {
        return get7DaysRevenueTimelineUpTo(LocalDate.now(), null);
    }

    @Override
    public List<RevenueTimelineDTO> get7DaysRevenueTimelineUpTo(LocalDate endDate) {
        return get7DaysRevenueTimelineUpTo(endDate, null);
    }

    @Override
    public List<RevenueTimelineDTO> get7DaysRevenueTimelineUpTo(LocalDate endDate, Integer employeeId) {
        List<DailySalesDTO> dailySales = getCached7DaysSales(endDate, employeeId);
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dailySales.stream()
                .map(d -> {
                    String label = d.getDate().format(shortFmt);
                    String tooltip = "Ngày " + d.getDate().format(fullFmt) + ": " 
                            + com.pharmacy.common.FormatUtils.formatCurrency(d.getAmount()) + " VNĐ";
                    return new RevenueTimelineDTO(label, d.getAmount(), d.getOrderCount(), tooltip);
                })
                .toList();
    }

    @Override
    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year) {
        return get12MonthsRevenueTimeline(year, null);
    }

    @Override
    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId) {
        return repository.get12MonthsRevenueTimeline(year, employeeId);
    }

    @Override
    public List<RevenueTimelineDTO> getRevenueTimeline(LocalDate from, LocalDate to) {
        return getRevenueTimeline(from, to, null);
    }

    @Override
    public List<RevenueTimelineDTO> getRevenueTimeline(LocalDate from, LocalDate to, Integer employeeId) {
        if (from.equals(to)) {
            return get7DaysRevenueTimelineUpTo(to, employeeId);
        } else {
            return repository.getDailyRevenueTimeline(from, to, employeeId);
        }
    }

    @Override
    public List<MedicineTimelineDTO> getLast7DaysMedicineTimeline() {
        return get7DaysMedicineTimelineUpTo(LocalDate.now(), null);
    }

    @Override
    public List<MedicineTimelineDTO> get7DaysMedicineTimelineUpTo(LocalDate endDate) {
        return get7DaysMedicineTimelineUpTo(endDate, null);
    }

    @Override
    public List<MedicineTimelineDTO> get7DaysMedicineTimelineUpTo(LocalDate endDate, Integer employeeId) {
        List<DailySalesDTO> dailySales = getCached7DaysSales(endDate, employeeId);
        DateTimeFormatter shortFmt = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter fullFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dailySales.stream()
                .map(d -> {
                    String label = d.getDate().format(shortFmt);
                    String tooltip = "Ngày " + d.getDate().format(fullFmt) + ": " 
                            + String.format("%,d", d.getTotalQuantity()) + " sản phẩm";
                    return new MedicineTimelineDTO(label, d.getTotalQuantity(), tooltip);
                })
                .toList();
    }

    @Override
    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year) {
        return get12MonthsMedicineTimeline(year, null);
    }

    @Override
    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId) {
        return repository.get12MonthsMedicineTimeline(year, employeeId);
    }

    @Override
    public List<MedicineTimelineDTO> getMedicineTimeline(LocalDate from, LocalDate to) {
        return getMedicineTimeline(from, to, null);
    }

    @Override
    public List<MedicineTimelineDTO> getMedicineTimeline(LocalDate from, LocalDate to, Integer employeeId) {
        if (from.equals(to)) {
            return get7DaysMedicineTimelineUpTo(to, employeeId);
        } else {
            return repository.getDailyMedicineTimeline(from, to, employeeId);
        }
    }
}
