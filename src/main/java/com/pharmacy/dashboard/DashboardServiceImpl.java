package com.pharmacy.dashboard;

import com.pharmacy.dashboard.dto.DashboardStatsDTO;
import com.pharmacy.inventory.InventoryRepository;
import com.pharmacy.inventory.InventoryService;
import com.pharmacy.inventory.InventoryServiceImpl;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.sales.SalesService;
import com.pharmacy.sales.SalesServiceImpl;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.sales.dto.TodaySalesSummary;

import java.util.List;

public class DashboardServiceImpl implements DashboardService {
    private final SalesService salesService;
    private final InventoryService inventoryService;

    public DashboardServiceImpl() {
        this(new SalesServiceImpl(), new InventoryServiceImpl());
    }

    public DashboardServiceImpl(SalesService salesService, InventoryService inventoryService) {
        this.salesService = salesService;
        this.inventoryService = inventoryService;
    }

    @Override
    public DashboardStatsDTO getStats() {
        TodaySalesSummary salesSummary = salesService.getTodaySalesSummary();
        InventoryRepository.InventorySummary invSummary = inventoryService.getInventorySummary();
        return new DashboardStatsDTO(salesSummary, invSummary.lowStockMedicines(), invSummary.nearExpiryMedicines());
    }

    @Override
    public List<DailySalesDTO> getLast7DaysSales() {
        return salesService.get7DaysSales();
    }

    @Override
    public List<InventoryAlertDTO> getInventoryAlerts(int limit) {
        return inventoryService.getInventoryAlerts(limit);
    }

    @Override
    public List<SalesOrderDTO> getRecentTransactions(int limit) {
        return salesService.getRecentSalesOrders(limit);
    }
}
