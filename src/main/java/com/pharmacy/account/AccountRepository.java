package com.pharmacy.account;

import com.pharmacy.infra.db.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.Optional;

public class AccountRepository {

    public Optional<Account> findByIdentifier(String identifier) {
        return HibernateUtil.executeWithResult(session -> {
            String hql = """
            SELECT a FROM Account a
            JOIN FETCH a.employee e
            JOIN FETCH a.role r
            WHERE e.phone = :identifier
               OR e.nationalId = :identifier
        """;
            Query<Account> query = session.createQuery(hql, Account.class);
            query.setParameter("identifier", identifier);
            return query.uniqueResultOptional();
        });
    }

    /**
     * Ghi nhận lịch sử đăng nhập thành công.
     */
    public void recordSuccessfulLogin(Integer accountId, String clientInfo) {
        saveLoginHistory(accountId, "SUCCESS", clientInfo);
    }

    /**
     * Ghi lịch sử đăng nhập (dùng cho FAILED hoặc SUCCESS nếu cần theo dõi).
     */
    public void saveLoginHistory(Integer accountId, String result, String clientInfo) {
        HibernateUtil.execute(session -> saveLoginHistoryInternal(session, accountId, result, clientInfo));
    }

    /**
     * Hàm nội bộ ghi lịch sử đăng nhập vào bảng login_history.
     */
    private void saveLoginHistoryInternal(Session session, Integer accountId, String result, String clientInfo) {
        String sql = """
            INSERT INTO login_history (account_id, login_at, result, client_info)
            VALUES (:accountId, NOW(), :result, :clientInfo)
        """;
        session.createNativeQuery(sql, Void.class)
                .setParameter("accountId", accountId)
                .setParameter("result", result)
                .setParameter("clientInfo", clientInfo)
                .executeUpdate();
    }

    /**
     * Cập nhật mật khẩu băm mới cho tài khoản theo ID.
     */
    public boolean updatePassword(Integer accountId, String newPasswordHash) {
        return HibernateUtil.executeWithResult(session -> {
            String hql = "UPDATE Account a SET a.passwordHash = :passwordHash WHERE a.id = :id";
            int rowsUpdated = session.createMutationQuery(hql)
                    .setParameter("passwordHash", newPasswordHash)
                    .setParameter("id", accountId)
                    .executeUpdate();
            return rowsUpdated > 0;
        });
    }
}