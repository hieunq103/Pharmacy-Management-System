package com.pharmacy.profile;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.FormValidator;
import com.pharmacy.common.UserSession;
import com.pharmacy.employee.EmployeeRepository;
import com.pharmacy.infra.security.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileServiceImpl implements ProfileService {
    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;

    public ProfileServiceImpl() {
        this(new EmployeeRepository(), new AccountRepository());
    }

    public ProfileServiceImpl(EmployeeRepository employeeRepository, AccountRepository accountRepository) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    public void updateProfile(ProfileUpdateRequest request) {
        if (request == null || request.employeeId() == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu cập nhật không hợp lệ.");
        }

        UserSession.requireAuthenticated();
        if (!request.employeeId().equals(UserSession.currentEmployeeId())) {
            throw new SecurityException("Chỉ được cập nhật hồ sơ cá nhân của chính mình.");
        }

        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        if (request.fullName().trim().length() > 100) {
            throw new IllegalArgumentException("Họ và tên không được vượt quá 100 ký tự.");
        }
        if (request.phone() == null || request.phone().isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (!FormValidator.isValidPhone(request.phone())) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09).");
        }
        if (request.address() == null || request.address().isBlank()) {
            throw new IllegalArgumentException("Địa chỉ không được để trống.");
        }
        if (request.address().length() > 255) {
            throw new IllegalArgumentException("Địa chỉ không được vượt quá 255 ký tự.");
        }

        log.info("Bắt đầu cập nhật hồ sơ cá nhân cho nhân viên ID: {}", request.employeeId());

        employeeRepository.findByPhone(request.phone(), request.employeeId()).ifPresent(c -> {
            log.warn("Cập nhật thất bại: Số điện thoại {} đã được sử dụng bởi nhân viên {}.", request.phone(), c.code());
            throw new IllegalArgumentException(c.message("Số điện thoại"));
        });

        boolean success = employeeRepository.updateOwnProfile(request.employeeId(),
                request.fullName().trim(), request.phone().trim(), request.address().trim());
        if (!success) {
            log.error("Không tìm thấy nhân viên hoặc không thể cập nhật dữ liệu với ID: {}", request.employeeId());
            throw new IllegalStateException("Không tìm thấy nhân viên hoặc không thể cập nhật dữ liệu.");
        }

        log.info("Cập nhật thông tin thành công cho nhân viên ID: {}", request.employeeId());
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Thông tin tài khoản không hợp lệ.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không được để trống.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống.");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có tối thiểu 8 ký tự.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản trong hệ thống."));

        if (!PasswordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        String newPasswordHash = PasswordEncoder.hash(newPassword);
        boolean updated = accountRepository.updatePassword(account.getId(), newPasswordHash);
        if (!updated) {
            throw new IllegalStateException("Đổi mật khẩu thất bại. Vui lòng thử lại.");
        }

        account.setPasswordHash(newPasswordHash);
        account.setMustChangePassword(false);

        if (UserSession.isLoggedIn() && account.getId().equals(UserSession.getInstance().getAccount().getId())) {
            UserSession.getInstance().getAccount().setPasswordHash(newPasswordHash);
            UserSession.getInstance().getAccount().setMustChangePassword(false);
        }
        log.info("Đổi mật khẩu thành công cho tài khoản ID: {}", account.getId());
    }
}
