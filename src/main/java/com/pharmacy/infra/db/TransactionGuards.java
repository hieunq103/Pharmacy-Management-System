package com.pharmacy.infra.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class TransactionGuards {

    private TransactionGuards() {
    }

    // Kiểm tra ngay trong giao dịch: nhân viên bị cho nghỉ/khóa tài khoản khi đang đăng nhập cũng không ghi được
    public static void requireActiveEmployee(Connection conn, Integer employeeId) throws SQLException {
        if (employeeId == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên thực hiện giao dịch.");
        }
        String sql = """
                SELECT e.status, a.status FROM employees e
                LEFT JOIN accounts a ON a.employee_id = e.id
                WHERE e.id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Nhân viên thực hiện giao dịch không tồn tại (ID: " + employeeId + ").");
                }
                if (!"ACTIVE".equals(rs.getString(1)) || !"ACTIVE".equals(rs.getString(2))) {
                    throw new IllegalStateException(
                            "Tài khoản của bạn đã bị khóa hoặc đã ngừng làm việc. Vui lòng liên hệ Quản lý cửa hàng.");
                }
            }
        }
    }
}
