package com.pharmacy.dashboard;

import com.pharmacy.dashboard.dto.DashboardStatsDTO;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.inventory.dto.InventorySummaryDTO;
import com.pharmacy.report.ReportService;
import com.pharmacy.report.dto.DailySalesDTO;
import com.pharmacy.report.dto.TodaySalesSummary;
import com.pharmacy.sales.SalesService;
import com.pharmacy.sales.dto.SalesOrderDTO;
import java.util.List;

public class DashboardService {

    private final SalesService salesService;
    private final InventoryService inventoryService;
    private final ReportService reportService;

    public DashboardService() {
        this(new SalesService(), new InventoryService(), new ReportService());
    }

    public DashboardService(SalesService salesService, InventoryService inventoryService, ReportService reportService) {
        this.salesService = salesService;
        this.inventoryService = inventoryService;
        this.reportService = reportService;
    }

    public DashboardStatsDTO getStats() {
        TodaySalesSummary salesSummary = reportService.getTodaySalesSummary();
        InventorySummaryDTO invSummary = inventoryService.getInventorySummary();
        return new DashboardStatsDTO(salesSummary, invSummary.lowStockMedicines(), invSummary.nearExpiryMedicines());
    }

    public List<DailySalesDTO> getLast7DaysSales() {
        return reportService.getLast7DaysSales();
    }

    public List<InventoryAlertDTO> getInventoryAlerts(int limit) {
        return inventoryService.getInventoryAlerts(limit);
    }

    public List<SalesOrderDTO> getRecentTransactions(int limit) {
        return salesService.getRecentSalesOrders(limit);
    }
}
