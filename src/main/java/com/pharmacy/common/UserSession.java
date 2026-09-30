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

    // ==================== CÁC HÀM TIỆN ÍCH HIỂN THỊ GIAO DIỆN ====================

    /**
     * Lấy họ tên nhân viên hiển thị lên thanh tiêu đề / Topbar.
     */
    public String getEmployeeName() {
        if (account != null && account.getEmployee() != null) {
            return account.getEmployee().getFullName();
        }
        return "Nhân viên";
    }

    /**
     * Lấy ID tài khoản (Integer)
     */
    public Integer getAccountId() {
        return account != null ? account.getId() : null;
    }

    /**
     * Lấy ID nhân viên phục vụ ghi nhận hóa đơn / chứng từ kho
     */
    public Integer getEmployeeId() {
        if (account != null && account.getEmployee() != null) {
            return account.getEmployee().getId();
        }
        return null;
    }

    /**
     * Lấy thông tin Employee đầy đủ
     */
    public Employee getEmployee() {
        return account != null ? account.getEmployee() : null;
    }

    // ==================== HÀM TIỆN ÍCH PHÂN QUYỀN & TRẠNG THÁI ====================

    public boolean isManager() {
        return account != null && account.isManager();
    }

    public boolean isStaff() {
        return account != null && account.isStaff();
    }

    public boolean mustChangePassword() {
        return account != null && account.isMustChangePassword();
    }
}