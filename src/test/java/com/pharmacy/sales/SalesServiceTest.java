package com.pharmacy.sales;

import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.SalesOrderRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SalesServiceTest {

    @Test
    void testSalesDataRetrieval() {
        SalesService salesService = new SalesServiceImpl();
        List<MedicineInventoryDTO> list = salesService.getMedicines("", "Tất cả");
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Danh sách thuốc bán hàng không được rỗng");

        MedicineInventoryDTO first = list.get(0);
        System.out.println("Thuốc bán hàng: " + first.getMedicineName() + ", Giá bán: " + first.getSellPrice() + ", Tồn: " + first.getTotalQuantity() + ", Kê toa: " + first.getRequiresPrescription());
        assertNotNull(first.getSellPrice());

        var categories = salesService.getCategories();
        assertNotNull(categories);
        assertFalse(categories.isEmpty(), "Danh mục thuốc không được rỗng");
        System.out.println("Tổng số danh mục: " + categories.size());
    }

    @Test
    void testSalesPaginationAndFilter() {
        SalesService salesService = new SalesServiceImpl();
        // Test pagination 6 items per page
        var page1 = salesService.getMedicinesPage(1, 6, "", "Tất cả nhóm thuốc");
        assertNotNull(page1);
        assertEquals(6, page1.getItems().size(), "Trang 1 phải có đúng 6 thuốc");
        assertTrue(page1.getTotalElements() > 6, "Tổng số thuốc phải > 6");
        assertTrue(page1.getTotalPages() >= 2, "Tổng số trang phải >= 2");

        // Test filter by category
        var categories = salesService.getCategories();
        if (!categories.isEmpty()) {
            String firstCat = categories.get(0);
            var catPage = salesService.getMedicinesPage(1, 6, "", firstCat);
            assertNotNull(catPage);
            System.out.println("Lọc nhóm '" + firstCat + "': tìm thấy " + catPage.getTotalElements() + " thuốc");
            for (var m : catPage.getItems()) {
                assertEquals(firstCat, m.getCategoryName(), "Thuốc phải thuộc nhóm " + firstCat);
            }
        }
    }

    @Test
    void testSalesCheckoutFEFO() {
        SalesService salesService = new SalesServiceImpl();
        List<MedicineInventoryDTO> list = salesService.getMedicines("", "Tất cả");

        // Find a medicine with available stock > 2
        MedicineInventoryDTO availableMed = list.stream()
                .filter(m -> m.getTotalQuantity() != null && m.getTotalQuantity() > 2)
                .findFirst()
                .orElse(null);

        assertNotNull(availableMed, "Phải có thuốc còn tồn để test thanh toán");

        CartItem item = new CartItem(
                availableMed.getMedicineId(),
                availableMed.getMedicineCode(),
                availableMed.getMedicineName(),
                availableMed.getUnit(),
                Boolean.TRUE.equals(availableMed.getRequiresPrescription()),
                availableMed.getSellPrice(),
                1,
                availableMed.getTotalQuantity()
        );

        SalesOrderRequest request = new SalesOrderRequest();
        request.setEmployeeId(1);
        request.setCustomerPhone("0987654321");
        request.setDiscount(BigDecimal.ZERO);
        request.setPaymentMethod("CASH");
        request.setPrescriptionConfirmed(Boolean.TRUE.equals(availableMed.getRequiresPrescription()));
        request.setItems(List.of(item));

        String orderCode = salesService.checkout(request);
        assertNotNull(orderCode, "Mã đơn hàng không được null");
        assertTrue(orderCode.startsWith("HDB-"), "Mã đơn hàng phải bắt đầu bằng HDB-");
        System.out.println("Tạo đơn hàng thành công: " + orderCode);
    }

    @Test
    void testDiscountCappedToSubtotal() {
        com.pharmacy.sales.dto.CartOrder order = new com.pharmacy.sales.dto.CartOrder(1, "Hóa đơn test");
        CartItem item = new CartItem(
                1, "MED001", "Thuốc A", "Hộp", false,
                BigDecimal.valueOf(50000), 2, 10
        ); // Subtotal = 100,000
        order.addItem(item);

        assertEquals(BigDecimal.valueOf(100000), order.getSubtotal());

        // Giảm giá vượt quá tổng tiền hàng (200.000 > 100.000)
        order.setDiscount(BigDecimal.valueOf(200000));
        assertEquals(BigDecimal.valueOf(100000), order.getDiscount(), "Giảm giá phải bị giới hạn tối đa bằng tổng tiền hàng");
        assertEquals(BigDecimal.ZERO, order.getTotalAmount(), "Tổng thanh toán phải là 0, không được âm");

        // Giảm giá âm
        order.setDiscount(BigDecimal.valueOf(-50000));
        assertEquals(BigDecimal.ZERO, order.getDiscount(), "Giảm giá âm phải về 0");
        assertEquals(BigDecimal.valueOf(100000), order.getTotalAmount());

        // Giảm giá hợp lệ (30.000)
        order.setDiscount(BigDecimal.valueOf(30000));
        assertEquals(BigDecimal.valueOf(30000), order.getDiscount());
        assertEquals(BigDecimal.valueOf(70000), order.getTotalAmount());
    }

    @Test
    void testSalesHistoryAndOrderDetails() {
        SalesService salesService = new SalesServiceImpl();
        java.time.LocalDate today = java.time.LocalDate.now();

        // 1. Quản lý xem toàn hệ thống (employeeId = null)
        var allHistory = salesService.getSalesHistory(today, null, "", 1, 10);
        assertNotNull(allHistory);
        assertNotNull(allHistory.getItems());
        System.out.println("Lịch sử bán hàng hôm nay (Toàn hệ thống): " + allHistory.getTotalElements() + " đơn");

        // 2. Nhân viên chỉ xem của bản thân (employeeId = 1)
        var empHistory = salesService.getSalesHistory(today, 1, "", 1, 10);
        assertNotNull(empHistory);
        assertNotNull(empHistory.getItems());
        System.out.println("Lịch sử bán hàng hôm nay (Nhân viên 1): " + empHistory.getTotalElements() + " đơn");

        if (!allHistory.getItems().isEmpty()) {
            var firstOrder = allHistory.getItems().get(0);
            assertNotNull(firstOrder.getCode());
            assertNotNull(firstOrder.getFormattedTotal());

            // 3. Lấy chi tiết đơn hàng
            var details = salesService.getSalesOrderDetails(firstOrder.getId());
            assertNotNull(details);
            System.out.println("Đơn hàng " + firstOrder.getCode() + " có " + details.size() + " mặt hàng chi tiết");
            for (var d : details) {
                assertNotNull(d.getMedicineName());
                assertNotNull(d.getLotNumber());
                assertTrue(d.getQuantity() > 0);
            }
        }
    }
}
