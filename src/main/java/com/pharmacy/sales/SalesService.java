package com.pharmacy.sales;

import com.pharmacy.common.FormValidator;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.UserSession;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.SalesOrderDTO;
import com.pharmacy.sales.dto.SalesOrderDetailDTO;
import com.pharmacy.sales.dto.SalesOrderRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class SalesService {

    private static final Set<String> PAYMENT_METHODS = Set.of("CASH", "TRANSFER", "CARD");

    private final SalesRepository salesRepository;

    public SalesService() {
        this(new SalesRepository());
    }

    public SalesService(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public List<String> getCategories() {
        return salesRepository.getAllCategories();
    }

    public PageResult<MedicineInventoryDTO> getMedicinesPage(int page, int size, String keyword, String categoryName) {
        return getMedicinesPage(page, size, keyword, categoryName, null);
    }

    public PageResult<MedicineInventoryDTO> getMedicinesPage(
        int page,
        int size,
        String keyword,
        String categoryName,
        Long cachedTotal
    ) {
        return salesRepository.getMedicinesPage(page, size, keyword, categoryName, cachedTotal);
    }

    public String generateNextOrderCode() {
        return salesRepository.generateNextOrderCode();
    }

    public String checkout(SalesOrderRequest request) {
        UserSession.requireAuthenticated();
        validateCheckout(request);

        request.setEmployeeId(UserSession.currentEmployeeId());
        return salesRepository.createSalesOrder(request);
    }

    public static void validateCheckout(SalesOrderRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng rỗng, không có sản phẩm nào để thanh toán.");
        }
        for (CartItem item : request.getItems()) {
            if (item == null || item.getMedicineId() == null) {
                throw new IllegalArgumentException("Giỏ hàng chứa sản phẩm không hợp lệ.");
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                    "Số lượng của thuốc '" + item.getMedicineName() + "' phải lớn hơn 0."
                );
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

    public PageResult<SalesOrderDTO> getSalesHistory(
        LocalDate fromDate,
        LocalDate toDate,
        Integer employeeId,
        String keyword,
        int page,
        int size
    ) {
        return salesRepository.getSalesOrders(
            fromDate,
            toDate,
            UserSession.scopeEmployeeId(employeeId),
            keyword,
            page,
            size
        );
    }

    public List<SalesOrderDetailDTO> getSalesOrderDetails(long orderId) {
        return salesRepository.getSalesOrderItems(orderId);
    }

    public List<SalesOrderDTO> getRecentSalesOrders(int limit) {
        return salesRepository.getRecentSalesOrders(limit, UserSession.scopeEmployeeId(null));
    }
}
