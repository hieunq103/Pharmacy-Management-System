package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.account.Role;
import com.pharmacy.common.UserSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeServiceSelfEditTest {
    private FakeEmployeeRepository fakeRepository;
    private EmployeeServiceImpl employeeService;
    private Employee currentManager;
    private Account currentAccount;

    static class FakeEmployeeRepository extends EmployeeRepository {
        boolean updateCalled = false;
        boolean markInactiveCalled = false;

        @Override
        public boolean updateEmployeeByManager(EmployeeUpdateRequest req) {
            updateCalled = true;
            return true;
        }

        @Override
        public java.util.Optional<EmployeeConflict> findByNationalId(String nationalId, Integer id) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<EmployeeConflict> findByPhone(String phone, Integer id) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean markAsInactive(Integer employeeId) {
            markInactiveCalled = true;
            return true;
        }
    }

    @BeforeEach
    void setUp() {
        fakeRepository = new FakeEmployeeRepository();
        employeeService = new EmployeeServiceImpl(fakeRepository);

        currentManager = new Employee();
        currentManager.setId(1);
        currentManager.setCode("PMS2601001");
        currentManager.setFullName("Nguyễn Văn Quản Lý");
        currentManager.setStatus(Employee.Status.ACTIVE);

        Role managerRole = new Role();
        managerRole.setId(1);
        managerRole.setCode("MANAGER");
        managerRole.setName("Quản lý cửa hàng");

        currentAccount = new Account();
        currentAccount.setId(10);
        currentAccount.setUsername("PMS2601001");
        currentAccount.setStatus(Account.Status.ACTIVE);
        currentAccount.setRole(managerRole);
        currentAccount.setEmployee(currentManager);
        currentManager.setAccount(currentAccount);

        UserSession.start(currentAccount);
    }

    @AfterEach
    void tearDown() {
        UserSession.invalidate();
    }

    @Test
    void testManagerCannotSelfDemoteRole() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                1,
                "Nguyễn Văn Quản Lý",
                "001090000001",
                "0987654321",
                "Hà Nội",
                LocalDate.of(1990, 1, 1),
                "Nam",
                "Đang làm việc",
                "Đang hoạt động",
                "Nhân viên bán thuốc"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.updateEmployeeByManager(request);
        });

        assertTrue(ex.getMessage().contains("vai trò hệ thống"));
        assertFalse(fakeRepository.updateCalled);
    }

    @Test
    void testManagerCannotSelfMarkAsInactiveViaUpdateRequest() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                1,
                "Nguyễn Văn Quản Lý",
                "001090000001",
                "0987654321",
                "Hà Nội",
                LocalDate.of(1990, 1, 1),
                "Nam",
                "Đã nghỉ việc",
                "Đang hoạt động",
                "Quản lý cửa hàng"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.updateEmployeeByManager(request);
        });

        assertTrue(ex.getMessage().contains("nghỉ việc"));
        assertFalse(fakeRepository.updateCalled);
    }

    @Test
    void testManagerCannotSelfLockAccountViaUpdateRequest() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                1,
                "Nguyễn Văn Quản Lý",
                "001090000001",
                "0987654321",
                "Hà Nội",
                LocalDate.of(1990, 1, 1),
                "Nam",
                "Đang làm việc",
                "Đang bị khóa",
                "Quản lý cửa hàng"
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.updateEmployeeByManager(request);
        });

        assertTrue(ex.getMessage().contains("khóa tài khoản"));
        assertFalse(fakeRepository.updateCalled);
    }

    @Test
    void testManagerCannotSelfMarkAsInactiveDirectly() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.markAsInactive(1);
        });

        assertTrue(ex.getMessage().contains("nghỉ việc"));
        assertFalse(fakeRepository.markInactiveCalled);
    }

    @Test
    void testManagerCannotSelfLockAccountDirectly() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            employeeService.lockAccount(10);
        });

        assertTrue(ex.getMessage().contains("khóa tài khoản"));
    }

    @Test
    void testManagerSelfEditOnlyNamePhoneAddress() {
        currentManager.setNationalId("001090000001");
        currentManager.setDob(LocalDate.of(1990, 1, 1));
        currentManager.setGender("Nam");

        EmployeeUpdateRequest allowed = new EmployeeUpdateRequest(1, "Tên Mới", "001090000001", "0987654321",
                "Hà Đông", LocalDate.of(1990, 1, 1), "Nam", "Đang làm việc", "Đang hoạt động", "Quản lý cửa hàng");
        assertTrue(employeeService.updateEmployeeByManager(allowed));

        EmployeeUpdateRequest changeCccd = new EmployeeUpdateRequest(1, "Tên Mới", "001090000099", "0987654321",
                "Hà Đông", LocalDate.of(1990, 1, 1), "Nam", "Đang làm việc", "Đang hoạt động", "Quản lý cửa hàng");
        EmployeeUpdateRequest changeDob = new EmployeeUpdateRequest(1, "Tên Mới", "001090000001", "0987654321",
                "Hà Đông", LocalDate.of(1991, 1, 1), "Nam", "Đang làm việc", "Đang hoạt động", "Quản lý cửa hàng");
        EmployeeUpdateRequest changeGender = new EmployeeUpdateRequest(1, "Tên Mới", "001090000001", "0987654321",
                "Hà Đông", LocalDate.of(1990, 1, 1), "Nữ", "Đang làm việc", "Đang hoạt động", "Quản lý cửa hàng");
        for (EmployeeUpdateRequest req : List.of(changeCccd, changeDob, changeGender)) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> employeeService.updateEmployeeByManager(req));
            assertTrue(ex.getMessage().contains("chỉ có thể thay đổi"));
        }
    }

    @Test
    void testManagerCanEditOtherEmployeeRoleAndStatus() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(
                2,
                "Trần Văn Nhân Viên",
                "001090000002",
                "0912345678",
                "Hà Nội",
                LocalDate.of(1995, 5, 5),
                "Nam",
                "Đang làm việc",
                "Đang hoạt động",
                "Quản lý cửa hàng"
        );

        boolean result = employeeService.updateEmployeeByManager(request);
        assertTrue(result, "Quản lý phải được phép điều chỉnh role/trạng thái của nhân viên khác");
        assertTrue(fakeRepository.updateCalled);
    }
}
