package com.pharmacy.common;

import com.pharmacy.account.Account;
import com.pharmacy.account.Role;
import com.pharmacy.employee.Employee;
import com.pharmacy.employee.EmployeeServiceImplAccess;
import com.pharmacy.inventory.InventoryServiceImplAccess;
import com.pharmacy.inventory.dto.BatchImportItemDTO;
import com.pharmacy.sales.SalesServiceImplAccess;
import com.pharmacy.sales.dto.CartItem;
import com.pharmacy.sales.dto.SalesOrderRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesTest {
    @AfterEach
    void tearDown() {
        UserSession.invalidate();
    }

    private static CartItem item(int qty, boolean prescription) {
        return new CartItem(1, "MED001", "Thuốc A", "Hộp", prescription, new BigDecimal("10000"), qty, 100);
    }

    private static SalesOrderRequest order(List<CartItem> items, String method, boolean confirmed, String phone) {
        return new SalesOrderRequest(1, phone, BigDecimal.ZERO, method, confirmed, items);
    }

    @Test
    void checkoutValidation() {
        assertDoesNotThrow(() -> SalesServiceImplAccess.validate(order(List.of(item(2, false)), "CASH", false, null)));
        assertThrows(IllegalArgumentException.class, () -> SalesServiceImplAccess.validate(order(List.of(), "CASH", false, null)));
        assertThrows(IllegalArgumentException.class, () -> SalesServiceImplAccess.validate(order(List.of(item(0, false)), "CASH", false, null)));
        assertThrows(IllegalArgumentException.class, () -> SalesServiceImplAccess.validate(order(List.of(item(-3, false)), "CASH", false, null)));
        assertThrows(IllegalArgumentException.class, () -> SalesServiceImplAccess.validate(order(List.of(item(1, false)), "BITCOIN", false, null)));
        assertThrows(IllegalArgumentException.class, () -> SalesServiceImplAccess.validate(order(List.of(item(1, true)), "CASH", false, null)),
                "Thuốc kê đơn phải xác nhận đơn thuốc");
        assertThrows(IllegalArgumentException.class, () -> SalesServiceImplAccess.validate(order(List.of(item(1, false)), "CASH", false, "12345")));
        assertDoesNotThrow(() -> SalesServiceImplAccess.validate(order(List.of(item(1, true)), "TRANSFER", true, "0987654321")));
    }

    private static BatchImportItemDTO lot(String lotNo, int qty, LocalDate expiry) {
        return BatchImportItemDTO.fromExisting(1, "MED001", "Thuốc A", null, null, lotNo, expiry, "Hộp", qty, BigDecimal.TEN);
    }

    @Test
    void importValidation() {
        LocalDate future = LocalDate.now().plusMonths(6);
        assertDoesNotThrow(() -> InventoryServiceImplAccess.validate(List.of(lot("L1", 5, future))));
        assertThrows(IllegalArgumentException.class, () -> InventoryServiceImplAccess.validate(List.of()));
        assertThrows(IllegalArgumentException.class, () -> InventoryServiceImplAccess.validate(List.of(lot(" ", 5, future))));
        assertThrows(IllegalArgumentException.class, () -> InventoryServiceImplAccess.validate(List.of(lot("L1", 0, future))));
        assertThrows(IllegalArgumentException.class, () -> InventoryServiceImplAccess.validate(List.of(lot("L1", 5, LocalDate.now()))),
                "Không nhập lô đã/đang hết hạn");
        BatchImportItemDTO a = BatchImportItemDTO.fromNew(null, "Thuốc Mới", null, null, null, BigDecimal.ONE, false, "L1", future, "Hộp", 1, BigDecimal.ONE);
        BatchImportItemDTO b = BatchImportItemDTO.fromNew(null, "thuốc mới ", null, null, null, BigDecimal.ONE, false, "L2", future, "Hộp", 1, BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> InventoryServiceImplAccess.validate(List.of(a, b)),
                "Thuốc mới trùng tên trong cùng phiếu");
    }

    @Test
    void employeeValidation() {
        LocalDate adult = LocalDate.now().minusYears(25);
        assertDoesNotThrow(() -> EmployeeServiceImplAccess.validate("Nguyễn Văn A", "0912345678", "001095000123", adult));
        assertThrows(IllegalArgumentException.class, () -> EmployeeServiceImplAccess.validate(" ", "0912345678", "001095000123", adult));
        assertThrows(IllegalArgumentException.class, () -> EmployeeServiceImplAccess.validate("A", "0112345678", "001095000123", adult));
        assertThrows(IllegalArgumentException.class, () -> EmployeeServiceImplAccess.validate("A", "0912345678", "12345", adult));
        assertThrows(IllegalArgumentException.class, () -> EmployeeServiceImplAccess.validate("A", "0912345678", "001095000123",
                LocalDate.now().minusYears(17)));
    }

    private static Account account(String roleCode, int employeeId) {
        Employee e = new Employee();
        e.setId(employeeId);
        Account a = new Account();
        a.setEmployee(e);
        a.setRole(new Role(roleCode, roleCode));
        return a;
    }

    @Test
    void staffIsScopedToOwnData() {
        assertEquals(5, UserSession.scopeEmployeeId(5), "Chưa đăng nhập: giữ nguyên tham số");
        UserSession.start(account("STAFF", 9));
        assertEquals(9, UserSession.scopeEmployeeId(null));
        assertEquals(9, UserSession.scopeEmployeeId(5));
        assertThrows(SecurityException.class, UserSession::requireManager);
        UserSession.start(account("MANAGER", 1));
        assertNull(UserSession.scopeEmployeeId(null));
        assertEquals(5, UserSession.scopeEmployeeId(5));
        assertDoesNotThrow(UserSession::requireManager);
    }

    @Test
    void sqlRowConversions() {
        assertEquals(LocalDate.of(2026, 1, 2), SqlRows.toLocalDate(java.sql.Date.valueOf("2026-01-02")));
        assertEquals(0, new BigDecimal("12.50").compareTo(SqlRows.toBigDecimal(12.5d)));
        assertEquals(BigDecimal.ZERO, SqlRows.toBigDecimal(null));
        assertEquals(3L, SqlRows.toLong(java.math.BigInteger.valueOf(3)));
        assertTrue(SqlRows.toBoolean((byte) 1));
        assertNull(SqlRows.toInteger(null));
    }
}
