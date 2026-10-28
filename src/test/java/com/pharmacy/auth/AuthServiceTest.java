package com.pharmacy.auth;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.FormValidator;
import com.pharmacy.common.UserSession;
import com.pharmacy.employee.EmployeeService;
import com.pharmacy.employee.EmployeeServiceImpl;
import com.pharmacy.infra.exception.AuthenticationException;
import com.pharmacy.support.DbTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private AuthService authService;
    private EmployeeService employeeService;

    @BeforeAll
    static void requireDb() {
        DbTestSupport.assumeDatabase();
    }

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
        Account account = authService.login(new LoginRequest("PMS2601001", "12345678"));
        assertNotNull(account, "Đăng nhập bằng mã nhân viên PMS2601001 phải thành công");
        assertEquals("PMS2601001", account.getUsername());
        assertEquals("PMS2601001", account.getEmployee().getCode());
        assertTrue(UserSession.isLoggedIn(), "UserSession phải ở trạng thái đã đăng nhập");
        assertEquals("PMS2601001", UserSession.getInstance().getUsername());
        assertEquals("PMS2601001", UserSession.getInstance().getEmployeeCode());
    }

    @Test
    void testLoginWithPhoneFails() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login(new LoginRequest("0985549854", "12345678"));
        }, "Đăng nhập bằng số điện thoại không còn được hỗ trợ và phải ném ngoại lệ");
    }

    @Test
    void testLoginWithCccdFails() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login(new LoginRequest("001090000001", "12345678"));
        }, "Đăng nhập bằng CCCD không còn được hỗ trợ và phải ném ngoại lệ");
    }

    @Test
    void testFormValidatorForUsername() {
        FormValidator.ValidationResult resEmpty = FormValidator.validateLoginForm("", "");
        assertFalse(resEmpty.isValid());

        FormValidator.ValidationResult resUser = FormValidator.validateLoginForm("PMS2601001", "12345678");
        assertTrue(resUser.isValid());
    }
}
