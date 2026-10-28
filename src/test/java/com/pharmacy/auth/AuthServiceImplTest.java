package com.pharmacy.auth;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.account.Role;
import com.pharmacy.common.UserSession;
import com.pharmacy.employee.Employee;
import com.pharmacy.infra.exception.AuthenticationException;
import com.pharmacy.infra.exception.ErrorCode;
import com.pharmacy.infra.security.PasswordEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceImplTest {
    private static final String USERNAME = "PMS2609099";
    private static final String STRONG_PASSWORD = "MatKhau@2026";

    private FakeAccountRepository repo;
    private AuthServiceImpl service;

    static class FakeAccountRepository extends AccountRepository {
        Account account;
        final List<String> history = new ArrayList<>();

        @Override
        public Optional<Account> findByUsername(String username) {
            return USERNAME.equals(username) ? Optional.of(account) : Optional.empty();
        }

        @Override
        public void saveLoginHistory(Integer accountId, String result, String clientInfo) {
            history.add(result);
        }

        @Override
        public void recordSuccessfulLogin(Integer accountId, String clientInfo) {
            history.add("SUCCESS");
        }
    }

    private static Account account(String rawPassword, Account.Status status) {
        Employee e = new Employee();
        e.setId(7);
        e.setFullName("Test");
        e.setCode(USERNAME);
        Account a = new Account();
        a.setId(70);
        a.setEmployee(e);
        a.setUsername(USERNAME);
        a.setRole(new Role("STAFF", "Nhân viên"));
        a.setPasswordHash(PasswordEncoder.hash(rawPassword));
        a.setStatus(status);
        return a;
    }

    @BeforeEach
    void setUp() {
        repo = new FakeAccountRepository();
        service = new AuthServiceImpl(repo);
    }

    @AfterEach
    void tearDown() {
        UserSession.invalidate();
    }

    @Test
    void wrongPasswordIsRecordedAsFailed() {
        repo.account = account(STRONG_PASSWORD, Account.Status.ACTIVE);
        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> service.login(new LoginRequest(USERNAME, "sai-mat-khau")));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertEquals(List.of("FAILED"), repo.history);
        assertFalse(UserSession.isLoggedIn());
    }

    @Test
    void disabledAccountIsRevealedOnlyWithCorrectPassword() {
        repo.account = account(STRONG_PASSWORD, Account.Status.DISABLED);
        assertEquals(ErrorCode.INVALID_CREDENTIALS, assertThrows(AuthenticationException.class,
                () -> service.login(new LoginRequest(USERNAME, "sai"))).getErrorCode());
        assertEquals(ErrorCode.ACCOUNT_DISABLED, assertThrows(AuthenticationException.class,
                () -> service.login(new LoginRequest(USERNAME, STRONG_PASSWORD))).getErrorCode());
    }

    @Test
    void defaultPasswordForcesPasswordChange() {
        repo.account = account(AuthServiceImpl.defaultPassword(), Account.Status.ACTIVE);
        Account logged = service.login(new LoginRequest(USERNAME, AuthServiceImpl.defaultPassword()));
        assertTrue(logged.isMustChangePassword());
        assertTrue(UserSession.isLoggedIn());
    }

    @Test
    void ownPasswordDoesNotForceChange() {
        repo.account = account(STRONG_PASSWORD, Account.Status.ACTIVE);
        assertFalse(service.login(new LoginRequest(USERNAME, STRONG_PASSWORD)).isMustChangePassword());
        assertEquals(List.of("SUCCESS"), repo.history);
    }
}
