package com.pharmacy.common;

import com.pharmacy.account.Account;
import com.pharmacy.employee.Employee;
import java.time.LocalDateTime;

public final class UserSession {

    private static volatile UserSession instance;

    private final Account account;
    private final LocalDateTime loginTime;

    private UserSession(Account account) {
        this.account = account;
        this.loginTime = LocalDateTime.now();
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

    public static boolean isAuthenticated() {
        return isLoggedIn();
    }

    public static void requireAuthenticated() {
        if (!isLoggedIn()) {
            throw new SecurityException("Phiên làm việc không tồn tại hoặc đã hết hạn. Yêu cầu đăng nhập.");
        }
    }

    public static synchronized void invalidate() {
        instance = null;
    }

    public Account getAccount() {
        return account;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public String getEmployeeName() {
        if (account != null && account.getEmployee() != null) {
            return account.getEmployee().getFullName();
        }
        return "Nhân viên";
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

    public boolean mustChangePassword() {
        return account != null && account.isMustChangePassword();
    }
}