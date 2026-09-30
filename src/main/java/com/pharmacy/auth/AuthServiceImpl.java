package com.pharmacy.auth;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.UserSession;
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
        this.accountRepository = new AccountRepository();
    }

    public AuthServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account login(LoginRequest request) {
        // 1. Tìm tài khoản bằng SĐT hoặc CCCD
        Account account = accountRepository.findByIdentifier(request.identifier())
                .orElseThrow(() -> {
                    log.warn("Đăng nhập thất bại: Tài khoản không tồn tại [{}]", request.identifier());
                    return new AuthenticationException(ErrorCode.INVALID_CREDENTIALS, "Tài khoản hoặc mật khẩu không chính xác.");
                });

        String employeeName = (account.getEmployee() != null && account.getEmployee().getFullName() != null)
                ? account.getEmployee().getFullName()
                : "N/A";

        Integer accountId = account.getId();

        // 2. Chỉ kiểm tra trạng thái vô hiệu hóa bởi Quản trị viên
        if (account.getStatus() == Account.Status.DISABLED) {
            log.warn("Nhân viên [{}] - Tài khoản đã bị vô hiệu hóa", employeeName);
            throw new AuthenticationException(
                    ErrorCode.ACCOUNT_DISABLED,
                    "Tài khoản này đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên."
            );
        }

        // 3. Kiểm tra mật khẩu
        if (!PasswordEncoder.matches(request.password(), account.getPasswordHash())) {
            log.warn("Nhân viên [{}] - Mật khẩu không chính xác", employeeName);
            // Ghi nhận lịch sử đăng nhập thất bại
            accountRepository.saveLoginHistory(accountId, "FAILED", CLIENT_INFO);

            throw new AuthenticationException(
                    ErrorCode.INVALID_CREDENTIALS,
                    "Tài khoản hoặc mật khẩu không chính xác."
            );
        }

        // 4. Xác thực thành công: Ghi nhận lịch sử SUCCESS và khởi tạo Session
        accountRepository.recordSuccessfulLogin(accountId, CLIENT_INFO);
        UserSession.start(account);

        log.info("Nhân viên [{}] đăng nhập thành công", UserSession.getInstance().getEmployeeName());
        return account;
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