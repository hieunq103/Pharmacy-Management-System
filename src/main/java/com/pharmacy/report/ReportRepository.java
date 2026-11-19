package com.pharmacy.report;

import static com.pharmacy.common.SqlRows.*;

import com.pharmacy.common.FormatUtils;
import com.pharmacy.common.SearchFilter;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.report.dto.DailySalesDTO;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import com.pharmacy.report.dto.TodaySalesSummary;
import com.pharmacy.sales.entity.SalesOrder;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportRepository {

    private static final Logger log = LoggerFactory.getLogger(ReportRepository.class);

    public TodaySalesSummary getTodaySalesSummary(Integer employeeId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            LocalDate today = LocalDate.now();
            String hql =
                """
                    SELECT COALESCE(SUM(so.totalAmount), 0), COUNT(so.id)
                    FROM SalesOrder so
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ + employeeFilter(employeeId);
            SelectionQuery<Object[]> query = session.createSelectionQuery(hql, Object[].class);
            bindRange(query, today, today, employeeId);
            Object[] r = query.uniqueResult();
            return new TodaySalesSummary(toBigDecimal(r[0]), toLong(r[1]));
        } catch (Exception e) {
            log.error("Lỗi khi thống kê doanh số hôm nay trong ReportRepository: ", e);
            return TodaySalesSummary.EMPTY;
        }
    }

    public ReportSummaryDTO getReportSummary(LocalDate from, LocalDate to, Integer employeeId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String orderHql =
                """
                    SELECT COUNT(so.id),
                           COALESCE(SUM(so.totalAmount), 0),
                           COALESCE(SUM(CASE WHEN so.paymentMethod = :cash THEN so.totalAmount ELSE 0 END), 0),
                           COALESCE(SUM(CASE WHEN so.paymentMethod = :transfer THEN so.totalAmount ELSE 0 END), 0)
                    FROM SalesOrder so
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ + employeeFilter(employeeId);
            SelectionQuery<Object[]> orderQuery = session
                .createSelectionQuery(orderHql, Object[].class)
                .setParameter("cash", SalesOrder.PaymentMethod.CASH)
                .setParameter("transfer", SalesOrder.PaymentMethod.TRANSFER);
            bindRange(orderQuery, from, to, employeeId);
            Object[] r = orderQuery.uniqueResult();

            String qtyHql =
                """
                    SELECT COALESCE(SUM(soi.quantity), 0)
                    FROM SalesOrderItem soi
                    JOIN soi.salesOrder so
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ + employeeFilter(employeeId);
            SelectionQuery<Long> qtyQuery = session.createSelectionQuery(qtyHql, Long.class);
            bindRange(qtyQuery, from, to, employeeId);
            long totalQty = toLong(qtyQuery.uniqueResult());

            return new ReportSummaryDTO(
                toLong(r[0]),
                toBigDecimal(r[1]),
                toBigDecimal(r[2]),
                toBigDecimal(r[3]),
                totalQty
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy tổng hợp báo cáo: ", e);
            return ReportSummaryDTO.empty();
        }
    }

    public List<SoldMedicineDTO> getSoldMedicines(LocalDate from, LocalDate to, String keyword, Integer employeeId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            SearchFilter search = SearchFilter.of(keyword, "kw").prefix("m.code").contains("m.name", "mc.name");
            String hql =
                """
                    SELECT new com.pharmacy.report.dto.SoldMedicineDTO(
                        m.code, m.name, COALESCE(mc.name, 'Chưa phân loại'), m.unit,
                        SUM(soi.quantity), SUM(soi.lineTotal)
                    )
                    FROM SalesOrderItem soi
                    JOIN soi.salesOrder so
                    JOIN soi.medicine m
                    LEFT JOIN m.category mc
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ +
                employeeFilter(employeeId) +
                search.hql() +
                """
                    GROUP BY m.id, m.code, m.name, mc.name, m.unit
                    ORDER BY SUM(soi.quantity) DESC, SUM(soi.lineTotal) DESC
                """;
            SelectionQuery<SoldMedicineDTO> query = session.createSelectionQuery(hql, SoldMedicineDTO.class);
            bindRange(query, from, to, employeeId);
            search.bind(query);
            return query.list();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thuốc đã bán: ", e);
            return Collections.emptyList();
        }
    }

    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String hql =
                """
                    SELECT month(so.createdAt), COALESCE(SUM(so.totalAmount), 0)
                    FROM SalesOrder so
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ +
                employeeFilter(employeeId) +
                " GROUP BY month(so.createdAt)";
            SelectionQuery<Object[]> query = session.createSelectionQuery(hql, Object[].class);
            bindRange(query, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), employeeId);
            Map<Integer, Object[]> byMonth = groupByMonth(query.list());

            List<RevenueTimelineDTO> result = new ArrayList<>(12);
            for (int m = 1; m <= 12; m++) {
                Object[] r = byMonth.get(m);
                BigDecimal rev = r != null ? toBigDecimal(r[1]) : BigDecimal.ZERO;
                String tooltip = "Tháng " + m + "/" + year + ": " + FormatUtils.formatCurrency(rev) + " VNĐ";
                result.add(new RevenueTimelineDTO("Thg " + m, rev, tooltip));
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy doanh số 12 tháng: ", e);
            return Collections.emptyList();
        }
    }

    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String hql =
                """
                    SELECT month(so.createdAt), COALESCE(SUM(soi.quantity), 0)
                    FROM SalesOrderItem soi
                    JOIN soi.salesOrder so
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ +
                employeeFilter(employeeId) +
                " GROUP BY month(so.createdAt)";
            SelectionQuery<Object[]> query = session.createSelectionQuery(hql, Object[].class);
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

    public List<DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate, Integer employeeId) {
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();
        return getDailySales(effectiveEnd.minusDays(6), effectiveEnd, employeeId);
    }

    public List<DailySalesDTO> getDailySales(LocalDate from, LocalDate to, Integer employeeId) {
        if (from == null || to == null || from.isAfter(to)) {
            return Collections.emptyList();
        }
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String orderHql =
                """
                    SELECT cast(so.createdAt as LocalDate),
                           COALESCE(SUM(so.totalAmount), 0)
                    FROM SalesOrder so
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ +
                employeeFilter(employeeId) +
                " GROUP BY cast(so.createdAt as LocalDate)";
            SelectionQuery<Object[]> orderQuery = session.createSelectionQuery(orderHql, Object[].class);
            bindRange(orderQuery, from, to, employeeId);

            Map<LocalDate, BigDecimal> revenueByDate = new HashMap<>();
            for (Object[] r : orderQuery.list()) {
                LocalDate d = toLocalDate(r[0]);
                if (d != null) revenueByDate.put(d, toBigDecimal(r[1]));
            }

            String qtyHql =
                """
                    SELECT cast(so.createdAt as LocalDate), COALESCE(SUM(soi.quantity), 0)
                    FROM SalesOrderItem soi
                    JOIN soi.salesOrder so
                    WHERE so.createdAt >= :startTime AND so.createdAt < :endTime
                """ +
                employeeFilter(employeeId) +
                " GROUP BY cast(so.createdAt as LocalDate)";
            SelectionQuery<Object[]> qtyQuery = session.createSelectionQuery(qtyHql, Object[].class);
            bindRange(qtyQuery, from, to, employeeId);

            Map<LocalDate, Long> qtyByDate = new HashMap<>();
            for (Object[] r : qtyQuery.list()) {
                LocalDate d = toLocalDate(r[0]);
                if (d != null) qtyByDate.put(d, toLong(r[1]));
            }

            List<DailySalesDTO> result = new ArrayList<>();
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                BigDecimal amount = revenueByDate.getOrDefault(d, BigDecimal.ZERO);
                long qty = qtyByDate.getOrDefault(d, 0L);
                String dowLabel = dayOfWeekLabel(d.getDayOfWeek());
                result.add(new DailySalesDTO(d, dowLabel, amount, qty));
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi truy vấn doanh số theo ngày trong ReportRepository: ", e);
            return Collections.emptyList();
        }
    }

    private static String dayOfWeekLabel(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "T2";
            case TUESDAY -> "T3";
            case WEDNESDAY -> "T4";
            case THURSDAY -> "T5";
            case FRIDAY -> "T6";
            case SATURDAY -> "T7";
            case SUNDAY -> "CN";
        };
    }

    private static Map<Integer, Object[]> groupByMonth(List<Object[]> rows) {
        Map<Integer, Object[]> byMonth = new HashMap<>();
        for (Object[] r : rows) {
            byMonth.put(toInt(r[0]), r);
        }
        return byMonth;
    }

    private static String employeeFilter(Integer employeeId) {
        return employeeId != null ? " AND so.employee.id = :employeeId " : "";
    }

    private static void bindRange(SelectionQuery<?> query, LocalDate from, LocalDate to, Integer employeeId) {
        query.setParameter("startTime", from.atStartOfDay());
        query.setParameter("endTime", to.plusDays(1).atStartOfDay());
        if (employeeId != null) {
            query.setParameter("employeeId", employeeId);
        }
    }
}
