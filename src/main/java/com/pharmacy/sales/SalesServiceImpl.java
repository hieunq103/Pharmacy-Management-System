package com.pharmacy.sales;

import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.SalesOrderRequest;

import java.time.LocalDate;
import java.util.List;

public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository = new SalesRepository();

    @Override
    public List<String> getCategories() {
        return salesRepository.getAllCategories();
    }

    @Override
    public List<MedicineInventoryDTO> getMedicines(String keyword, String categoryName) {
        return salesRepository.getMedicinesForSales(keyword, categoryName);
    }

    @Override
    public com.pharmacy.common.PageResult<MedicineInventoryDTO> getMedicinesPage(int page, int size, String keyword, String categoryName) {
        return salesRepository.getMedicinesPage(page, size, keyword, categoryName);
    }

    @Override
    public String generateNextOrderCode() {
        return salesRepository.generateNextOrderCode();
    }

    @Override
    public String checkout(SalesOrderRequest request) {
        return salesRepository.createSalesOrder(request);
    }

    @Override
    public com.pharmacy.common.PageResult<com.pharmacy.sales.dto.SalesOrderDTO> getSalesHistory(java.time.LocalDate date, Integer employeeId, String keyword, int page, int size) {
        return salesRepository.getSalesOrders(date, employeeId, keyword, page, size);
    }

    @Override
    public List<com.pharmacy.sales.dto.SalesOrderDetailDTO> getSalesOrderDetails(long orderId) {
        return salesRepository.getSalesOrderItems(orderId);
    }

    @Override
    public com.pharmacy.sales.dto.TodaySalesSummary getTodaySalesSummary() {
        return salesRepository.getTodaySalesSummary();
    }

    @Override
    public List<com.pharmacy.sales.dto.DailySalesDTO> get7DaysSales() {
        return salesRepository.getLast7DaysSales();
    }

    @Override
    public List<com.pharmacy.sales.dto.DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate) {
        return get7DaysSalesUpTo(endDate, null);
    }

    @Override
    public List<com.pharmacy.sales.dto.DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate, Integer employeeId) {
        return salesRepository.get7DaysSalesUpTo(endDate, employeeId);
    }

    @Override
    public List<com.pharmacy.sales.dto.SalesOrderDTO> getRecentSalesOrders(int limit) {
        return salesRepository.getRecentSalesOrders(limit);
    }
}
