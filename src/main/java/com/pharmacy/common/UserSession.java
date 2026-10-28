package com.pharmacy.common;

import com.pharmacy.account.Account;
import com.pharmacy.employee.Employee;

public final class UserSession {

    private static volatile UserSession instance;

    private final Account account;

    private UserSession(Account account) {
        this.account = account;
    }

    public static synchronized void start(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account không được phép null khi khởi tạo session.");
        }
        instance = new UserSession(account);
    }

    public static UserSession getInstance() {
        return instance;
    }

    public static boolean isLoggedIn() {
        UserSession current = instance;
        return current != null && current.account != null;
    }

    public static void requireAuthenticated() {
        if (!isLoggedIn()) {
            throw new SecurityException("Phiên làm việc không tồn tại hoặc đã hết hạn. Yêu cầu đăng nhập.");
        }
    }

    /**
     * Kiểm tra quyền Quản lý ở tầng service (không chỉ dựa vào việc ẩn nút trên UI).
     */
    public static void requireManager() {
        requireAuthenticated();
        if (!instance.isManager()) {
            throw new SecurityException("Chỉ Quản lý mới được thực hiện thao tác này.");
        }
    }

    /**
     * ID nhân viên đang đăng nhập, hoặc null nếu chưa có phiên làm việc.
     */
    public static Integer currentEmployeeId() {
        UserSession current = instance;
        return current != null ? current.getEmployeeId() : null;
    }

    /**
     * Phạm vi dữ liệu theo nhân viên: Nhân viên (STAFF) chỉ được xem dữ liệu của chính mình,
     * bất kể tham số UI truyền vào; Quản lý giữ nguyên bộ lọc yêu cầu (null = toàn hệ thống).
     */
    public static Integer scopeEmployeeId(Integer requested) {
        UserSession current = instance;
        if (current != null && current.account != null && !current.isManager()) {
            return current.getEmployeeId();
        }
        return requested;
    }

    public static synchronized void invalidate() {
        instance = null;
    }

    public Account getAccount() {
        return account;
    }

    public String getEmployeeName() {
        if (account != null && account.getEmployee() != null) {
            return account.getEmployee().getFullName();
        }
        return "Nhân viên";
    }

    public String getUsername() {
        return account != null ? account.getUsername() : null;
    }

    public String getEmployeeCode() {
        if (account != null && account.getEmployee() != null) {
            return account.getEmployee().getCode();
        }
        return null;
    }

    public Integer getAccountId() {
        return account != null ? account.getId() : null;
    }

    public Integer getEmployeeId() {
        if (account != null && account.getEmployee() != null) {
            return account.getEmployee().getId();
        }
        return null;
    }

    public Employee getEmployee() {
        return account != null ? account.getEmployee() : null;
    }

    public boolean isManager() {
        return account != null && account.isManager();
    }

    public boolean isStaff() {
        return account != null && account.isStaff();
    }
}