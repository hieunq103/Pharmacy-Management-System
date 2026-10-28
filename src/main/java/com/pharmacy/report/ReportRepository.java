package com.pharmacy.report;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.pharmacy.common.SqlRows.*;

public class ReportRepository {
    private static final Logger log = LoggerFactory.getLogger(ReportRepository.class);

    // Lấy tổng quan báo cáo doanh số, số đơn hàng và số lượng thuốc bán
    public ReportSummaryDTO getReportSummary(LocalDate from, LocalDate to, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT COUNT(*) AS total_orders,
                       COALESCE(SUM(so.total_amount), 0) AS total_revenue,
                       COALESCE(SUM(CASE WHEN so.payment_method = 'CASH' THEN so.total_amount END), 0) AS cash_revenue,
                       COALESCE(SUM(CASE WHEN so.payment_method = 'TRANSFER' THEN so.total_amount END), 0) AS transfer_revenue,
                       COALESCE(SUM(q.qty), 0) AS total_qty
                FROM sales_orders so
                LEFT JOIN LATERAL (
                    SELECT SUM(soi.quantity) AS qty FROM sales_order_items soi WHERE soi.sales_order_id = so.id
                ) q ON TRUE
                WHERE so.created_at >= :startTime AND so.created_at < :endTime
            """ + employeeFilter(employeeId);
            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            bindRange(query, from, to, employeeId);
            Object[] r = query.uniqueResult();
            return new ReportSummaryDTO(toLong(r[0]), toBigDecimal(r[1]), toBigDecimal(r[2]), toBigDecimal(r[3]), toLong(r[4]));
        } catch (Exception e) {
            log.error("Lỗi khi lấy tổng hợp báo cáo: ", e);
            return ReportSummaryDTO.empty();
        }
    }

    // Lấy danh sách thuốc đã bán kèm số lượng và doanh thu trong khoảng thời gian
    public List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            boolean hasKeyword = keyword != null && !keyword.isBlank();
            String sql = """
                SELECT m.id, m.code, m.name, COALESCE(mc.name, 'Chưa phân loại') AS category_name, m.unit,
                       agg.quantity_sold, agg.total_revenue
                FROM (
                    SELECT soi.medicine_id, SUM(soi.quantity) AS quantity_sold, SUM(soi.line_total) AS total_revenue
                    FROM sales_orders so
                    JOIN sales_order_items soi ON soi.sales_order_id = so.id
                    WHERE so.created_at >= :startTime AND so.created_at < :endTime
            """ + employeeFilter(employeeId) + """
                    GROUP BY soi.medicine_id
                ) agg
                JOIN medicines m ON m.id = agg.medicine_id
                LEFT JOIN medicine_categories mc ON m.category_id = mc.id
            """ + (hasKeyword ? " WHERE (m.code LIKE :keyword OR m.name LIKE :keyword OR mc.name LIKE :keyword) " : "") + """
                ORDER BY agg.quantity_sold DESC, agg.total_revenue DESC
            """;
            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            bindRange(query, from, to, employeeId);
            if (hasKeyword) {
                query.setParameter("keyword", "%" + keyword.trim() + "%");
            }

            List<SoldMedicineDTO> result = new ArrayList<>();
            for (Object[] r : query.list()) {
                result.add(new SoldMedicineDTO(toInteger(r[0]), toStr(r[1]), toStr(r[2]), toStr(r[3]), toStr(r[4]),
                        toLong(r[5]), toBigDecimal(r[6])));
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thuốc đã bán: ", e);
            return Collections.emptyList();
        }
    }

    // Lấy biểu đồ doanh thu theo 12 tháng của năm
    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT MONTH(so.created_at) AS month_val, COALESCE(SUM(so.total_amount), 0), COUNT(*)
                FROM sales_orders so
                WHERE so.created_at >= :startTime AND so.created_at < :endTime
            """ + employeeFilter(employeeId) + " GROUP BY MONTH(so.created_at)";
            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            bindRange(query, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), employeeId);
            Map<Integer, Object[]> byMonth = groupByMonth(query.list());

            List<RevenueTimelineDTO> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                Object[] r = byMonth.get(m);
                BigDecimal rev = r != null ? toBigDecimal(r[1]) : BigDecimal.ZERO;
                long orders = r != null ? toLong(r[2]) : 0L;
                String tooltip = "Tháng " + m + "/" + year + ": " + FormatUtils.formatCurrency(rev) + " VNĐ";
                result.add(new RevenueTimelineDTO("Thg " + m, rev, orders, tooltip));
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy doanh số 12 tháng: ", e);
            return Collections.emptyList();
        }
    }

    // Lấy biểu đồ số lượng thuốc bán theo 12 tháng của năm
    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = """
                SELECT MONTH(so.created_at) AS month_val, COALESCE(SUM(soi.quantity), 0)
                FROM sales_orders so
                JOIN sales_order_items soi ON so.id = soi.sales_order_id
                WHERE so.created_at >= :startTime AND so.created_at < :endTime
            """ + employeeFilter(employeeId) + " GROUP BY MONTH(so.created_at)";
            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            bindRange(query, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), employeeId);
            Map<Integer, Object[]> byMonth = groupByMonth(query.list());

            List<MedicineTimelineDTO> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                Object[] r = byMonth.get(m);
                long qty = r != null ? toLong(r[1]) : 0L;
                String tooltip = "Tháng " + m + "/" + year + ": " + String.format("%,d", qty) + " sản phẩm";
                result.add(new MedicineTimelineDTO("Thg " + m, qty, tooltip));
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy sản lượng thuốc 12 tháng: ", e);
            return Collections.emptyList();
        }
    }

    // Gom nhóm kết quả truy vấn theo tháng
    private static Map<Integer, Object[]> groupByMonth(List<Object[]> rows) {
        Map<Integer, Object[]> byMonth = new HashMap<>();
        for (Object[] r : rows) {
            byMonth.put(toInt(r[0]), r);
        }
        return byMonth;
    }

    // Tạo điều kiện SQL lọc theo nhân viên nếu có
    private static String employeeFilter(Integer employeeId) {
        return employeeId != null ? " AND so.employee_id = :employeeId " : "";
    }

    // Gán tham số khoảng ngày và nhân viên vào câu truy vấn
    private static void bindRange(NativeQuery<?> query, LocalDate from, LocalDate to, Integer employeeId) {
        query.setParameter("startTime", Timestamp.valueOf(from.atStartOfDay()));
        query.setParameter("endTime", Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        if (employeeId != null) {
            query.setParameter("employeeId", employeeId);
        }
    }
}
