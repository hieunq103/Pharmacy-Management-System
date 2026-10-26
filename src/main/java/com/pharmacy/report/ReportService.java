package com.pharmacy.report;

import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo doanh số / sản lượng. Tham số employeeId = null nghĩa là toàn cửa hàng;
 * tài khoản Nhân viên luôn bị giới hạn về dữ liệu của chính mình.
 */
public interface ReportService {
    ReportSummaryDTO getSummary(LocalDate from, LocalDate to, Integer employeeId);

    List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword, Integer employeeId);

    List<RevenueTimelineDTO> get7DaysRevenueTimelineUpTo(LocalDate endDate, Integer employeeId);
    List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId);

    List<MedicineTimelineDTO> get7DaysMedicineTimelineUpTo(LocalDate endDate, Integer employeeId);
    List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId);
}
