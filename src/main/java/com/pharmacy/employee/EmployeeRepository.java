package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.account.Role;
import com.pharmacy.infra.db.HibernateUtil;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pharmacy.common.PageResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmployeeRepository {

    private static final Logger log = LoggerFactory.getLogger(EmployeeRepository.class);

    /**
     * Kiểm tra mã nhân viên đã tồn tại hay chưa
     */
    public boolean existsByCode(String code) {
        return existsByField("code", code, null, "mã nhân viên");
    }

    /**
     * Tự động sinh mã nhân viên tiếp theo theo định dạng PMS + 4 chữ số (PMS0001, PMS0002...).
     */
    public String generateNextEmployeeCode() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String sql = "SELECT code FROM employees WHERE code LIKE 'PMS%' ORDER BY LENGTH(code) DESC, code DESC LIMIT 1";
            Object result = session.createNativeQuery(sql, String.class).uniqueResult();
            int nextNumber = 1;
            if (result != null) {
                String code = result.toString().trim();
                if (code.startsWith("PMS")) {
                    try {
                        String numPart = code.substring(3);
                        nextNumber = Integer.parseInt(numPart) + 1;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return String.format("PMS%04d", nextNumber);
        } catch (Exception e) {
            log.error("Lỗi khi sinh mã nhân viên tiếp theo: ", e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi sinh mã nhân viên.", e);
        }
    }

    /**
     * Dùng cho CẬP NHẬT: Kiểm tra SĐT có bị trùng với NGƯỜI KHÁC hay không
     */
    public boolean existsByPhoneAndNotId(String phone, Integer currentEmployeeId) {
        return existsByField("phone", phone, currentEmployeeId, "số điện thoại");
    }

    public boolean existsByPhone(String phone) {
        return existsByField("phone", phone, null, "số điện thoại");
    }

    public boolean existsByNationalId(String nationalId) {
        return existsByField("nationalId", nationalId, null, "CCCD");
    }

    /**
     * Dùng cho CẬP NHẬT: Kiểm tra CCCD có bị trùng với NGƯỜI KHÁC hay không
     */
    public boolean existsByNationalIdAndNotId(String nationalId, Integer employeeId) {
        if (employeeId == null) return false;
        return existsByField("nationalId", nationalId, employeeId, "CCCD");
    }

    /** Đếm nhân viên có field = value (bỏ qua nhân viên excludeId nếu có). Cột phone/national_id đều có UNIQUE index. */
    private boolean existsByField(String field, String value, Integer excludeId, String label) {
        if (value == null || value.isBlank()) return false;
        try {
            return HibernateUtil.executeWithResult(session -> {
                String hql = "SELECT COUNT(e.id) FROM Employee e WHERE e." + field + " = :value"
                        + (excludeId != null ? " AND e.id <> :id" : "");
                var query = session.createSelectionQuery(hql, Long.class).setParameter("value", value.trim());
                if (excludeId != null) query.setParameter("id", excludeId);
                Long count = query.getSingleResult();
                return count != null && count > 0;
            });
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trùng {} [{}]: ", label, value, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi kiểm tra " + label + ".", e);
        }
    }

    /** Cập nhật hồ sơ cá nhân: chỉ Họ tên, SĐT, địa chỉ. */
    public boolean updateOwnProfile(Integer employeeId, String fullName, String phone, String address) {
        try {
            return HibernateUtil.executeWithResult(session -> session.createMutationQuery("""
                        UPDATE Employee e SET e.fullName = :fullName, e.phone = :phone, e.address = :address
                        WHERE e.id = :id
                    """)
                    .setParameter("fullName", fullName)
                    .setParameter("phone", phone)
                    .setParameter("address", address)
                    .setParameter("id", employeeId)
                    .executeUpdate() > 0);
        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật hồ sơ cá nhân [ID: {}]: ", employeeId, e);
            throw e;
        }
    }

    public Optional<Employee> findByIdWithDetails(Integer id) {
        if (id == null)
            return Optional.empty();
        return HibernateUtil.executeWithResult(session -> {
            String hql = """
                        SELECT e FROM Employee e
                        LEFT JOIN FETCH e.account a
                        LEFT JOIN FETCH a.role r
                        WHERE e.id = :id
                    """;
            return session.createSelectionQuery(hql, Employee.class)
                    .setParameter("id", id)
                    .uniqueResultOptional();
        });
    }

    public List<Object[]> getLoginHistory(Integer accountId) {
        if (accountId == null)
            return List.of();
        return HibernateUtil.executeWithResult(session -> {
            String sql = """
                        SELECT login_at, result, client_info
                        FROM login_history
                        WHERE account_id = :accountId
                        ORDER BY login_at DESC
                        LIMIT 100
                    """;
            return session.createNativeQuery(sql, Object[].class)
                    .setParameter("accountId", accountId)
                    .list();
        });
    }

    public record EmployeeSummary(long totalEmployees, long activeEmployees) {
        public long total() {
            return totalEmployees;
        }

        public long active() {
            return activeEmployees;
        }
    }

    public EmployeeSummary getEmployeeSummary() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = """
                        SELECT
                            (SELECT COUNT(e.id) FROM Employee e),
                            (SELECT COUNT(e.id) FROM Employee e WHERE e.status = com.pharmacy.employee.Employee.Status.ACTIVE)
                    """;
            Object[] row = session.createSelectionQuery(hql, Object[].class).getSingleResult();
            long total = row[0] != null ? ((Number) row[0]).longValue() : 0L;
            long active = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            return new EmployeeSummary(total, active);
        } catch (Exception e) {
            log.error("Lỗi khi lấy thống kê nhân viên: ", e);
            return new EmployeeSummary(0L, 0L);
        }
    }

    /**
     * Phân trang nhân viên chuẩn Server-side: Tìm kiếm và lọc đa tiêu chí tại CSDL
     */
    public PageResult<Employee> getEmployeesPage(int page, int size, String keyword, String gender, String role,
            String status, String accountStatus) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
            Map<String, Object> params = new HashMap<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String cleanKw = keyword.trim();
                boolean isDigitsOnly = cleanKw.matches("^[0-9+]+$");
                if (isDigitsOnly) {
                    whereClause.append(" AND (e.phone LIKE :keyword OR e.nationalId LIKE :keyword OR e.code LIKE :keyword) ");
                } else {
                    whereClause.append(
                            " AND (e.fullName LIKE :keyword OR e.phone LIKE :keyword OR e.nationalId LIKE :keyword OR e.code LIKE :keyword) ");
                }
                params.put("keyword", "%" + cleanKw + "%");
            }

            if (gender != null && !gender.isBlank() && !"Tất cả".equalsIgnoreCase(gender)) {
                whereClause.append(" AND e.gender = :gender ");
                params.put("gender", gender.trim());
            }

            if (role != null && !role.isBlank() && !"Tất cả".equalsIgnoreCase(role)) {
                String roleCode = ("Quản lý".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) ? "MANAGER"
                        : "STAFF";
                whereClause.append(" AND r.code = :roleCode ");
                params.put("roleCode", roleCode);
            }

            if (status != null && !status.isBlank() && !"Tất cả".equalsIgnoreCase(status)) {
                Employee.Status empStatus = ("Đang làm".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status))
                        ? Employee.Status.ACTIVE
                        : Employee.Status.INACTIVE;
                whereClause.append(" AND e.status = :empStatus ");
                params.put("empStatus", empStatus);
            }

            if (accountStatus != null && !accountStatus.isBlank() && !"Tất cả".equalsIgnoreCase(accountStatus)) {
                Account.Status accStatus = "Active".equalsIgnoreCase(accountStatus) ? Account.Status.ACTIVE
                        : Account.Status.DISABLED;
                whereClause.append(" AND a.status = :accStatus ");
                params.put("accStatus", accStatus);
            }

            boolean needAccountJoin = (role != null && !role.isBlank() && !"Tất cả".equalsIgnoreCase(role))
                    || (accountStatus != null && !accountStatus.isBlank() && !"Tất cả".equalsIgnoreCase(accountStatus));

            // 1. Query COUNT
            String countHql = needAccountJoin
                    ? "SELECT COUNT(e.id) FROM Employee e LEFT JOIN e.account a LEFT JOIN a.role r "
                            + whereClause
                    : "SELECT COUNT(e.id) FROM Employee e " + whereClause;
            var countQuery = session.createSelectionQuery(countHql, Long.class);
            params.forEach(countQuery::setParameter);
            Long totalCount = countQuery.getSingleResult();
            long total = totalCount != null ? totalCount : 0L;

            if (total == 0) {
                return PageResult.empty(page, size);
            }

            // 2. Query DATA (phân trang bằng setFirstResult và setMaxResults)
            // Quan hệ to-one nên JOIN FETCH không nhân bản dòng -> không cần DISTINCT, LIMIT/OFFSET chạy ở SQL
            String dataHql = "SELECT e FROM Employee e LEFT JOIN FETCH e.account a LEFT JOIN FETCH a.role r "
                    + whereClause + " ORDER BY e.id DESC";
            var dataQuery = session.createSelectionQuery(dataHql, Employee.class);
            params.forEach(dataQuery::setParameter);
            dataQuery.setFirstResult((page - 1) * size);
            dataQuery.setMaxResults(size);

            List<Employee> items = dataQuery.getResultList();
            return new PageResult<>(items, total, page, size);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang nhân viên: ", e);
            return PageResult.empty(page, size);
        }
    }


    public boolean addEmployeeWithAccount(Employee emp, String roleDisplayOrCode, String initialPasswordHash) {
        String roleCode = roleDisplayOrCode;
        if ("Quản lý".equalsIgnoreCase(roleDisplayOrCode) || "MANAGER".equalsIgnoreCase(roleDisplayOrCode)) {
            roleCode = "MANAGER";
        } else if ("Nhân viên".equalsIgnoreCase(roleDisplayOrCode) || "STAFF".equalsIgnoreCase(roleDisplayOrCode)) {
            roleCode = "STAFF";
        }
        final String code = roleCode;
        try {
            return HibernateUtil.executeWithResult(session -> {
                Role role = session.createSelectionQuery("FROM Role r WHERE r.code = :code", Role.class)
                        .setParameter("code", code)
                        .uniqueResult();
                if (role == null) {
                    log.error("Không tìm thấy vai trò với mã: {}", code);
                    return false;
                }
                if (emp.getCode() == null || emp.getCode().isBlank()) {
                    emp.setCode(generateNextEmployeeCode());
                }
                session.persist(emp);

                Account account = new Account();
                account.setEmployee(emp);
                // Tên đăng nhập gắn cố định theo mã nhân viên (PMSxxxx)
                account.setUsername(emp.getCode());
                account.setRole(role);
                account.setPasswordHash(initialPasswordHash);
                account.setStatus(Account.Status.ACTIVE);
                // Tài khoản mới dùng mật khẩu khởi tạo -> bắt buộc đổi ở lần đăng nhập đầu tiên
                account.setMustChangePassword(true);
                session.persist(account);
                return true;
            });
        } catch (Exception e) {
            log.error("Lỗi khi thêm mới nhân viên và tài khoản: ", e);
            return false;
        }
    }

    public boolean markAsInactive(Integer employeeId) {
        try {
            HibernateUtil.execute(session -> {
                session.createMutationQuery("UPDATE Employee e SET e.status = :st WHERE e.id = :id")
                        .setParameter("st", Employee.Status.INACTIVE)
                        .setParameter("id", employeeId)
                        .executeUpdate();
                session.createMutationQuery("UPDATE Account a SET a.status = :st WHERE a.employee.id = :id")
                        .setParameter("st", Account.Status.DISABLED)
                        .setParameter("id", employeeId)
                        .executeUpdate();
            });
            return true;
        } catch (Exception e) {
            log.error("Lỗi khi đánh dấu nhân viên đã nghỉ [ID: {}]: ", employeeId, e);
            return false;
        }
    }

    public boolean updateEmployeeByManager(EmployeeUpdateRequest req) {
        try {
            return HibernateUtil.executeWithResult(session -> {
                int rowsUpdated = session.createMutationQuery("""
                            UPDATE Employee e SET
                                e.fullName = :fullName, e.dob = :dob, e.gender = :gender,
                                e.nationalId = :nationalId, e.phone = :phone, e.address = :address,
                                e.status = :status
                            WHERE e.id = :id
                        """)
                        .setParameter("fullName", req.fullName())
                        .setParameter("dob", req.dob())
                        .setParameter("gender", req.gender())
                        .setParameter("nationalId", req.nationalId())
                        .setParameter("phone", req.phone())
                        .setParameter("address", req.address())
                        .setParameter("status", Employee.Status.valueOf(req.empStatus()))
                        .setParameter("id", req.employeeId())
                        .executeUpdate();
                if (rowsUpdated == 0) {
                    return false;
                }

                // Tìm Role theo code hoặc tên hiển thị; không tìm thấy thì giữ nguyên role, chỉ đổi trạng thái
                Role role = session.createSelectionQuery("FROM Role r WHERE r.code = :roleVal OR r.name = :roleVal", Role.class)
                        .setParameter("roleVal", req.roleName())
                        .setMaxResults(1)
                        .uniqueResult();
                var accountUpdate = session.createMutationQuery(role != null
                                ? "UPDATE Account a SET a.status = :status, a.role.id = :roleId WHERE a.employee.id = :empId"
                                : "UPDATE Account a SET a.status = :status WHERE a.employee.id = :empId")
                        .setParameter("status", Account.Status.valueOf(req.accountStatus()))
                        .setParameter("empId", req.employeeId());
                if (role != null) {
                    accountUpdate.setParameter("roleId", role.getId());
                }
                accountUpdate.executeUpdate();
                return true;
            });
        } catch (RuntimeException e) {
            log.error("Lỗi cập nhật nhân viên bởi Manager [ID: {}]: ", req.employeeId(), e);
            throw e;
        }
    }
}
