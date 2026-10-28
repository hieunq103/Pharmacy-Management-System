package com.pharmacy.dashboard;

import com.pharmacy.dashboard.dto.DashboardStatsDTO;
import com.pharmacy.inventory.dto.InventoryAlertDTO;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;

import java.util.List;

public interface DashboardService {
    DashboardStatsDTO getStats();

    List<DailySalesDTO> getLast7DaysSales();

    List<InventoryAlertDTO> getInventoryAlerts(int limit);

    List<SalesOrderDTO> getRecentTransactions(int limit);
}
