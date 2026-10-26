package com.pharmacy.support;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.UserSession;
import com.pharmacy.infra.db.HibernateUtil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Hỗ trợ integration test cần MySQL thật (cấu hình qua DB_URL / DB_USER / DB_PASSWORD hoặc .env).
 * Khi không kết nối được CSDL, test được ĐÁNH DẤU BỎ QUA thay vì báo fail giả
 * (trước đây repository nuốt lỗi kết nối và trả về danh sách rỗng -> 15 test fail "rỗng").
 */
public final class DbTestSupport {

    /** Tên đăng nhập (mã NV) của Quản lý được seed trong V1__init_schema.sql */
    public static final String MANAGER_USERNAME = "PMS0001";
    /** Tên đăng nhập (mã NV) của Nhân viên bán hàng được seed trong V1__init_schema.sql */
    public static final String STAFF_USERNAME = "PMS0002";

    private static Boolean available;

    private DbTestSupport() {
    }

    public static synchronized void assumeDatabase() {
        if (available == null) {
            try {
                HibernateUtil.getSessionFactory();
                available = HibernateUtil.isDatabaseAlive();
            } catch (Exception e) {
                available = false;
            }
        }
        assumeTrue(available, "Bỏ qua integration test: không kết nối được MySQL");
    }

    public static Account loginAs(String username) {
        Account account = new AccountRepository().findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Thiếu tài khoản seed " + username));
        UserSession.start(account);
        return account;
    }

    public static Account loginAsManager() {
        return loginAs(MANAGER_USERNAME);
    }

    public static void logout() {
        UserSession.invalidate();
    }
}
