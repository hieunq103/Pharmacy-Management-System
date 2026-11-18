package com.pharmacy.infra.db;

import com.pharmacy.account.Account;
import com.pharmacy.employee.Employee;
import org.hibernate.Session;

public final class TransactionGuards {

    private TransactionGuards() {
    }

    // Kiểm tra ngay trong giao dịch: nhân viên bị cho nghỉ/khóa tài khoản khi đang đăng nhập cũng không ghi được
    public static void requireActiveEmployee(Session session, Integer employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên thực hiện giao dịch.");
        }
        String hql = """
                SELECT e.status, a.status
                FROM Employee e
                LEFT JOIN e.account a
                WHERE e.id = :id
                """;
        Object[] row = session.createSelectionQuery(hql, Object[].class)
                .setParameter("id", employeeId)
                .uniqueResult();
        if (row == null) {
            throw new IllegalArgumentException("Nhân viên thực hiện giao dịch không tồn tại (ID: " + employeeId + ").");
        }
        if (row[0] != Employee.Status.ACTIVE || row[1] != Account.Status.ACTIVE) {
            throw new IllegalStateException(
                    "Tài khoản của bạn đã bị khóa hoặc đã ngừng làm việc. Vui lòng liên hệ Quản lý cửa hàng.");
        }
    }
}
