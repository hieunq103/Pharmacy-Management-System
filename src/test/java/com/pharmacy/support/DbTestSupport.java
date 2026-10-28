package com.pharmacy.support;

import com.pharmacy.account.Account;
import com.pharmacy.account.AccountRepository;
import com.pharmacy.common.UserSession;
import com.pharmacy.infra.db.HibernateUtil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public final class DbTestSupport {
    public static final String MANAGER_USERNAME = "PMS0001";

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
