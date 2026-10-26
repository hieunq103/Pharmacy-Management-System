package com.pharmacy.sales;

import com.pharmacy.common.PageResult;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.sales.dto.TodaySalesSummary;

import java.time.LocalDate;
import java.util.List;

public interface SalesService {
    List<String> getCategories();
    PageResult<MedicineInventoryDTO> getMedicinesPage(int page, int size, String keyword, String categoryName);
    String generateNextOrderCode();

    /**
     * Thanh toán đơn hàng. Hàng đã bán không hỗ trợ hoàn/hủy.
     * Nhân viên lập đơn lấy từ phiên đăng nhập; tổng tiền được tính lại từ giá trong CSDL.
     */
    String checkout(SalesOrderRequest request);
    PageResult<SalesOrderDTO> getSalesHistory(LocalDate date, Integer employeeId, String keyword, int page, int size);

    /** Lịch sử bán hàng trong khoảng ngày [fromDate, toDate], ví dụ cả một tháng. */
    PageResult<SalesOrderDTO> getSalesHistory(LocalDate fromDate, LocalDate toDate, Integer employeeId, String keyword, int page, int size);
    List<SalesOrderDetailDTO> getSalesOrderDetails(long orderId);

    TodaySalesSummary getTodaySalesSummary();
    List<DailySalesDTO> get7DaysSales();
    List<DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate, Integer employeeId);
    List<SalesOrderDTO> getRecentSalesOrders(int limit);
}
