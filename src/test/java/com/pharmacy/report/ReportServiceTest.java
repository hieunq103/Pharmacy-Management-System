package com.pharmacy.report;

import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử phân hệ Báo cáo & Thống kê")
class ReportServiceTest {

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl();
    }

    @Test
    @DisplayName("Lấy tổng hợp báo cáo hôm nay")
    void testGetSummaryToday() {
        ReportSummaryDTO summary = reportService.getSummaryToday();
        assertNotNull(summary, "ReportSummaryDTO không được null");
        assertNotNull(summary.totalRevenue(), "Doanh thu không được null");
        assertTrue(summary.totalOrders() >= 0, "Tổng số hóa đơn phải >= 0");
        assertTrue(summary.totalMedicinesSold() >= 0, "Số lượng thuốc đã bán phải >= 0");
    }

    @Test
    @DisplayName("Lấy tổng hợp báo cáo tháng này")
    void testGetSummaryThisMonth() {
        ReportSummaryDTO summary = reportService.getSummaryThisMonth();
        assertNotNull(summary, "ReportSummaryDTO tháng này không được null");
        assertNotNull(summary.totalRevenue());
        assertTrue(summary.totalOrders() >= 0);
    }

    @Test
    @DisplayName("Lấy danh sách thuốc đã bán trong ngày và trong tháng")
    void testGetSoldMedicines() {
        List<SoldMedicineDTO> todayMeds = reportService.getSoldMedicinesToday(null);
        assertNotNull(todayMeds, "Danh sách thuốc bán hôm nay không được null");

        List<SoldMedicineDTO> monthMeds = reportService.getSoldMedicinesThisMonth(null);
        assertNotNull(monthMeds, "Danh sách thuốc bán tháng này không được null");
    }

    @Test
    @DisplayName("Lấy timeline biểu đồ 7 ngày gần nhất và 12 tháng")
    void testGetRevenueTimeline() {
        // 1. Biểu đồ 7 ngày gần nhất
        List<RevenueTimelineDTO> sevenDays = reportService.getLast7DaysRevenueTimeline();
        assertNotNull(sevenDays, "Dữ liệu biểu đồ 7 ngày không được null");
        assertEquals(7, sevenDays.size(), "Biểu đồ theo ngày phải có đúng 7 cột đại diện cho 7 ngày gần nhất");

        // 2. Biểu đồ 12 tháng trong năm hiện tại
        int currentYear = LocalDate.now().getYear();
        List<RevenueTimelineDTO> twelveMonths = reportService.get12MonthsRevenueTimeline(currentYear);
        assertNotNull(twelveMonths, "Dữ liệu biểu đồ 12 tháng không được null");
        assertEquals(12, twelveMonths.size(), "Biểu đồ theo tháng phải có đúng 12 cột đại diện cho 12 tháng");
        assertEquals("Thg 1", twelveMonths.get(0).timeLabel(), "Cột đầu tiên phải là Thg 1");
        assertEquals("Thg 12", twelveMonths.get(11).timeLabel(), "Cột cuối cùng phải là Thg 12");
    }

    @Test
    @DisplayName("Lấy timeline biểu đồ sản lượng thuốc 7 ngày gần nhất và 12 tháng")
    void testGetMedicineTimeline() {
        // 1. Biểu đồ sản lượng thuốc 7 ngày gần nhất
        List<com.pharmacy.report.dto.MedicineTimelineDTO> sevenDays = reportService.getLast7DaysMedicineTimeline();
        assertNotNull(sevenDays, "Dữ liệu biểu đồ sản lượng thuốc 7 ngày không được null");
        assertEquals(7, sevenDays.size(), "Biểu đồ theo ngày phải có đúng 7 cột đại diện cho 7 ngày gần nhất");

        // 2. Biểu đồ sản lượng thuốc 12 tháng trong năm hiện tại
        int currentYear = LocalDate.now().getYear();
        List<com.pharmacy.report.dto.MedicineTimelineDTO> twelveMonths = reportService.get12MonthsMedicineTimeline(currentYear);
        assertNotNull(twelveMonths, "Dữ liệu biểu đồ sản lượng thuốc 12 tháng không được null");
        assertEquals(12, twelveMonths.size(), "Biểu đồ theo tháng phải có đúng 12 cột đại diện cho 12 tháng");
        assertEquals("Thg 1", twelveMonths.get(0).timeLabel());
        assertEquals("Thg 12", twelveMonths.get(11).timeLabel());
    }

    @Test
    @DisplayName("Lấy timeline biểu đồ 7 ngày kết thúc tại một ngày bất kỳ (ví dụ 15/05/2026)")
    void testGet7DaysTimelineUpToArbitraryDate() {
        LocalDate selectedDate = LocalDate.of(2026, 5, 15);

        // Biểu đồ doanh số 7 ngày (09/05 - 15/05)
        List<RevenueTimelineDTO> rev7Days = reportService.get7DaysRevenueTimelineUpTo(selectedDate);
        assertNotNull(rev7Days);
        assertEquals(7, rev7Days.size(), "Phải có đúng 7 ngày tính đến ngày được chọn");
        assertEquals("09/05", rev7Days.get(0).timeLabel());
        assertEquals("15/05", rev7Days.get(6).timeLabel());

        // Biểu đồ sản lượng thuốc 7 ngày (09/05 - 15/05)
        List<com.pharmacy.report.dto.MedicineTimelineDTO> med7Days = reportService.get7DaysMedicineTimelineUpTo(selectedDate);
        assertNotNull(med7Days);
        assertEquals(7, med7Days.size());
        assertEquals("09/05", med7Days.get(0).timeLabel());
        assertEquals("15/05", med7Days.get(6).timeLabel());
    }


    @Test
    @DisplayName("Lấy báo cáo và biểu đồ lọc theo nhân viên cụ thể")
    void testReportWithEmployeeFilter() {
        LocalDate today = LocalDate.now();
        Integer testEmpId = 1;

        ReportSummaryDTO empSummary = reportService.getSummary(today, today, testEmpId);
        assertNotNull(empSummary);

        List<RevenueTimelineDTO> empRev7Days = reportService.get7DaysRevenueTimelineUpTo(today, testEmpId);
        assertNotNull(empRev7Days);
        assertEquals(7, empRev7Days.size());

        List<com.pharmacy.report.dto.MedicineTimelineDTO> empMed7Days = reportService.get7DaysMedicineTimelineUpTo(today, testEmpId);
        assertNotNull(empMed7Days);
        assertEquals(7, empMed7Days.size());

        List<RevenueTimelineDTO> emp12Months = reportService.get12MonthsRevenueTimeline(2026, testEmpId);
        assertNotNull(emp12Months);
        assertEquals(12, emp12Months.size());

        List<SoldMedicineDTO> empSoldMeds = reportService.getSoldMedicines(today, today, null, testEmpId);
        assertNotNull(empSoldMeds);
    }
}



