package com.pharmacy.dashboard;

import com.pharmacy.dashboard.dto.DashboardStatsDTO;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.support.DbTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardServiceTest {

    @BeforeAll
    static void requireDb() {
        DbTestSupport.assumeDatabase();
    }

    @Test
    void testDashboardDataRetrieval() {
        DashboardService service = new DashboardServiceImpl();

        // 1. Kiểm tra 4 chỉ số thống kê
        DashboardStatsDTO stats = service.getStats();
        assertNotNull(stats, "Thống kê tổng quan không được null");
        assertTrue(stats.todaySales().revenue().signum() >= 0);
        assertTrue(stats.todaySales().orderCount() >= 0);
        assertTrue(stats.lowStockCount() >= 0);
        assertTrue(stats.nearExpiryCount() >= 0);

        // 2. Kiểm tra doanh số 7 ngày gần nhất
        List<DailySalesDTO> weeklySales = service.getLast7DaysSales();
        assertNotNull(weeklySales, "Doanh số 7 ngày không được null");
        assertEquals(7, weeklySales.size(), "Phải có đúng 7 ngày");
        System.out.println("=== DOANH SỐ 7 NGÀY GẦN NHẤT ===");
        for (DailySalesDTO d : weeklySales) {
            System.out.println(d.getDayOfWeekLabel() + " (" + d.getDate() + "): " + d.getAmount() + "đ (" + d.getOrderCount() + " đơn)");
            assertNotNull(d.getDayOfWeekLabel());
            assertNotNull(d.getAmount());
        }

        // 3. Kiểm tra cảnh báo kho
        List<InventoryAlertDTO> alerts = service.getInventoryAlerts(4);
        assertNotNull(alerts, "Danh sách cảnh báo kho không được null");
        System.out.println("=== CẢNH BÁO KHO (" + alerts.size() + ") ===");
        for (InventoryAlertDTO a : alerts) {
            System.out.println(a.getTitle() + " | " + a.getSubtitle() + " | " + a.getQuantityBadgeText() + " | Type: " + a.getAlertType());
            assertNotNull(a.getTitle());
            assertNotNull(a.getQuantityBadgeText());
        }

        // 4. Kiểm tra giao dịch gần đây (tái sử dụng SalesOrderDTO)
        List<SalesOrderDTO> recentTx = service.getRecentTransactions(5);
        assertNotNull(recentTx, "Giao dịch gần đây không được null");
        System.out.println("=== GIAO DỊCH GẦN ĐÂY (" + recentTx.size() + ") ===");
        for (SalesOrderDTO tx : recentTx) {
            System.out.println(tx.getCode() + " | " + tx.getCustomerPhone() + " | " + tx.getEmployeeName() + " (" + tx.getEmployeeInitials() + ") | " + tx.getTimeOnly() + " | " + tx.getFormattedPaymentMethod() + " | " + tx.getFormattedTotal());
            assertNotNull(tx.getCode());
            assertNotNull(tx.getFormattedTotal());
            assertNotNull(tx.getEmployeeInitials());
        }
    }
}
