package com.pharmacy.account;

import com.pharmacy.infra.db.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Optional;

public class AccountRepository {

    /**
     * Tìm kiếm tài khoản duy nhất bằng tên đăng nhập (Mã nhân viên PMSxxxx).
     */
    public Optional<Account> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return HibernateUtil.executeWithResult(session -> {
            String hql = """
            SELECT a FROM Account a
            JOIN FETCH a.employee e
            JOIN FETCH a.role r
            WHERE a.username = :username
        """;
            Query<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("username", username.trim());
            return query.uniqueResultOptional();
        });
    }

    /**
     * Ghi nhận lịch sử đăng nhập thành công.
     */
    public void recordSuccessfulLogin(Integer accountId, String clientInfo) {
        saveLoginHistory(accountId, "SUCCESS", clientInfo);
    }

    public void saveLoginHistory(Integer accountId, String result, String clientInfo) {
        HibernateUtil.execute(session -> saveLoginHistoryInternal(session, accountId, result, clientInfo));
    }

    private void saveLoginHistoryInternal(Session session, Integer accountId, String result, String clientInfo) {
        String sql = """
        INSERT INTO login_history (account_id, login_at, result, client_info)
        VALUES (:accountId, NOW(), :result, :clientInfo)
    """;
        session.createNativeMutationQuery(sql)
                .setParameter("accountId", accountId)
                .setParameter("result", result)
                .setParameter("clientInfo", clientInfo)
                .executeUpdate();
    }

    public boolean updatePassword(Integer accountId, String newPasswordHash) {
        return HibernateUtil.executeWithResult(session -> {
            // Đổi mật khẩu thành công thì gỡ cờ bắt buộc đổi mật khẩu lần đầu
            String hql = "UPDATE Account a SET a.passwordHash = :passwordHash, a.mustChangePassword = false WHERE a.id = :id";
            int rowsUpdated = session.createMutationQuery(hql)
                    .setParameter("passwordHash", newPasswordHash)
                    .setParameter("id", accountId)
                    .executeUpdate();
            return rowsUpdated > 0;
        });
    }

    public boolean lockAccount(Integer accountId) {
        if (accountId == null) return false;
        return HibernateUtil.executeWithResult(session -> {
            String hql = "UPDATE Account a SET a.status = 'DISABLED' WHERE a.id = :id";
            int rowsUpdated = session.createMutationQuery(hql)
                    .setParameter("id", accountId)
                    .executeUpdate();
            return rowsUpdated > 0;
        });
    }

    public boolean unlockAccount(Integer accountId) {
        if (accountId == null) return false;
        return HibernateUtil.executeWithResult(session -> {
            String hql = "UPDATE Account a SET a.status = 'ACTIVE' WHERE a.id = :id";
            int rowsUpdated = session.createMutationQuery(hql)
                    .setParameter("id", accountId)
                    .executeUpdate();
            return rowsUpdated > 0;
        });
    }
}