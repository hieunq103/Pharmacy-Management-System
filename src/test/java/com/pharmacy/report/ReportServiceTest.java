package com.pharmacy.report;

import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import com.pharmacy.support.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử phân hệ Báo cáo & Thống kê")
class ReportServiceTest {

    // Dữ liệu seed V4 có đơn bán từ tháng 1 đến tháng 9/2026
    private static final LocalDate MARCH_START = LocalDate.of(2026, 3, 1);
    private static final LocalDate MARCH_END = MARCH_START.with(TemporalAdjusters.lastDayOfMonth());

    private final ReportService reportService = new ReportServiceImpl();

    @BeforeAll
    static void requireDb() {
        DbTestSupport.assumeDatabase();
    }

    @AfterEach
    void logout() {
        DbTestSupport.logout();
    }

    @Test
    @DisplayName("Tổng hợp tháng khớp với biểu đồ 12 tháng và bảng thuốc đã bán")
    void testSummaryConsistentWithTimelines() {
        ReportSummaryDTO march = reportService.getSummary(MARCH_START, MARCH_END, null);
        assertTrue(march.totalOrders() > 0, "Seed V4 phải có đơn tháng 3/2026");
        assertTrue(march.cashRevenue().add(march.transferRevenue()).compareTo(march.totalRevenue()) <= 0);

        List<RevenueTimelineDTO> months = reportService.get12MonthsRevenueTimeline(2026, null);
        assertEquals(12, months.size());
        assertEquals(0, march.totalRevenue().compareTo(months.get(2).revenue()));
        assertEquals(march.totalOrders(), months.get(2).orderCount());

        List<MedicineTimelineDTO> qtyMonths = reportService.get12MonthsMedicineTimeline(2026, null);
        assertEquals(march.totalMedicinesSold(), qtyMonths.get(2).quantitySold());

        List<SoldMedicineDTO> sold = reportService.getSoldMedicines(MARCH_START, MARCH_END, null, null);
        assertEquals(march.totalMedicinesSold(), sold.stream().mapToLong(SoldMedicineDTO::getQuantitySold).sum());
        BigDecimal soldRevenue = sold.stream().map(SoldMedicineDTO::getTotalRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(soldRevenue.compareTo(march.totalRevenue()) >= 0, "Tổng tiền hàng (trước giảm giá) >= doanh thu");
    }

    @Test
    @DisplayName("Biểu đồ 7 ngày tính đến ngày bất kỳ luôn đủ 7 cột và khớp tổng hợp")
    void testGet7DaysTimelineUpToArbitraryDate() {
        LocalDate selectedDate = LocalDate.of(2026, 3, 15);
        List<RevenueTimelineDTO> rev = reportService.get7DaysRevenueTimelineUpTo(selectedDate, null);
        List<MedicineTimelineDTO> med = reportService.get7DaysMedicineTimelineUpTo(selectedDate, null);
        assertEquals(7, rev.size());
        assertEquals(7, med.size());
        assertEquals("09/03", rev.get(0).timeLabel());
        assertEquals("15/03", rev.get(6).timeLabel());

        ReportSummaryDTO week = reportService.getSummary(selectedDate.minusDays(6), selectedDate, null);
        assertEquals(0, week.totalRevenue().compareTo(rev.stream().map(RevenueTimelineDTO::revenue).reduce(BigDecimal.ZERO, BigDecimal::add)));
        assertEquals(week.totalMedicinesSold(), med.stream().mapToLong(MedicineTimelineDTO::quantitySold).sum());
    }

    @Test
    @DisplayName("Nhân viên chỉ xem được báo cáo của chính mình")
    void testStaffScopedToOwnData() {
        Integer staffId = DbTestSupport.loginAs(DbTestSupport.STAFF_PHONE).getEmployee().getId();
        ReportSummaryDTO requestedAll = reportService.getSummary(MARCH_START, MARCH_END, null);
        DbTestSupport.loginAsManager();
        ReportSummaryDTO ownByManager = reportService.getSummary(MARCH_START, MARCH_END, staffId);
        assertEquals(ownByManager, requestedAll, "Staff truyền employeeId = null vẫn chỉ thấy số liệu của mình");
    }
}
