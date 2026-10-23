package com.pharmacy.profile;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.FormValidator;
import com.pharmacy.common.UserSession;
import com.pharmacy.employee.EmployeeRepository;
import org.mindrot.jbcrypt.BCrypt;
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

        boolean isManager = UserSession.isLoggedIn() && UserSession.getInstance().isManager();

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

        if (isManager) {
            if (request.fullName() == null || request.fullName().isBlank()) {
                throw new IllegalArgumentException("Họ và tên không được để trống.");
            }
            if (request.nationalId() == null || !FormValidator.isValidNationalId(request.nationalId())) {
                throw new IllegalArgumentException("Căn cước công dân phải đúng 12 chữ số.");
            }
            if (request.dob() == null) {
                throw new IllegalArgumentException("Ngày sinh không được để trống.");
            }
            if (request.dob().isAfter(java.time.LocalDate.now().minusYears(18))) {
                throw new IllegalArgumentException("Nhân viên phải từ đủ 18 tuổi trở lên.");
            }
        }

        log.info("Bắt đầu cập nhật hồ sơ cho nhân viên ID: {} (Vai trò: {})",
                request.employeeId(), isManager ? "Manager" : "Staff");

        boolean isPhoneTakenByOthers = employeeRepository.existsByPhoneAndNotId(request.phone(), request.employeeId());
        if (isPhoneTakenByOthers) {
            log.warn("Cập nhật thất bại: Số điện thoại {} đã được sử dụng bởi nhân viên khác.", request.phone());
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng bởi nhân viên khác.");
        }

        boolean success;

        if (isManager) {
            boolean isNationalIdTaken = employeeRepository.existsByNationalIdAndNotId(request.nationalId(), request.employeeId());
            if (isNationalIdTaken) {
                log.warn("Cập nhật thất bại: CCCD {} đã thuộc về nhân viên khác.", request.nationalId());
                throw new IllegalArgumentException("Căn cước công dân này đã thuộc về nhân viên khác.");
            }

            success = employeeRepository.updateManagerProfile(request);
        } else {
            success = employeeRepository.updateStaffProfile(request.employeeId(), request.phone(), request.address());
        }

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

        if (!BCrypt.checkpw(currentPassword, account.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }

        String newPasswordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        boolean updated = accountRepository.updatePassword(account.getId(), newPasswordHash);
        if (!updated) {
            throw new IllegalStateException("Đổi mật khẩu thất bại. Vui lòng thử lại.");
        }

        account.setPasswordHash(newPasswordHash);
        log.info("Đổi mật khẩu thành công cho tài khoản ID: {}", account.getId());
    }
}