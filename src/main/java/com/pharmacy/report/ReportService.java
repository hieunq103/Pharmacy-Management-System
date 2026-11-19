package com.pharmacy.report;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UserSession;
import com.pharmacy.report.dto.DailySalesDTO;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import com.pharmacy.report.dto.TodaySalesSummary;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class ReportService {

    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private final ReportRepository repository;

    public ReportService() {
        this(new ReportRepository());
    }

    public ReportService(ReportRepository repository) {
        this.repository = repository;
    }

    public TodaySalesSummary getTodaySalesSummary() {
        return repository.getTodaySalesSummary(UserSession.scopeEmployeeId(null));
    }

    public ReportSummaryDTO getSummary(LocalDate from, LocalDate to, Integer employeeId) {
        return repository.getReportSummary(from, to, UserSession.scopeEmployeeId(employeeId));
    }

    public List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword, Integer employeeId) {
        return repository.getSoldMedicines(from, to, keyword, UserSession.scopeEmployeeId(employeeId));
    }

    public List<DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate, Integer employeeId) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return repository.get7DaysSalesUpTo(effectiveEnd, UserSession.scopeEmployeeId(employeeId));
    }

    private record CacheKey(LocalDate endDate, Integer employeeId) {}

    private CacheKey lastCacheKey;
    private long lastCacheTimeMs = 0;
    private List<DailySalesDTO> cached7DaysSales = Collections.emptyList();

    private synchronized List<DailySalesDTO> getCached7DaysSales(LocalDate endDate, Integer employeeId) {
        long now = System.currentTimeMillis();
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        CacheKey currentKey = new CacheKey(effectiveEnd, UserSession.scopeEmployeeId(employeeId));
        if (currentKey.equals(lastCacheKey) && now - lastCacheTimeMs < 2000) {
            return cached7DaysSales;
        }
        cached7DaysSales = repository.get7DaysSalesUpTo(effectiveEnd, currentKey.employeeId());
        lastCacheKey = currentKey;
        lastCacheTimeMs = now;
        return cached7DaysSales;
    }

    public List<RevenueTimelineDTO> get7DaysRevenueTimelineUpTo(LocalDate endDate, Integer employeeId) {
        return getCached7DaysSales(endDate, employeeId)
            .stream()
            .map(d ->
                new RevenueTimelineDTO(
                    d.getDate().format(SHORT_FMT),
                    d.getAmount(),
                    "Ngày " +
                        d.getDate().format(FormatUtils.DATE_FORMATTER) +
                        ": " +
                        FormatUtils.formatCurrency(d.getAmount()) +
                        " VNĐ"
                )
            )
            .toList();
    }

    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId) {
        return repository.get12MonthsRevenueTimeline(year, UserSession.scopeEmployeeId(employeeId));
    }

    public List<MedicineTimelineDTO> get7DaysMedicineTimelineUpTo(LocalDate endDate, Integer employeeId) {
        return getCached7DaysSales(endDate, employeeId)
            .stream()
            .map(d ->
                new MedicineTimelineDTO(
                    d.getDate().format(SHORT_FMT),
                    d.getTotalQuantity(),
                    "Ngày " +
                        d.getDate().format(FormatUtils.DATE_FORMATTER) +
                        ": " +
                        String.format("%,d", d.getTotalQuantity()) +
                        " sản phẩm"
                )
            )
            .toList();
    }

    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId) {
        return repository.get12MonthsMedicineTimeline(year, UserSession.scopeEmployeeId(employeeId));
    }
}
