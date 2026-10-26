// Công cụ đo hiệu năng truy vấn (không nằm trong build Maven). Cách chạy: xem docs/benchmark/README.md
import com.pharmacy.account.AccountRepository;
import com.pharmacy.employee.EmployeeRepository;
import com.pharmacy.inventory.InventoryRepository;
import com.pharmacy.inventory.dto.BatchExportItemDTO;
import com.pharmacy.inventory.dto.BatchImportItemDTO;
import com.pharmacy.report.ReportRepository;
import com.pharmacy.sales.SalesRepository;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.SalesOrderRequest;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;

/** Đo số câu SQL (Com_select/insert/update) + số dòng InnoDB đọc + thời gian cho từng kịch bản. */
public class QueryBench {
    static Connection meta;
    static final String[] COUNTERS = {"Com_select", "Com_insert", "Com_update", "Com_delete"};

    static Map<String, Long> status() throws SQLException {
        Map<String, Long> m = new HashMap<>();
        try (Statement st = meta.createStatement(); ResultSet rs = st.executeQuery(
                "SHOW GLOBAL STATUS WHERE Variable_name IN ('Com_select','Com_insert','Com_update','Com_delete','Innodb_rows_read')")) {
            while (rs.next()) m.put(rs.getString(1), rs.getLong(2));
        }
        return m;
    }

    static void run(String name, int repeat, Supplier<Object> body) throws Exception {
        body.get(); // warm-up (JIT, Hibernate query plan cache)
        long stmts = 0, rows = 0, nanos = 0;
        for (int i = 0; i < repeat; i++) {
            Map<String, Long> a = status();
            long t = System.nanoTime();
            body.get();
            nanos += System.nanoTime() - t;
            Map<String, Long> b = status();
            for (String c : COUNTERS) stmts += b.get(c) - a.get(c);
            rows += b.get("Innodb_rows_read") - a.get("Innodb_rows_read");
        }
        System.out.printf("| %-48s | %5d | %10d | %8.1f |%n", name, stmts / repeat, rows / repeat, nanos / 1e6 / repeat);
    }

