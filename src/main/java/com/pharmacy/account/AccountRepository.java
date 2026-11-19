package com.pharmacy.account;

import com.pharmacy.infra.db.HibernateUtil;
import java.util.List;
import java.util.Optional;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class AccountRepository {

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

    // Ghi nhận lịch sử đăng nhập thành công
    public void recordSuccessfulLogin(Integer accountId, String clientInfo) {
        saveLoginHistory(accountId, "SUCCESS", clientInfo);
    }

    // Lưu lịch sử đăng nhập vào cơ sở dữ liệu
    public void saveLoginHistory(Integer accountId, String result, String clientInfo) {
        HibernateUtil.execute(session -> saveLoginHistoryInternal(session, accountId, result, clientInfo));
    }

    // Thực thi câu lệnh insert bản ghi lịch sử đăng nhập
    private void saveLoginHistoryInternal(Session session, Integer accountId, String result, String clientInfo) {
        Account accountRef = session.getReference(Account.class, accountId);
        session.persist(new LoginHistory(accountRef, LoginHistory.Result.valueOf(result), clientInfo));
    }

    // Lấy danh sách lịch sử đăng nhập gần nhất của tài khoản
    public List<Object[]> getLoginHistory(Integer accountId) {
        if (accountId == null) {
            return List.of();
        }
        try (Session session = HibernateUtil.openReadOnlySession()) {
            String hql = """
                SELECT lh.loginAt, lh.result, lh.clientInfo
                FROM LoginHistory lh
                WHERE lh.account.id = :accountId
                ORDER BY lh.loginAt DESC
            """;
            return session
                .createSelectionQuery(hql, Object[].class)
                .setParameter("accountId", accountId)
                .setMaxResults(100)
                .list();
        }
    }

    // Cập nhật mật khẩu mới và tắt cờ bắt buộc đổi mật khẩu
    public boolean updatePassword(Integer accountId, String newPasswordHash) {
        return HibernateUtil.executeWithResult(session -> {
            String hql =
                "UPDATE Account a SET a.passwordHash = :passwordHash, a.mustChangePassword = false WHERE a.id = :id";
            int rowsUpdated = session
                .createMutationQuery(hql)
                .setParameter("passwordHash", newPasswordHash)
                .setParameter("id", accountId)
                .executeUpdate();
            return rowsUpdated > 0;
        });
    }

    // Cấp lại mật khẩu tạm: thay bản băm và bật lại cờ bắt buộc đổi mật khẩu
    public boolean resetPassword(Integer accountId, String newPasswordHash) {
        return HibernateUtil.executeWithResult(
            session ->
                session
                    .createMutationQuery(
                        "UPDATE Account a SET a.passwordHash = :passwordHash, a.mustChangePassword = true WHERE a.id = :id"
                    )
                    .setParameter("passwordHash", newPasswordHash)
                    .setParameter("id", accountId)
                    .executeUpdate() > 0
        );
    }

    // Khóa tài khoản nhân viên
    public boolean lockAccount(Integer accountId) {
        if (accountId == null) return false;
        return HibernateUtil.executeWithResult(session -> {
            String hql = "UPDATE Account a SET a.status = 'DISABLED' WHERE a.id = :id";
            int rowsUpdated = session.createMutationQuery(hql).setParameter("id", accountId).executeUpdate();
            return rowsUpdated > 0;
        });
    }

    // Mở khóa tài khoản nhân viên
    public boolean unlockAccount(Integer accountId) {
        if (accountId == null) return false;
        return HibernateUtil.executeWithResult(session -> {
            String hql = "UPDATE Account a SET a.status = 'ACTIVE' WHERE a.id = :id";
            int rowsUpdated = session.createMutationQuery(hql).setParameter("id", accountId).executeUpdate();
            return rowsUpdated > 0;
        });
    }
}
