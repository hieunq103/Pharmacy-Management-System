package com.pharmacy.report;

import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.report.dto.ReportSummaryDTO;
import com.pharmacy.report.dto.RevenueTimelineDTO;
import com.pharmacy.report.dto.MedicineTimelineDTO;
import com.pharmacy.report.dto.SoldMedicineDTO;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportRepository {
    private static final Logger log = LoggerFactory.getLogger(ReportRepository.class);

    /**
     * Lấy tổng hợp doanh số, số đơn, số tiền mặt/chuyển khoản và tổng số lượng
     * thuốc đã bán trong kỳ.
     * Tối ưu hóa: Loại bỏ subquery lồng nhau, tách thành 2 câu truy vấn đánh trúng
     * chỉ mục idx_sales_orders_created_at
     * chạy trong cùng 1 Hibernate Session.
     */
    public ReportSummaryDTO getReportSummary(LocalDateTime from, LocalDateTime to) {
        return getReportSummary(from, to, null);
    }

    public ReportSummaryDTO getReportSummary(LocalDateTime from, LocalDateTime to, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Timestamp startTs = Timestamp.valueOf(from);
            Timestamp endTs = Timestamp.valueOf(to);

            // Truy vấn 1: Doanh số, số đơn, tiền mặt, chuyển khoản từ sales_orders
            String empFilter1 = (employeeId != null) ? " AND employee_id = :employeeId " : "";
            String orderSql = """
                        SELECT
                            COUNT(id) as total_orders,
                            COALESCE(SUM(total_amount), 0) as total_revenue,
                            COALESCE(SUM(CASE WHEN payment_method = 'CASH' THEN total_amount ELSE 0 END), 0) as cash_revenue,
                            COALESCE(SUM(CASE WHEN payment_method = 'TRANSFER' THEN total_amount ELSE 0 END), 0) as transfer_revenue
                        FROM sales_orders
                        WHERE created_at >= :startTime AND created_at <= :endTime
                    """
                    + empFilter1;

            NativeQuery<Object[]> orderQuery = session.createNativeQuery(orderSql, Object[].class);
            orderQuery.setParameter("startTime", startTs);
            orderQuery.setParameter("endTime", endTs);
            if (employeeId != null) {
                orderQuery.setParameter("employeeId", employeeId);
            }

            Object[] row = orderQuery.uniqueResult();
            long totalOrders = 0L;
            BigDecimal totalRevenue = BigDecimal.ZERO;
            BigDecimal cashRevenue = BigDecimal.ZERO;
            BigDecimal transferRevenue = BigDecimal.ZERO;

            if (row != null) {
                totalOrders = row[0] != null ? ((Number) row[0]).longValue() : 0L;
                totalRevenue = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                cashRevenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                transferRevenue = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
            }

            // Truy vấn 2: Tổng số lượng thuốc đã bán (chỉ truy vấn khi có ít nhất 1 đơn
            // hàng)
            long totalMedicinesSold = 0L;
            if (totalOrders > 0) {
                String empFilter2 = (employeeId != null) ? " AND so.employee_id = :employeeId " : "";
                String medSql = """
                            SELECT COALESCE(SUM(soi.quantity), 0)
                            FROM sales_order_items soi
                            JOIN sales_orders so ON soi.sales_order_id = so.id
                            WHERE so.created_at >= :startTime AND so.created_at <= :endTime
                        """ + empFilter2;
                NativeQuery<Object> medQuery = session.createNativeQuery(medSql, Object.class);
                medQuery.setParameter("startTime", startTs);
                medQuery.setParameter("endTime", endTs);
                if (employeeId != null) {
                    medQuery.setParameter("employeeId", employeeId);
                }

                Object medRow = medQuery.uniqueResult();
                totalMedicinesSold = medRow != null ? ((Number) medRow).longValue() : 0L;
            }

            return new ReportSummaryDTO(totalOrders, totalRevenue, cashRevenue, transferRevenue, totalMedicinesSold);
        } catch (Exception e) {
            log.error("Lỗi khi lấy tổng hợp báo cáo: ", e);
            return ReportSummaryDTO.empty();
        }
    }

    /**
     * Báo cáo chi tiết số lượng từng loại thuốc đã bán trong kỳ.
     */
    public List<SoldMedicineDTO> getSoldMedicines(LocalDateTime from, LocalDateTime to, String keyword) {
        return getSoldMedicines(from, to, keyword, null);
    }

    public List<SoldMedicineDTO> getSoldMedicines(LocalDateTime from, LocalDateTime to, String keyword,
            Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder sql = new StringBuilder("""
                        SELECT
                            m.id,
                            m.code,
                            m.name,
                            COALESCE(mc.name, 'Chưa phân loại') as category_name,
                            m.unit,
                            SUM(soi.quantity) as quantity_sold,
                            SUM(soi.line_total) as total_revenue
                        FROM sales_order_items soi
                        JOIN sales_orders so ON soi.sales_order_id = so.id
                        JOIN medicines m ON soi.medicine_id = m.id
                        LEFT JOIN medicine_categories mc ON m.category_id = mc.id
                        WHERE so.created_at >= :startTime AND so.created_at <= :endTime
                    """);

            if (employeeId != null) {
                sql.append(" AND so.employee_id = :employeeId ");
            }

            boolean hasKeyword = keyword != null && !keyword.isBlank();
            if (hasKeyword) {
                sql.append(" AND (m.code LIKE :keyword OR m.name LIKE :keyword OR mc.name LIKE :keyword) ");
            }

            sql.append(" GROUP BY m.id, m.code, m.name, mc.name, m.unit ");
            sql.append(" ORDER BY quantity_sold DESC, total_revenue DESC ");

            NativeQuery<Object[]> query = session.createNativeQuery(sql.toString(), Object[].class);
            query.setParameter("startTime", Timestamp.valueOf(from));
            query.setParameter("endTime", Timestamp.valueOf(to));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }
            if (hasKeyword) {
                query.setParameter("keyword", "%" + keyword.trim() + "%");
            }

            List<Object[]> rows = query.list();
            List<SoldMedicineDTO> result = new ArrayList<>();
            for (Object[] r : rows) {
                Integer medId = r[0] != null ? ((Number) r[0]).intValue() : null;
                String medCode = r[1] != null ? r[1].toString() : "";
                String medName = r[2] != null ? r[2].toString() : "";
                String catName = r[3] != null ? r[3].toString() : "";
                String unit = r[4] != null ? r[4].toString() : "";
                long qty = r[5] != null ? ((Number) r[5]).longValue() : 0L;
                BigDecimal revenue = r[6] != null ? new BigDecimal(r[6].toString()) : BigDecimal.ZERO;

                result.add(new SoldMedicineDTO(medId, medCode, medName, catName, unit, qty, revenue));
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách thuốc đã bán: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy dữ liệu doanh số cho 7 ngày gần nhất tính từ ngày hôm nay (đảm bảo đủ 7
     * cột liên tiếp).
     */
    public List<RevenueTimelineDTO> getLast7DaysRevenueTimeline() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate today = LocalDate.now();
            LocalDate startDate = today.minusDays(6);

            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = today.atTime(LocalTime.MAX);

            String sql = """
                        SELECT
                            DATE(so.created_at) as date_val,
                            COALESCE(SUM(so.total_amount), 0) as revenue,
                            COUNT(so.id) as order_count
                        FROM sales_orders so
                        WHERE so.created_at >= :startTime AND so.created_at <= :endTime
                        GROUP BY DATE(so.created_at)
                        ORDER BY date_val ASC
                    """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            query.setParameter("startTime", Timestamp.valueOf(from));
            query.setParameter("endTime", Timestamp.valueOf(to));

            List<Object[]> rows = query.list();
            java.util.Map<LocalDate, RevenueTimelineDTO> map = new java.util.HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

            for (Object[] r : rows) {
                LocalDate lDate = null;
                if (r[0] instanceof java.sql.Date d) {
                    lDate = d.toLocalDate();
                } else if (r[0] instanceof LocalDate ld) {
                    lDate = ld;
                }
                BigDecimal rev = r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO;
                long orders = r[2] != null ? ((Number) r[2]).longValue() : 0L;
                if (lDate != null) {
                    map.put(lDate, new RevenueTimelineDTO(lDate.format(formatter), rev, orders));
                }
            }

            List<RevenueTimelineDTO> result = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate d = startDate.plusDays(i);
                if (map.containsKey(d)) {
                    result.add(map.get(d));
                } else {
                    result.add(new RevenueTimelineDTO(d.format(formatter), BigDecimal.ZERO, 0L));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy doanh số 7 ngày gần nhất: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy dữ liệu doanh số đủ 12 tháng trong năm hiện tại (Tháng 1 -> Tháng 12).
     */
    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year) {
        return get12MonthsRevenueTimeline(year, null);
    }

    public List<RevenueTimelineDTO> get12MonthsRevenueTimeline(int year, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate startOfYear = LocalDate.of(year, 1, 1);
            LocalDate endOfYear = LocalDate.of(year, 12, 31);

            LocalDateTime from = startOfYear.atStartOfDay();
            LocalDateTime to = endOfYear.atTime(LocalTime.MAX);

            String empFilter = (employeeId != null) ? " AND so.employee_id = :employeeId " : "";
            String sql = """
                        SELECT
                            MONTH(so.created_at) as month_val,
                            COALESCE(SUM(so.total_amount), 0) as revenue,
                            COUNT(so.id) as order_count
                        FROM sales_orders so
                        WHERE so.created_at >= :startTime AND so.created_at <= :endTime
                    """ + empFilter + """
                        GROUP BY MONTH(so.created_at)
                        ORDER BY month_val ASC
                    """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            query.setParameter("startTime", Timestamp.valueOf(from));
            query.setParameter("endTime", Timestamp.valueOf(to));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }

            List<Object[]> rows = query.list();
            java.util.Map<Integer, RevenueTimelineDTO> map = new java.util.HashMap<>();

            for (Object[] r : rows) {
                int month = r[0] != null ? ((Number) r[0]).intValue() : 0;
                BigDecimal rev = r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO;
                long orders = r[2] != null ? ((Number) r[2]).longValue() : 0L;
                if (month >= 1 && month <= 12) {
                    String tooltip = "Tháng " + month + "/" + year + ": "
                            + com.pharmacy.common.FormatUtils.formatCurrency(rev) + " VNĐ";
                    map.put(month, new RevenueTimelineDTO("Thg " + month, rev, orders, tooltip));
                }
            }

            List<RevenueTimelineDTO> result = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                if (map.containsKey(m)) {
                    result.add(map.get(m));
                } else {
                    String tooltip = "Tháng " + m + "/" + year + ": 0 VNĐ";
                    result.add(new RevenueTimelineDTO("Thg " + m, BigDecimal.ZERO, 0L, tooltip));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy doanh số 12 tháng: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy dữ liệu doanh số theo ngày cho khoảng tùy chọn.
     */
    public List<RevenueTimelineDTO> getDailyRevenueTimeline(LocalDate startDate, LocalDate endDate) {
        return getDailyRevenueTimeline(startDate, endDate, null);
    }

    public List<RevenueTimelineDTO> getDailyRevenueTimeline(LocalDate startDate, LocalDate endDate,
            Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = endDate.atTime(LocalTime.MAX);

            String empFilter = (employeeId != null) ? " AND so.employee_id = :employeeId " : "";
            String sql = """
                        SELECT
                            DATE(so.created_at) as date_val,
                            COALESCE(SUM(so.total_amount), 0) as revenue,
                            COUNT(so.id) as order_count
                        FROM sales_orders so
                        WHERE so.created_at >= :startTime AND so.created_at <= :endTime
                    """ + empFilter + """
                        GROUP BY DATE(so.created_at)
                        ORDER BY date_val ASC
                    """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            query.setParameter("startTime", Timestamp.valueOf(from));
            query.setParameter("endTime", Timestamp.valueOf(to));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }

            List<Object[]> rows = query.list();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
            DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            List<RevenueTimelineDTO> result = new ArrayList<>();
            for (Object[] r : rows) {
                LocalDate lDate = null;
                if (r[0] instanceof java.sql.Date d) {
                    lDate = d.toLocalDate();
                } else if (r[0] instanceof LocalDate ld) {
                    lDate = ld;
                }
                BigDecimal rev = r[1] != null ? new BigDecimal(r[1].toString()) : BigDecimal.ZERO;
                long orders = r[2] != null ? ((Number) r[2]).longValue() : 0L;
                if (lDate != null) {
                    String tooltip = "Ngày " + lDate.format(fullFormatter) + ": "
                            + com.pharmacy.common.FormatUtils.formatCurrency(rev) + " VNĐ";
                    result.add(new RevenueTimelineDTO(lDate.format(formatter), rev, orders, tooltip));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy doanh số theo ngày: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy dữ liệu số lượng thuốc đã bán đủ 12 tháng trong năm hiện tại (Tháng 1 ->
     * Tháng 12).
     */
    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year) {
        return get12MonthsMedicineTimeline(year, null);
    }

    public List<MedicineTimelineDTO> get12MonthsMedicineTimeline(int year, Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDate startOfYear = LocalDate.of(year, 1, 1);
            LocalDate endOfYear = LocalDate.of(year, 12, 31);

            LocalDateTime from = startOfYear.atStartOfDay();
            LocalDateTime to = endOfYear.atTime(LocalTime.MAX);

            String empFilter = (employeeId != null) ? " AND so.employee_id = :employeeId " : "";
            String sql = """
                        SELECT
                            MONTH(so.created_at) as month_val,
                            COALESCE(SUM(soi.quantity), 0) as total_qty
                        FROM sales_orders so
                        JOIN sales_order_items soi ON so.id = soi.sales_order_id
                        WHERE so.created_at >= :startTime AND so.created_at <= :endTime
                    """ + empFilter + """
                        GROUP BY MONTH(so.created_at)
                        ORDER BY month_val ASC
                    """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            query.setParameter("startTime", Timestamp.valueOf(from));
            query.setParameter("endTime", Timestamp.valueOf(to));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }

            List<Object[]> rows = query.list();
            java.util.Map<Integer, MedicineTimelineDTO> map = new java.util.HashMap<>();

            for (Object[] r : rows) {
                int month = r[0] != null ? ((Number) r[0]).intValue() : 0;
                long qty = r[1] != null ? ((Number) r[1]).longValue() : 0L;
                if (month >= 1 && month <= 12) {
                    String tooltip = "Tháng " + month + "/" + year + ": "
                            + String.format("%,d", qty) + " sản phẩm";
                    map.put(month, new MedicineTimelineDTO("Thg " + month, qty, tooltip));
                }
            }

            List<MedicineTimelineDTO> result = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                if (map.containsKey(m)) {
                    result.add(map.get(m));
                } else {
                    String tooltip = "Tháng " + m + "/" + year + ": 0 sản phẩm";
                    result.add(new MedicineTimelineDTO("Thg " + m, 0L, tooltip));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy sản lượng thuốc 12 tháng: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy dữ liệu số lượng thuốc đã bán theo ngày cho khoảng tùy chọn.
     */
    public List<MedicineTimelineDTO> getDailyMedicineTimeline(LocalDate startDate, LocalDate endDate) {
        return getDailyMedicineTimeline(startDate, endDate, null);
    }

    public List<MedicineTimelineDTO> getDailyMedicineTimeline(LocalDate startDate, LocalDate endDate,
            Integer employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            LocalDateTime from = startDate.atStartOfDay();
            LocalDateTime to = endDate.atTime(LocalTime.MAX);

            String empFilter = (employeeId != null) ? " AND so.employee_id = :employeeId " : "";
            String sql = """
                        SELECT
                            DATE(so.created_at) as date_val,
                            COALESCE(SUM(soi.quantity), 0) as total_qty
                        FROM sales_orders so
                        JOIN sales_order_items soi ON so.id = soi.sales_order_id
                        WHERE so.created_at >= :startTime AND so.created_at <= :endTime
                    """ + empFilter + """
                        GROUP BY DATE(so.created_at)
                        ORDER BY date_val ASC
                    """;

            NativeQuery<Object[]> query = session.createNativeQuery(sql, Object[].class);
            query.setParameter("startTime", Timestamp.valueOf(from));
            query.setParameter("endTime", Timestamp.valueOf(to));
            if (employeeId != null) {
                query.setParameter("employeeId", employeeId);
            }

            List<Object[]> rows = query.list();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
            DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            List<MedicineTimelineDTO> result = new ArrayList<>();
            for (Object[] r : rows) {
                LocalDate lDate = null;
                if (r[0] instanceof java.sql.Date d) {
                    lDate = d.toLocalDate();
                } else if (r[0] instanceof LocalDate ld) {
                    lDate = ld;
                }
                long qty = r[1] != null ? ((Number) r[1]).longValue() : 0L;
                if (lDate != null) {
                    String tooltip = "Ngày " + lDate.format(fullFormatter) + ": "
                            + String.format("%,d", qty) + " sản phẩm";
                    result.add(new MedicineTimelineDTO(lDate.format(formatter), qty, tooltip));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Lỗi khi lấy sản lượng thuốc theo ngày: ", e);
            return Collections.emptyList();
        }
    }
}
