package com.pharmacy.profile;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.employee.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository = new AccountRepository();

    public ProfileServiceImpl() {
        this.employeeRepository = new EmployeeRepository();
    }

    public ProfileServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void updateProfile(ProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Yêu cầu cập nhật không được để trống.");
        }

        log.info("Bắt đầu cập nhật hồ sơ cho nhân viên ID: {}", request.employeeId());

        boolean isPhoneTakenByOthers = employeeRepository.existsByPhoneAndNotId(request.phone(), request.employeeId());
        if (isPhoneTakenByOthers) {
            log.warn("Cập nhật thất bại: Số điện thoại {} đã được sử dụng bởi nhân viên khác.", request.phone());
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi nhân viên khác.");
        }

        boolean success = employeeRepository.updateProfile(request);
        if (!success) {
            log.error("Không tìm thấy nhân viên hoặc không thể cập nhật dữ liệu với ID: {}", request.employeeId());
            throw new IllegalStateException("Không tìm thấy nhân viên hoặc không thể cập nhật dữ liệu.");
        }

        log.info("Cập nhật thông tin thành công cho nhân viên ID: {}", request.employeeId());
    }

    @Override
    public void changePassword(String identifier, String currentPassword, String newPassword) {
        if (identifier == null || identifier.isBlank()) {
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

        Account account = accountRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản trong hệ thống."));

        if (!org.mindrot.jbcrypt.BCrypt.checkpw(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        String newPasswordHash = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt(10));

        boolean updated = accountRepository.updatePassword(account.getId(), newPasswordHash);
        if (!updated) {
            throw new IllegalStateException("Đổi mật khẩu thất bại. Vui lòng thử lại.");
        }

        account.setPasswordHash(newPasswordHash);
        log.info("Đổi mật khẩu thành công cho tài khoản ID: {}", account.getId());
    }
}