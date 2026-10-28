package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.account.Role;
import com.pharmacy.infra.db.HibernateUtil;
import org.hibernate.Session;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pharmacy.common.PageResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EmployeeRepository {
    private static final Logger log = LoggerFactory.getLogger(EmployeeRepository.class);

    // Kiểm tra mã nhân viên đã tồn tại chưa
    public boolean existsByCode(String code) {
        return findConflict("code", code, null, "mã nhân viên").isPresent();
    }

    // Mã nhân viên dự kiến cho màn hình thêm mới (chỉ xem, không giữ chỗ); mã thật được cấp khi lưu
    public String generateNextEmployeeCode() {
        String prefix = employeeCodePrefix();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Number last = (Number) session.createNativeQuery(
                    "SELECT current_number FROM code_sequences WHERE name = :name", Object.class)
                    .setParameter("name", sequenceName(prefix)).uniqueResult();
            return formatEmployeeCode(prefix, (last != null ? last.longValue() : 0) + 1);
        } catch (Exception e) {
            log.error("Lỗi khi sinh mã nhân viên tiếp theo: ", e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi sinh mã nhân viên.", e);
        }
    }

    // Cấp mã thật trong giao dịch đang mở: khóa dòng bộ đếm của tháng đến khi commit nên hai người lưu cùng lúc xếp hàng
    private static String allocateEmployeeCode(Session session) {
        String prefix = employeeCodePrefix();
        session.createNativeMutationQuery(
                "INSERT INTO code_sequences (name, current_number) VALUES (:name, LAST_INSERT_ID(1)) "
                        + "ON DUPLICATE KEY UPDATE current_number = LAST_INSERT_ID(current_number + 1)")
                .setParameter("name", sequenceName(prefix))
                .executeUpdate();
        Number allocated = (Number) session.createNativeQuery("SELECT LAST_INSERT_ID()", Object.class).uniqueResult();
        return formatEmployeeCode(prefix, allocated.longValue());
    }

    // PMS + YYMM của tháng hiện tại, ví dụ PMS2610
    private static String employeeCodePrefix() {
        return "PMS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
    }

    private static String sequenceName(String prefix) {
        return "EMPLOYEE-" + prefix.substring(3);
    }

    // Số thứ tự trong tháng có tối thiểu 3 chữ số: PMS2610001
    private static String formatEmployeeCode(String prefix, long number) {
        return prefix + String.format("%03d", number);
    }

    // Nhân viên khác (kể cả đã nghỉ) đang dùng số điện thoại này
    public Optional<EmployeeConflict> findByPhone(String phone, Integer excludeId) {
        return findConflict("phone", phone, excludeId, "số điện thoại");
    }

    // Nhân viên khác (kể cả đã nghỉ) đang dùng CCCD này
    public Optional<EmployeeConflict> findByNationalId(String nationalId, Integer excludeId) {
        return findConflict("nationalId", nationalId, excludeId, "CCCD");
    }

    // Tìm nhân viên có trường dữ liệu trùng giá trị (bỏ qua excludeId)
    private Optional<EmployeeConflict> findConflict(String field, String value, Integer excludeId, String label) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return HibernateUtil.executeWithResult(session -> {
                String hql = "SELECT new com.pharmacy.employee.EmployeeConflict(e.code, e.fullName, e.status) "
                        + "FROM Employee e WHERE e." + field + " = :value"
                        + (excludeId != null ? " AND e.id <> :id" : "");
                var query = session.createSelectionQuery(hql, EmployeeConflict.class).setParameter("value", value.trim());
                if (excludeId != null) query.setParameter("id", excludeId);
                return query.setMaxResults(1).uniqueResultOptional();
            });
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trùng {} [{}]: ", label, value, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi kiểm tra " + label + ".", e);
        }
    }

    // Cập nhật thông tin hồ sơ cá nhân của nhân viên đang đăng nhập
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

    // Tìm nhân viên theo ID kèm thông tin tài khoản và vai trò
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

    // Lấy danh sách lịch sử đăng nhập của tài khoản
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

    // Thống kê tổng số nhân viên và số nhân viên đang hoạt động
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

    // Phân trang và lọc danh sách nhân viên theo nhiều tiêu chí
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

            List<String> genders = parseMultiFilter(gender);
            if (!genders.isEmpty()) {
                whereClause.append(" AND e.gender IN (:genders) ");
                params.put("genders", genders);
            }

            List<String> roleCodes = parseMultiFilter(role).stream()
                    .map(r -> ("Quản lý".equalsIgnoreCase(r) || "MANAGER".equalsIgnoreCase(r)) ? "MANAGER" : "STAFF")
                    .distinct()
                    .toList();
            if (!roleCodes.isEmpty()) {
                whereClause.append(" AND r.code IN (:roleCodes) ");
                params.put("roleCodes", roleCodes);
            }

            if (status != null && !status.isBlank() && !"Tất cả".equalsIgnoreCase(status)) {
                Employee.Status empStatus = ("Đang làm".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status))
                        ? Employee.Status.ACTIVE
                        : Employee.Status.INACTIVE;
                whereClause.append(" AND e.status = :empStatus ");
                params.put("empStatus", empStatus);
            }

            List<Account.Status> accStatuses = parseMultiFilter(accountStatus).stream()
                    .map(a -> ("Hoạt động".equalsIgnoreCase(a) || "Active".equalsIgnoreCase(a)) ? Account.Status.ACTIVE : Account.Status.DISABLED)
                    .distinct()
                    .toList();
            if (!accStatuses.isEmpty()) {
                whereClause.append(" AND a.status IN (:accStatuses) ");
                params.put("accStatuses", accStatuses);
            }

            boolean needAccountJoin = !roleCodes.isEmpty() || !accStatuses.isEmpty();

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

    // Chuyển chuỗi lọc nhiều giá trị phân tách bởi dấu phẩy thành danh sách
    private static List<String> parseMultiFilter(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(v -> !v.isEmpty() && !"Tất cả".equalsIgnoreCase(v))
                .distinct()
                .toList();
    }

    // Thêm mới nhân viên đồng thời tạo tài khoản đăng nhập
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
                    emp.setCode(allocateEmployeeCode(session));
                }
                session.persist(emp);

                Account account = new Account();
                account.setEmployee(emp);

                account.setUsername(emp.getCode());
                account.setRole(role);
                account.setPasswordHash(initialPasswordHash);
                account.setStatus(Account.Status.ACTIVE);

                account.setMustChangePassword(true);
                session.persist(account);
                return true;
            });
        } catch (Exception e) {
            log.error("Lỗi khi thêm mới nhân viên và tài khoản: ", e);
            return false;
        }
    }

    // Đánh dấu nhân viên đã nghỉ việc và vô hiệu hóa tài khoản
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

    // Cập nhật thông tin nhân viên, trạng thái và vai trò tài khoản bởi Quản lý
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
