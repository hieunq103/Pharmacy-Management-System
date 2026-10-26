package com.pharmacy.auth;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.FormValidator;
import com.pharmacy.common.UserSession;
import com.pharmacy.employee.EmployeeService;
import com.pharmacy.employee.EmployeeServiceImpl;
import com.pharmacy.infra.exception.AuthenticationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl();
        employeeService = new EmployeeServiceImpl();
        UserSession.invalidate();
    }

    @AfterEach
    void tearDown() {
        UserSession.invalidate();
    }

    @Test
    void testLoginWithUsernameSuccess() {
        // Đăng nhập bằng mã nhân viên/username chuẩn PMS0001
        Account account = authService.login(new LoginRequest("PMS0001", "12345678"));
        assertNotNull(account, "Đăng nhập bằng mã nhân viên PMS0001 phải thành công");
        assertEquals("PMS0001", account.getUsername());
        assertEquals("PMS0001", account.getEmployee().getCode());
        assertTrue(UserSession.isLoggedIn(), "UserSession phải ở trạng thái đã đăng nhập");
        assertEquals("PMS0001", UserSession.getInstance().getUsername());
        assertEquals("PMS0001", UserSession.getInstance().getEmployeeCode());
    }

    @Test
    void testLoginWithPhoneFails() {
        // Hệ thống đã chuyển sang chỉ cho đăng nhập bằng user/mã NV. Thử đăng nhập bằng số điện thoại phải bị từ chối
        assertThrows(AuthenticationException.class, () -> {
            authService.login(new LoginRequest("0985549854", "12345678"));
        }, "Đăng nhập bằng số điện thoại không còn được hỗ trợ và phải ném ngoại lệ");
    }

    @Test
    void testLoginWithCccdFails() {
        // Thử đăng nhập bằng CCCD cũng phải bị từ chối
        assertThrows(AuthenticationException.class, () -> {
            authService.login(new LoginRequest("001090000001", "12345678"));
        }, "Đăng nhập bằng CCCD không còn được hỗ trợ và phải ném ngoại lệ");
    }

    @Test
    void testEmployeeCodeFormatAndGeneration() {
        String nextCode = employeeService.generateNextEmployeeCode();
        assertNotNull(nextCode);
        assertTrue(nextCode.matches("^PMS\\d{4,}$"), "Mã nhân viên tự động sinh phải có format PMSxxxx: " + nextCode);
    }

    @Test
    void testFormValidatorForUsername() {
        // Form trống
        FormValidator.ValidationResult resEmpty = FormValidator.validateLoginForm("", "");
        assertFalse(resEmpty.isValid());

        // Username hợp lệ bất kỳ (không ép buộc regex số điện thoại hay CCCD nữa)
        FormValidator.ValidationResult resUser = FormValidator.validateLoginForm("PMS0001", "12345678");
        assertTrue(resUser.isValid());
    }
}
