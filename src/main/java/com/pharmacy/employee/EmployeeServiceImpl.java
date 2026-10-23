package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.UserSession;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pharmacy.common.PageResult;
import java.util.List;
import java.util.Optional;

public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository = new AccountRepository();

    public EmployeeServiceImpl() {
        this(new EmployeeRepository());
    }

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public PageResult<Employee> getEmployeesPage(int page, int size, String keyword, String gender, String role, String status, String accountStatus) {
        if (!UserSession.isLoggedIn()) {
            log.warn("Từ chối truy cập: Chưa đăng nhập vào hệ thống.");
            return PageResult.empty(page, size);
        }

        Account currentAccount = UserSession.getInstance().getAccount();
        if (currentAccount == null || !currentAccount.isManager()) {
            log.warn("Từ chối truy cập: Tài khoản ID [{}] không có quyền Quản lý để xem danh sách nhân sự.",
                    currentAccount != null ? currentAccount.getId() : "null");
            throw new SecurityException("Bạn không có quyền truy cập danh sách nhân viên.");
        }

        return employeeRepository.getEmployeesPage(page, size, keyword, gender, role, status, accountStatus);
    }

    @Override
    public EmployeeRepository.EmployeeSummary getEmployeeSummary() {
        if (!UserSession.isLoggedIn()) {
            return new EmployeeRepository.EmployeeSummary(0L, 0L);
        }
        return employeeRepository.getEmployeeSummary();
    }


    @Override
    public Optional<Employee> findByIdWithDetails(Integer id) {
        if (id == null || id <= 0) {
            log.warn("Yêu cầu tìm kiếm thất bại: ID không hợp lệ ({})", id);
            return Optional.empty();
        }

        return employeeRepository.findByIdWithDetails(id);
    }

    @Override
    public boolean createEmployeeWithAccount(Employee employee, String role) {
        if (employee == null || role == null || role.trim().isEmpty()) {
            return false;
        }
        if (employeeRepository.existsByNationalId(employee.getNationalId())) {
            throw new IllegalArgumentException("Căn cước công dân này đã tồn tại trong hệ thống.");
        }
        if (employeeRepository.existsByPhone(employee.getPhone())) {
            throw new IllegalArgumentException("Số điện thoại này đã được sử dụng trong hệ thống.");
        }
        String defaultInitialPassword = "12345678";
        String hashedPassword = BCrypt.hashpw(defaultInitialPassword, BCrypt.gensalt(10));
        return employeeRepository.addEmployeeWithAccount(employee, role.trim(), hashedPassword);
    }

    @Override
    public boolean markAsInactive(Integer employeeId) {
        if (employeeId == null) return false;
        return employeeRepository.markAsInactive(employeeId);
    }

    @Override
    public boolean lockAccount(Integer accountId) {
        if (accountId == null) {
            log.warn("Yêu cầu khóa tài khoản thất bại: accountId null.");
            return false;
        }
        return accountRepository.lockAccount(accountId);
    }

    @Override
    public boolean unlockAccount(Integer accountId) {
        if (accountId == null) {
            log.warn("Yêu cầu mở khóa tài khoản thất bại: accountId null.");
            return false;
        }
        return accountRepository.unlockAccount(accountId);
    }

    @Override
    public List<Object[]> getLoginHistory(Integer accountId) {
        return employeeRepository.getLoginHistory(accountId);
    }

    @Override
    public boolean updateEmployeeByManager(EmployeeUpdateRequest req) {
        if (req == null || req.employeeId() == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật nhân viên không hợp lệ.");
        }

        // 1. Chuẩn hóa trạng thái Employee: sang "ACTIVE" hoặc "INACTIVE"
        String normalizedEmpStatus = parseEmployeeStatus(req.empStatus());

        // 2. Chuẩn hóa trạng thái Account: sang "ACTIVE" hoặc "DISABLED"
        String normalizedAccStatus = parseAccountStatus(req.accountStatus());

        // 3. Chuẩn hóa Role: sang code hệ thống (ví dụ: MANAGER, STAFF)
        String normalizedRole = parseRoleCode(req.roleName());

        // 4. Đóng gói dữ liệu đã được làm sạch
        EmployeeUpdateRequest sanitizedRequest = new EmployeeUpdateRequest(
                req.employeeId(),
                req.fullName() != null ? req.fullName().trim() : "",
                req.nationalId() != null ? req.nationalId().trim() : "",
                req.phone() != null ? req.phone().trim() : "",
                req.address() != null ? req.address().trim() : "",
                req.dob(),
                req.gender(),
                normalizedEmpStatus,
                normalizedAccStatus,
                normalizedRole
        );

        // 5. DB Validation: Kiểm tra trùng lặp CCCD và Số điện thoại với nhân viên khác trong CSDL
        if (employeeRepository.existsByNationalIdAndNotId(sanitizedRequest.nationalId(), sanitizedRequest.employeeId())) {
            throw new IllegalArgumentException("Căn cước công dân này đã thuộc về nhân viên khác.");
        }
        if (employeeRepository.existsByPhoneAndNotId(sanitizedRequest.phone(), sanitizedRequest.employeeId())) {
            throw new IllegalArgumentException("Số điện thoại này đã thuộc về nhân viên khác.");
        }

        // 6. Gọi repository thực thi Hibernate Transaction
        return employeeRepository.updateEmployeeByManager(sanitizedRequest);
    }

    private String parseEmployeeStatus(String status) {
        if (status == null) return "ACTIVE";
        String trimmed = status.trim().toUpperCase();
        if (trimmed.contains("NGHỈ") || trimmed.contains("INACTIVE")) {
            return "INACTIVE";
        }
        return "ACTIVE";
    }

    private String parseAccountStatus(String status) {
        if (status == null) return "ACTIVE";
        String trimmed = status.trim().toUpperCase();
        if (trimmed.contains("KHÓA") || trimmed.contains("DISABLED") || trimmed.contains("LOCKED")) {
            return "DISABLED";
        }
        return "ACTIVE";
    }

    private String parseRoleCode(String role) {
        if (role == null) return "STAFF";
        String trimmed = role.trim();
        if (trimmed.contains("Quản lý") || trimmed.equalsIgnoreCase("MANAGER")) {
            return "MANAGER";
        }
        if (trimmed.contains("Nhân viên") || trimmed.equalsIgnoreCase("STAFF")) {
            return "STAFF";
        }
        return trimmed;
    }

}