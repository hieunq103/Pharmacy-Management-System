package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.UserSession;
import com.pharmacy.auth.AuthServiceImpl;
import com.pharmacy.common.FormValidator;
import com.pharmacy.infra.security.PasswordEncoder;
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
        requireManager();
        return employeeRepository.getEmployeesPage(page, size, keyword, gender, role, status, accountStatus);
    }

    @Override
    public EmployeeRepository.EmployeeSummary getEmployeeSummary() {
        requireManager();
        return employeeRepository.getEmployeeSummary();
    }

    @Override
    public Optional<Employee> findByIdWithDetails(Integer id) {
        if (id == null || id <= 0) {
            log.warn("Yêu cầu tìm kiếm thất bại: ID không hợp lệ ({})", id);
            return Optional.empty();
        }
        requireManager();
        return employeeRepository.findByIdWithDetails(id);
    }

    @Override
    public String generateNextEmployeeCode() {
        return employeeRepository.generateNextEmployeeCode();
    }

    @Override
    public boolean createEmployeeWithAccount(Employee employee, String role) {
        requireManager();
        if (employee == null || role == null || role.trim().isEmpty()) {
            return false;
        }
        validateEmployee(employee.getFullName(), employee.getPhone(), employee.getNationalId(), employee.getDob());
        if (employee.getCode() == null || employee.getCode().isBlank()) {
            employee.setCode(generateNextEmployeeCode());
        }
        if (employeeRepository.existsByCode(employee.getCode())) {
            throw new IllegalArgumentException("Mã nhân viên này đã tồn tại trong hệ thống.");
        }
        employeeRepository.findByNationalId(employee.getNationalId(), null).ifPresent(c -> {
            throw new IllegalArgumentException(c.message("Căn cước công dân"));
        });
        employeeRepository.findByPhone(employee.getPhone(), null).ifPresent(c -> {
            throw new IllegalArgumentException(c.message("Số điện thoại"));
        });

        String hashedPassword = PasswordEncoder.hash(AuthServiceImpl.defaultPassword());
        return employeeRepository.addEmployeeWithAccount(employee, role.trim(), hashedPassword);
    }

    @Override
    public boolean markAsInactive(Integer employeeId) {
        if (employeeId == null) return false;
        requireManager();
        if (employeeId.equals(UserSession.currentEmployeeId())) {
            throw new IllegalArgumentException("Quản lý không thể tự đánh dấu nghỉ việc cho chính mình.");
        }
        return employeeRepository.markAsInactive(employeeId);
    }

    @Override
    public boolean lockAccount(Integer accountId) {
        if (accountId == null) {
            log.warn("Yêu cầu khóa tài khoản thất bại: accountId null.");
            return false;
        }
        requireManager();
        if (accountId.equals(UserSession.getInstance().getAccountId())) {
            throw new IllegalArgumentException("Quản lý không thể tự khóa tài khoản của chính mình.");
        }
        return accountRepository.lockAccount(accountId);
    }

    @Override
    public boolean unlockAccount(Integer accountId) {
        if (accountId == null) {
            log.warn("Yêu cầu mở khóa tài khoản thất bại: accountId null.");
            return false;
        }
        requireManager();
        return accountRepository.unlockAccount(accountId);
    }

    @Override
    public List<Object[]> getLoginHistory(Integer accountId) {
        requireManager();
        return employeeRepository.getLoginHistory(accountId);
    }

    @Override
    public boolean updateEmployeeByManager(EmployeeUpdateRequest req) {
        if (req == null || req.employeeId() == null) {
            throw new IllegalArgumentException("Dữ liệu cập nhật nhân viên không hợp lệ.");
        }
        requireManager();
        validateEmployee(req.fullName(), req.phone(), req.nationalId(), req.dob());

        String normalizedEmpStatus = parseEmployeeStatus(req.empStatus());

        String normalizedAccStatus = parseAccountStatus(req.accountStatus());

        String normalizedRole = parseRoleCode(req.roleName());

        if (UserSession.isLoggedIn()) {
            Integer currentEmpId = UserSession.getInstance().getEmployeeId();
            if (currentEmpId != null && currentEmpId.equals(req.employeeId())) {
                Account currentAcc = UserSession.getInstance().getAccount();
                if (currentAcc != null && currentAcc.getRole() != null) {
                    String currentRoleCode = currentAcc.getRole().getCode();
                    if (currentRoleCode != null && !currentRoleCode.equalsIgnoreCase(normalizedRole)) {
                        throw new IllegalArgumentException("Quản lý không thể tự thay đổi vai trò hệ thống của chính mình.");
                    }
                }

                if ("INACTIVE".equalsIgnoreCase(normalizedEmpStatus)) {
                    throw new IllegalArgumentException("Quản lý không thể tự đánh dấu nghỉ việc cho chính mình.");
                }

                if ("DISABLED".equalsIgnoreCase(normalizedAccStatus)) {
                    throw new IllegalArgumentException("Quản lý không thể tự khóa tài khoản của chính mình.");
                }

                Employee self = UserSession.getInstance().getEmployee();
                if (self != null && (changed(self.getNationalId(), req.nationalId() != null ? req.nationalId().trim() : null)
                        || changed(self.getDob(), req.dob())
                        || changed(self.getGender(), req.gender()))) {
                    throw new IllegalArgumentException(
                            "Với hồ sơ của chính mình, chỉ có thể thay đổi Họ và tên, Số điện thoại và Địa chỉ.");
                }
            }
        }

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

        employeeRepository.findByNationalId(sanitizedRequest.nationalId(), sanitizedRequest.employeeId()).ifPresent(c -> {
            throw new IllegalArgumentException(c.message("Căn cước công dân"));
        });
        employeeRepository.findByPhone(sanitizedRequest.phone(), sanitizedRequest.employeeId()).ifPresent(c -> {
            throw new IllegalArgumentException(c.message("Số điện thoại"));
        });

        return employeeRepository.updateEmployeeByManager(sanitizedRequest);
    }

    private static boolean changed(Object current, Object requested) {
        return current != null && !current.equals(requested);
    }

    private static void requireManager() {
        try {
            UserSession.requireManager();
        } catch (SecurityException e) {
            Account current = UserSession.isLoggedIn() ? UserSession.getInstance().getAccount() : null;
            log.warn("Từ chối truy cập module nhân sự: tài khoản ID [{}] không có quyền Quản lý.",
                    current != null ? current.getId() : "null");
            throw e;
        }
    }

    static void validateEmployee(String fullName, String phone, String nationalId, java.time.LocalDate dob) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên nhân viên không được để trống.");
        }
        if (!FormValidator.isValidPhone(phone)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ.");
        }
        if (!FormValidator.isValidNationalId(nationalId)) {
            throw new IllegalArgumentException("Số CCCD phải gồm đúng 12 chữ số.");
        }
        if (dob == null || dob.isAfter(java.time.LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ (nhân viên phải từ đủ 18 tuổi trở lên).");
        }
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
