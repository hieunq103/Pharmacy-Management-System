package com.pharmacy.profile;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.account.Role;
import com.pharmacy.common.UserSession;
import com.pharmacy.employee.Employee;
import com.pharmacy.employee.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileServiceImplTest {
    static class FakeEmployeeRepository extends EmployeeRepository {
        String savedName, savedPhone, savedAddress;

        @Override
        public java.util.Optional<com.pharmacy.employee.EmployeeConflict> findByPhone(String phone, Integer excludeId) {
            return java.util.Optional.empty();
        }

        @Override
        public boolean updateOwnProfile(Integer employeeId, String fullName, String phone, String address) {
            savedName = fullName;
            savedPhone = phone;
            savedAddress = address;
            return true;
        }
    }

    private final FakeEmployeeRepository repo = new FakeEmployeeRepository();
    private final ProfileService service = new ProfileServiceImpl(repo, new AccountRepository());

    private static void login(String roleCode, int employeeId) {
        Employee e = new Employee();
        e.setId(employeeId);
        Account a = new Account();
        a.setEmployee(e);
        a.setRole(new Role(roleCode, roleCode));
        UserSession.start(a);
    }

    @AfterEach
    void tearDown() {
        UserSession.invalidate();
    }

    @Test
    void staffCanUpdateOwnNamePhoneAddress() {
        login("STAFF", 5);
        service.updateProfile(new ProfileUpdateRequest(5, "  Nguyễn Văn B ", "0912345678", "Hà Nội"));
        assertEquals("Nguyễn Văn B", repo.savedName);
        assertEquals("0912345678", repo.savedPhone);
        assertEquals("Hà Nội", repo.savedAddress);
    }

    @Test
    void managerUsesSameRulesOnOwnProfile() {
        login("MANAGER", 1);
        service.updateProfile(new ProfileUpdateRequest(1, "Quản lý", "0912345678", "Hà Nội"));
        assertEquals("Quản lý", repo.savedName);
    }

    @Test
    void cannotUpdateSomeoneElsesProfile() {
        login("MANAGER", 1);
        assertThrows(SecurityException.class,
                () -> service.updateProfile(new ProfileUpdateRequest(2, "Người khác", "0912345678", "Hà Nội")));
        assertNull(repo.savedName);
    }

    @Test
    void validatesInput() {
        login("STAFF", 5);
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(new ProfileUpdateRequest(5, " ", "0912345678", "Hà Nội")));
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(new ProfileUpdateRequest(5, "A", "123", "Hà Nội")));
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(new ProfileUpdateRequest(5, "A", "0912345678", "")));
    }
}
