package com.pharmacy.employee;

import com.pharmacy.common.PageResult;
import com.pharmacy.support.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Integration test phân hệ Nhân viên (cần MySQL). */
class EmployeeServiceTest {

    private final EmployeeService service = new EmployeeServiceImpl();

    @BeforeAll
    static void requireDb() {
        DbTestSupport.assumeDatabase();
    }

    @AfterEach
    void logout() {
        DbTestSupport.logout();
    }

    @Test
    void managerCanSearchEmployees() {
        DbTestSupport.loginAsManager();
        PageResult<Employee> page = service.getEmployeesPage(1, 7, null, null, null, null, null);
        assertTrue(page.getTotalElements() > 0);
        Employee first = page.getItems().get(0);
        assertNotNull(first.getAccount(), "Account phải được JOIN FETCH sẵn");

        PageResult<Employee> byCccd = service.getEmployeesPage(1, 7, first.getNationalId(), null, null, null, null);
        assertEquals(first.getNationalId(), byCccd.getItems().get(0).getNationalId());
        assertTrue(service.getEmployeesPage(1, 7, first.getFullName(), null, null, null, null).getTotalElements() > 0);
        PageResult<Employee> byCode = service.getEmployeesPage(1, 7, first.getCode(), null, null, null, null);
        assertEquals(first.getCode(), byCode.getItems().get(0).getCode(), "Tìm theo mã nhân viên");
        assertTrue(service.getEmployeeSummary().total() >= page.getTotalElements());
    }

    /** Bộ lọc đa chọn (Giới tính, Vai trò, Tài khoản): chọn nhiều giá trị = hợp các kết quả chọn riêng lẻ. */
    @Test
    void multiSelectFilters() {
        DbTestSupport.loginAsManager();
        long all = service.getEmployeesPage(1, 7, null, null, null, null, null).getTotalElements();
        long nam = service.getEmployeesPage(1, 7, null, "Nam", null, null, null).getTotalElements();
        long nu = service.getEmployeesPage(1, 7, null, "Nữ", null, null, null).getTotalElements();
        long khac = service.getEmployeesPage(1, 7, null, "Khác", null, null, null).getTotalElements();
        assertEquals(nam + nu, service.getEmployeesPage(1, 7, null, "Nam,Nữ", null, null, null).getTotalElements());
        assertEquals(all, nam + nu + khac);

        long staff = service.getEmployeesPage(1, 7, null, null, "Nhân viên", null, null).getTotalElements();
        long manager = service.getEmployeesPage(1, 7, null, null, "Quản lý", null, null).getTotalElements();
        assertEquals(staff + manager, service.getEmployeesPage(1, 7, null, null, "Nhân viên,Quản lý", null, null).getTotalElements());

        long active = service.getEmployeesPage(1, 7, null, null, null, null, "Hoạt động").getTotalElements();
        long disabled = service.getEmployeesPage(1, 7, null, null, null, null, "Ngưng hoạt động").getTotalElements();
        assertEquals(active + disabled, service.getEmployeesPage(1, 7, null, null, null, null, "Hoạt động,Ngưng hoạt động").getTotalElements());
        assertEquals(service.getEmployeesPage(1, 7, null, "Nam", "Nhân viên", null, null).getTotalElements(),
                service.getEmployeesPage(1, 7, null, "Nam", "Nhân viên", null, "Hoạt động,Ngưng hoạt động").getTotalElements());
    }

    @Test
    void staffCannotUseEmployeeModule() {
        var staff = DbTestSupport.loginAs(DbTestSupport.STAFF_USERNAME);
        assertThrows(SecurityException.class, () -> service.getEmployeesPage(1, 7, null, null, null, null, null));
        assertThrows(SecurityException.class, () -> service.lockAccount(staff.getId()));
        assertThrows(SecurityException.class, () -> service.markAsInactive(staff.getEmployee().getId()));
        assertThrows(SecurityException.class, () -> service.getLoginHistory(staff.getId()));
    }

    @Test
    void managerCannotLockOwnAccount() {
        var manager = DbTestSupport.loginAsManager();
        assertThrows(IllegalArgumentException.class, () -> service.lockAccount(manager.getId()));
        assertThrows(IllegalArgumentException.class, () -> service.markAsInactive(manager.getEmployee().getId()));
    }

    @Test
    void newEmployeeMustChangePasswordOnFirstLogin() {
        DbTestSupport.loginAsManager();
        long n = System.nanoTime() % 100_000_000L;
        Employee e = new Employee();
        e.setFullName("Nhân viên Test " + n);
        e.setDob(LocalDate.of(1999, 1, 1));
        e.setGender(Employee.GENDER_FEMALE);
        e.setNationalId(String.format("0790%08d", n));
        e.setPhone(String.format("09%08d", n));
        e.setHiredDate(LocalDate.now());
        assertTrue(service.createEmployeeWithAccount(e, "STAFF"));

        assertTrue(e.getCode() != null && e.getCode().startsWith("PMS"), "Mã nhân viên tự sinh: " + e.getCode());
        var account = new com.pharmacy.account.AccountRepository().findByUsername(e.getCode()).orElseThrow();
        assertEquals(e.getCode(), account.getUsername(), "Tên đăng nhập = mã nhân viên");
        assertTrue(account.isMustChangePassword());
        assertThrows(IllegalArgumentException.class, () -> service.createEmployeeWithAccount(e, "STAFF"),
                "Trùng CCCD/SĐT phải bị chặn");
    }
}
