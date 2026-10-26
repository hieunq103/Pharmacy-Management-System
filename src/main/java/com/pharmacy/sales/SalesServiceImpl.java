package com.pharmacy.sales;

import com.pharmacy.common.FormValidator;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.UserSession;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.DailySalesDTO;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.sales.dto.TodaySalesSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class SalesServiceImpl implements SalesService {

    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "TRANSFER", "CARD");

    private final SalesRepository salesRepository;

    public SalesServiceImpl() {
        this(new SalesRepository());
    }

    public SalesServiceImpl(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    @Override
    public List<String> getCategories() {
        return salesRepository.getAllCategories();
    }

    @Override
    public PageResult<MedicineInventoryDTO> getMedicinesPage(int page, int size, String keyword, String categoryName) {
        return salesRepository.getMedicinesPage(page, size, keyword, categoryName);
    }

    @Override
    public String generateNextOrderCode() {
        return salesRepository.generateNextOrderCode();
    }

    @Override
    public String checkout(SalesOrderRequest request) {
        UserSession.requireAuthenticated();
        validateCheckout(request);
        // Không tin employeeId do UI gửi lên: luôn ghi nhận người đang đăng nhập
        request.setEmployeeId(UserSession.currentEmployeeId());
        return salesRepository.createSalesOrder(request);
    }

    /** Kiểm tra dữ liệu đơn hàng trước khi ghi CSDL (tách riêng để unit test không cần DB). */
    static void validateCheckout(SalesOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng rỗng, không có sản phẩm nào để thanh toán.");
        }
        for (CartItem item : request.getItems()) {
            if (item == null || item.getMedicineId() == null) {
                throw new IllegalArgumentException("Giỏ hàng chứa sản phẩm không hợp lệ.");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Số lượng của thuốc '" + item.getMedicineName() + "' phải lớn hơn 0.");
            }
        }
        if (!PAYMENT_METHODS.contains(request.getPaymentMethod())) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod());
        }
        if (request.hasPrescriptionItem() && !request.isPrescriptionConfirmed()) {
            throw new IllegalArgumentException("Đơn có thuốc kê đơn: cần xác nhận đã kiểm tra đơn thuốc của khách.");
        }
        String phone = request.getCustomerPhone();
        if (phone != null && !phone.isBlank() && !FormValidator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại khách hàng không hợp lệ.");
        }
    }

    @Override
    public PageResult<SalesOrderDTO> getSalesHistory(LocalDate date, Integer employeeId, String keyword, int page, int size) {
        return salesRepository.getSalesOrders(date, UserSession.scopeEmployeeId(employeeId), keyword, page, size);
    }

    @Override
    public PageResult<SalesOrderDTO> getSalesHistory(LocalDate fromDate, LocalDate toDate, Integer employeeId, String keyword, int page, int size) {
        return salesRepository.getSalesOrders(fromDate, toDate, UserSession.scopeEmployeeId(employeeId), keyword, page, size);
    }

    @Override
    public List<SalesOrderDetailDTO> getSalesOrderDetails(long orderId) {
        return salesRepository.getSalesOrderItems(orderId);
    }

    @Override
    public TodaySalesSummary getTodaySalesSummary() {
        return salesRepository.getTodaySalesSummary();
    }

    @Override
    public List<DailySalesDTO> get7DaysSales() {
        return salesRepository.getLast7DaysSales();
    }

    @Override
    public List<DailySalesDTO> get7DaysSalesUpTo(LocalDate endDate, Integer employeeId) {
        return salesRepository.get7DaysSalesUpTo(endDate, UserSession.scopeEmployeeId(employeeId));
    }

    @Override
    public List<SalesOrderDTO> getRecentSalesOrders(int limit) {
        return salesRepository.getRecentSalesOrders(limit);
    }
}
