package com.pharmacy.auth;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.UserSession;
import com.pharmacy.infra.config.AppConfig;
import com.pharmacy.infra.exception.AuthenticationException;
import com.pharmacy.infra.exception.ErrorCode;
import com.pharmacy.infra.security.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String CLIENT_INFO = "JavaFX Desktop Client";

    private final AccountRepository accountRepository;

    public AuthServiceImpl() {
        this(new AccountRepository());
    }

    public AuthServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account login(LoginRequest request) {
        // Tìm tài khoản duy nhất bằng tên đăng nhập (Mã nhân viên)
        Account account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: Tài khoản không tồn tại [{}]", request.username());
                    return new AuthenticationException(ErrorCode.INVALID_CREDENTIALS,
                            "Tài khoản hoặc mật khẩu không chính xác.");
                });

        String employeeName = (account.getEmployee() != null && account.getEmployee().getFullName() != null)
                ? account.getEmployee().getFullName()
                : "N/A";
        Integer accountId = account.getId();

        // Kiểm tra mật khẩu TRƯỚC trạng thái để không tiết lộ tình trạng tài khoản cho người không biết mật khẩu
        if (!PasswordEncoder.matches(request.password(), account.getPasswordHash())) {
            log.warn("Nhân viên [{}] - Mật khẩu không chính xác", employeeName);
            accountRepository.saveLoginHistory(accountId, "FAILED", CLIENT_INFO);
            throw new AuthenticationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Tài khoản hoặc mật khẩu không chính xác.");
        }

        if (account.getStatus() == Account.Status.DISABLED) {
            log.warn("Nhân viên [{}] - Tài khoản đã bị vô hiệu hóa", employeeName);
            throw new AuthenticationException(
                    ErrorCode.ACCOUNT_DISABLED,
                    "Tài khoản này đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        // Tài khoản mới (cờ must_change_password) hoặc vẫn dùng mật khẩu khởi tạo mặc định -> bắt buộc đổi
        if (request.password().equals(defaultPassword())) {
            account.setMustChangePassword(true);
        }

        accountRepository.recordSuccessfulLogin(accountId, CLIENT_INFO);
        UserSession.start(account);

        log.info("Nhân viên [{}] đăng nhập thành công", UserSession.getInstance().getEmployeeName());
        return account;
    }

    /** Mật khẩu khởi tạo cho tài khoản mới (cấu hình auth.defaultPassword). */
    public static String defaultPassword() {
        return AppConfig.get("auth.defaultPassword", "12345678");
    }

    @Override
    public void logout() {
        if (UserSession.isLoggedIn()) {
            log.info("Nhân viên [{}] đã đăng xuất", UserSession.getInstance().getEmployeeName());
        }
        UserSession.invalidate();
    }

    @Override
    public Optional<Account> getCurrentUser() {
        if (UserSession.isLoggedIn()) {
            return Optional.ofNullable(UserSession.getInstance().getAccount());
        }
        return Optional.empty();
    }

    @Override
    public boolean isAuthenticated() {
        return UserSession.isLoggedIn();
    }
}
