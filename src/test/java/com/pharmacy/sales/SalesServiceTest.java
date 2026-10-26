package com.pharmacy.sales;

import com.pharmacy.common.PageResult;
import com.pharmacy.inventory.InventoryRepository;
import com.pharmacy.inventory.dto.MedicineInventoryDTO;
import com.pharmacy.inventory.dto.MedicineLotDTO;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.SalesOrderRequest;
import com.pharmacy.support.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Integration test phân hệ Bán hàng (cần MySQL đã chạy migration V1..V5). */
public class SalesServiceTest {

    private final SalesService salesService = new SalesServiceImpl();
    private final InventoryRepository inventoryRepository = new InventoryRepository();

    @BeforeAll
    static void requireDb() {
        DbTestSupport.assumeDatabase();
    }

    @BeforeEach
    void login() {
        DbTestSupport.loginAsManager();
    }

    @AfterEach
    void logout() {
        DbTestSupport.logout();
    }

    private List<MedicineInventoryDTO> allSalesMedicines() {
        return salesService.getMedicinesPage(1, 10_000, "", "Tất cả").getItems();
    }

    private static CartItem cartItem(MedicineInventoryDTO m, int qty, BigDecimal price) {
        return new CartItem(m.getMedicineId(), m.getMedicineCode(), m.getMedicineName(), m.getUnit(),
                Boolean.TRUE.equals(m.getRequiresPrescription()), price, qty, m.getTotalQuantity());
    }

    private static SalesOrderRequest request(CartItem item) {
        SalesOrderRequest request = new SalesOrderRequest(null, "0987654321", BigDecimal.ZERO, "CASH", true, List.of(item));
        request.setOrderCode("HDB-TEST-" + System.nanoTime() % 1_000_000_000L);
        return request;
    }

    private int lotRemaining(int medicineId, long lotId) {
        return inventoryRepository.getLotsByMedicineId(medicineId).stream()
                .filter(l -> l.getId() == lotId).findFirst().orElseThrow().getQuantityRemaining();
    }

    private MedicineInventoryDTO anyInStock() {
        return allSalesMedicines().stream().filter(m -> m.getTotalQuantity() > 0).findFirst().orElseThrow();
    }

    @Test
    void testSalesPaginationAndFilter() {
        PageResult<MedicineInventoryDTO> page1 = salesService.getMedicinesPage(1, 6, "", "Tất cả nhóm thuốc");
        assertEquals(6, page1.getItems().size(), "Trang 1 phải có đúng 6 thuốc");
        assertTrue(page1.getTotalPages() >= 2);

        // Thuốc còn hàng bán được luôn đứng trước thuốc hết hàng
        List<MedicineInventoryDTO> all = allSalesMedicines();
        int firstOut = -1;
        for (int i = 0; i < all.size(); i++) {
            boolean out = all.get(i).getTotalQuantity() == 0;
            if (out && firstOut < 0) firstOut = i;
            if (!out && firstOut >= 0) fail("Thuốc còn hàng ở vị trí " + i + " đứng sau thuốc hết hàng ở " + firstOut);
        }

        String firstCat = salesService.getCategories().get(0);
        for (MedicineInventoryDTO m : salesService.getMedicinesPage(1, 6, "", firstCat).getItems()) {
            assertEquals(firstCat, m.getCategoryName());
        }
    }

    /** FEFO: trừ lô còn hạn gần nhất, KHÔNG trừ lô đã hết hạn. */
    @Test
    void testCheckoutUsesEarliestNonExpiredLot() {
        LocalDate today = LocalDate.now();
        MedicineInventoryDTO med = allSalesMedicines().stream()
                .filter(m -> m.getTotalQuantity() > 0 && m.getStatus() == MedicineInventoryDTO.InventoryStatus.HAS_EXPIRED_LOT)
                .findFirst()
                .orElseGet(this::anyInStock);
        List<MedicineLotDTO> lots = inventoryRepository.getLotsByMedicineId(med.getMedicineId());
        MedicineLotDTO fefoLot = lots.stream()
                .filter(l -> l.getQuantityRemaining() > 0 && l.getExpiryDate().isAfter(today))
                .min(Comparator.comparing(MedicineLotDTO::getExpiryDate).thenComparing(MedicineLotDTO::getId))
                .orElseThrow();
        List<MedicineLotDTO> expiredLots = lots.stream()
                .filter(l -> l.getQuantityRemaining() > 0 && !l.getExpiryDate().isAfter(today)).toList();

        assertNotNull(salesService.checkout(request(cartItem(med, 1, med.getSellPrice()))));

        assertEquals(fefoLot.getQuantityRemaining() - 1, lotRemaining(med.getMedicineId(), fefoLot.getId()));
        for (MedicineLotDTO expired : expiredLots) {
            assertEquals(expired.getQuantityRemaining(), lotRemaining(med.getMedicineId(), expired.getId()),
                    "Không được bán từ lô đã hết hạn " + expired.getLotNumber());
        }
    }

    @Test
    void testCheckoutRejectsOverSellableQuantity() {
        MedicineInventoryDTO med = anyInStock();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> salesService.checkout(request(cartItem(med, med.getTotalQuantity() + 1, med.getSellPrice()))));
        assertTrue(ex.getMessage().contains("không đủ số lượng"), ex.getMessage());
    }

    @Test
    void testCheckoutRejectsTamperedPrice() {
        MedicineInventoryDTO med = anyInStock();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> salesService.checkout(request(cartItem(med, 1, BigDecimal.ONE))));
        assertTrue(ex.getMessage().contains("đã thay đổi"), ex.getMessage());
    }

    @Test
    void testCheckoutRequiresLogin() {
        MedicineInventoryDTO med = anyInStock();
        DbTestSupport.logout();
        assertThrows(SecurityException.class, () -> salesService.checkout(request(cartItem(med, 1, med.getSellPrice()))));
    }

    @Test
    void testSalesHistoryAndOrderDetails() {
        LocalDate today = LocalDate.now();
        var allHistory = salesService.getSalesHistory(today, null, "", 1, 10);
        assertNotNull(allHistory.getItems());
        for (var order : allHistory.getItems()) {
            var details = salesService.getSalesOrderDetails(order.getId());
            assertEquals(order.getTotalQuantity(), details.stream().mapToInt(d -> d.getQuantity()).sum());
        }

        // Nhân viên chỉ xem được đơn của chính mình dù UI truyền employeeId = null
        var staff = DbTestSupport.loginAs(DbTestSupport.STAFF_PHONE);
        for (var order : salesService.getSalesHistory(today, null, "", 1, 50).getItems()) {
            assertEquals(staff.getEmployee().getId(), order.getEmployeeId());
        }
    }
}
