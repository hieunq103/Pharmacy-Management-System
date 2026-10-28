package com.pharmacy.report;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.UserSession;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import com.pharmacy.sales.SalesService;
import com.pharmacy.sales.SalesServiceImpl;
import com.pharmacy.sales.dto.DailySalesDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * ReportService hoạt động theo mô hình Aggregator / Facade:
 * Tái sử dụng SalesService cho biểu đồ 7 ngày, kết hợp ReportRepository cho thống kê chuyên sâu.
 */
public class ReportServiceImpl implements ReportService {

    private static final DateTimeFormatter SHORT_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private final SalesService salesService;
    private final ReportRepository repository;

    public ReportServiceImpl() {
        this(new SalesServiceImpl(), new ReportRepository());
    }

    public ReportServiceImpl(SalesService salesService, ReportRepository repository) {
        this.salesService = salesService;
        this.repository = repository;
    }

    @Override
    public ReportSummaryDTO getSummary(LocalDate from, LocalDate to, Integer employeeId) {
        return repository.getReportSummary(from, to, UserSession.scopeEmployeeId(employeeId));
    }

    @Override
    public List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword, Integer employeeId) {
        return repository.getSoldMedicines(from, to, keyword, UserSession.scopeEmployeeId(employeeId));
    }

    // Biểu đồ doanh số và sản lượng 7 ngày được vẽ cùng lúc -> dùng chung 1 lần truy vấn trong 2 giây
    private record CacheKey(LocalDate endDate, Integer employeeId) {}
    private CacheKey lastCacheKey;
    private long lastCacheTimeMs = 0;
    private List<DailySalesDTO> cached7DaysSales = Collections.emptyList();

    private synchronized List<DailySalesDTO> getCached7DaysSales(LocalDate endDate, Integer employeeId) {
        long now = System.currentTimeMillis();
        CacheKey currentKey = new CacheKey(endDate, UserSession.scopeEmployeeId(employeeId));
        if (currentKey.equals(lastCacheKey) && (now - lastCacheTimeMs < 2000)) {
            return cached7DaysSales;
        }
        cached7DaysSales = salesService.get7DaysSalesUpTo(endDate, currentKey.employeeId());
        lastCacheKey = currentKey;
        lastCacheTimeMs = now;
        return cached7DaysSales;
    }

    @Override
    public List<RevenueTimelineDTO> get7DaysRevenueTimelineUpTo(LocalDate endDate, Integer employeeId) {
        return getCached7DaysSales(endDate, employeeId).stream()
                .map(d -> new RevenueTimelineDTO(d.getDate().format(SHORT_FMT), d.getAmount(), d.getOrderCount(),
                        "Ngày " + d.getDate().format(FormatUtils.DATE_FORMATTER) + ": " + FormatUtils.formatCurrency(d.getAmount()) + " VNĐ"))
                .toList();
    }

    @Override
    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId) {
        return repository.get12MonthsRevenueTimeline(year, UserSession.scopeEmployeeId(employeeId));
    }

    @Override
    public List<MedicineTimelineDTO> get7DaysMedicineTimelineUpTo(LocalDate endDate, Integer employeeId) {
        return getCached7DaysSales(endDate, employeeId).stream()
                .map(d -> new MedicineTimelineDTO(d.getDate().format(SHORT_FMT), d.getTotalQuantity(),
                        "Ngày " + d.getDate().format(FormatUtils.DATE_FORMATTER) + ": " + String.format("%,d", d.getTotalQuantity()) + " sản phẩm"))
                .toList();
    }

    @Override
    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId) {
        return repository.get12MonthsMedicineTimeline(year, UserSession.scopeEmployeeId(employeeId));
    }
}