    static BigDecimal price(int medId) {
        try (Statement st = meta.createStatement(); ResultSet rs = st.executeQuery("SELECT sell_price FROM medicines WHERE id=" + medId)) {
            rs.next(); return rs.getBigDecimal(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public static void main(String[] args) throws Exception {
        meta = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        boolean writes = args.length > 0 && args[0].equals("writes");
        SalesRepository sales = new SalesRepository();
        InventoryRepository inv = new InventoryRepository();
        ReportRepository report = new ReportRepository();
        EmployeeRepository emp = new EmployeeRepository();
        AccountRepository acc = new AccountRepository();
        LocalDate today = LocalDate.of(2026, 10, 26);
        LocalDate monthStart = today.withDayOfMonth(1);

        System.out.println("| Kịch bản | SQL | Dòng đọc | ms |");
        System.out.println("|---|---:|---:|---:|");
        if (!writes) {
            run("Bán hàng: getMedicinesPage trang 1", 3, () -> sales.getMedicinesPage(1, 6, "", null));
            run("Bán hàng: getMedicinesPage trang 50", 3, () -> sales.getMedicinesPage(50, 6, "", null));
            run("Bán hàng: getMedicinesPage tìm 'bench 12'", 3, () -> sales.getMedicinesPage(1, 6, "bench 12", null));
            run("Kho: getInventoryPageResult trang 1", 3, () -> inv.getInventoryPageResult(1, 15, "", null, null));
            run("Kho: lọc 'Sắp hết hàng' + 'Có lô cận hạn'", 3, () -> inv.getInventoryPageResult(1, 15, "", "Sắp hết hàng", "Có lô cận hạn"));
            run("Kho: getMedicinesForPicker", 3, inv::getMedicinesForPicker);
            run("Kho: getInventorySummary", 3, inv::getInventorySummary);
            run("Kho: getInventoryAlerts(10)", 3, () -> inv.getInventoryAlerts(10));
            run("Kho: getImportTransactions (30 ngày)", 3, () -> inv.getImportTransactions(null, today.minusDays(30), today));
            run("Kho: getExportTransactions (hôm qua)", 3, () -> inv.getExportTransactions(null, today.minusDays(1), today.minusDays(1)));
            run("Dashboard: getRecentSalesOrders(10)", 3, () -> sales.getRecentSalesOrders(10));
            run("Dashboard: getTodaySalesSummary", 3, sales::getTodaySalesSummary);
            run("Dashboard: get7DaysSalesUpTo", 3, () -> sales.get7DaysSalesUpTo(today, null));
            run("Lịch sử bán: getSalesOrders(hôm nay, trang 1)", 3, () -> sales.getSalesOrders(today, null, "", 1, 20));
            run("Lịch sử bán: getSalesOrders(NV 2, trang 1)", 3, () -> sales.getSalesOrders(today, 2, "", 1, 20));
            run("Báo cáo: getReportSummary(tháng, NV 2)", 3, () -> report.getReportSummary(monthStart, today, 2));
            run("Báo cáo: getSoldMedicines(tháng)", 3, () -> report.getSoldMedicines(monthStart, today, null, null));
            run("Báo cáo: get12MonthsRevenueTimeline(NV 2)", 3, () -> report.get12MonthsRevenueTimeline(2026, 2));
            run("Nhân viên: getEmployeesPage", 3, () -> emp.getEmployeesPage(1, 10, null, null, null, null, null));
            run("Đăng nhập: findByIdentifier", 3, () -> acc.findByIdentifier("0985549854"));
            run("Nhân viên: findByIdWithDetails", 3, () -> emp.findByIdWithDetails(1));
        } else {
            List<Integer> meds = new ArrayList<>();
            try (Statement st = meta.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT medicine_id FROM inventory_lots WHERE quantity_remaining > 0 AND expiry_date > '2026-12-31' GROUP BY medicine_id HAVING SUM(quantity_remaining) >= 300 ORDER BY medicine_id LIMIT 400")) {
                while (rs.next()) meds.add(rs.getInt(1));
            }
            Map<Integer, BigDecimal> prices = new HashMap<>();
            for (Integer id : meds) prices.put(id, price(id)); // đọc trước, không tính vào số đo
            int[] k = {0};
            run("Thanh toán: createSalesOrder 5 mặt hàng", 5, () -> {
                List<CartItem> items = new ArrayList<>();
                for (int i = 0; i < 5; i++) items.add(new CartItem(meds.get(k[0]++), "x", "x", "Hộp", false, prices.get(meds.get(k[0]-1)), 150, 999));
                SalesOrderRequest r = new SalesOrderRequest(1, null, BigDecimal.ZERO, "CASH", true, items);
                r.setOrderCode("BENCH-W-" + UUID.randomUUID().toString().substring(0, 12));
                return sales.createSalesOrder(r);
            });
            run("Nhập kho: importBatchStock 20 dòng", 3, () -> {
                List<BatchImportItemDTO> items = new ArrayList<>();
                for (int i = 0; i < 20; i++) items.add(BatchImportItemDTO.fromExisting(meds.get(i), "x", "x", null, null,
                        "W-" + UUID.randomUUID().toString().substring(0, 8), today.plusYears(2), "Hộp", 50, new BigDecimal("5000")));
                return inv.importBatchStock("DHG Pharma", 1, "bench", items);
            });
            List<long[]> lots = new ArrayList<>();
            try (Statement st = meta.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT id, medicine_id FROM inventory_lots WHERE quantity_remaining >= 40 ORDER BY id LIMIT 200")) {
                while (rs.next()) lots.add(new long[]{rs.getLong(1), rs.getLong(2)});
            }
            int[] j = {0};
            run("Xuất hủy: exportBatchStock 20 dòng", 3, () -> {
                List<BatchExportItemDTO> items = new ArrayList<>();
                for (int i = 0; i < 20; i++) { long[] l = lots.get(j[0]++);
                    items.add(BatchExportItemDTO.of((int) l[1], "x", "x", null, null, l[0], "x", today, "Hộp", 40, 1, BigDecimal.ONE, "Hỏng")); }
                return inv.exportBatchStock("Hỏng", 1, "bench", items);
            });
        }
        com.pharmacy.infra.db.HibernateUtil.shutdown();
    }
}
