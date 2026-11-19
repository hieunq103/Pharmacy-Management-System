package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.account.Role;
import com.pharmacy.common.PageResult;
import com.pharmacy.common.SearchFilter;
import com.pharmacy.employee.dto.EmployeeConflict;
import com.pharmacy.employee.dto.EmployeeSummary;
import com.pharmacy.employee.dto.EmployeeUpdateRequest;
import com.pharmacy.infra.db.CodeSequence;
import com.pharmacy.infra.db.HibernateUtil;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmployeeRepository {

    private static final Logger log = LoggerFactory.getLogger(EmployeeRepository.class);
    private static final DateTimeFormatter YYMM = DateTimeFormatter.ofPattern("yyMM");

    public boolean existsByCode(String code) {
        return findConflict("code", code, null, "mã nhân viên").isPresent();
    }

    private static void ensureEmployeeSequence(String prefix) {
        String seqName = sequenceName(prefix);
        try {
            HibernateUtil.execute(session -> {
                if (session.find(CodeSequence.class, seqName) == null) {
                    session.persist(new CodeSequence(seqName, 0L));
                }
            });
        } catch (Exception ignored) {
            // Đã có luồng khác tạo dòng bộ đếm cùng thời điểm
        }
    }

    // Cấp mã thật trong giao dịch đang mở (khóa bi quan PESSIMISTIC_WRITE tương đương SELECT ... FOR UPDATE)
    private static String allocateEmployeeCode(Session session, String prefix) {
        CodeSequence seq = session.find(CodeSequence.class, sequenceName(prefix), LockModeType.PESSIMISTIC_WRITE);
        if (seq == null) {
            throw new IllegalStateException("Thiếu bộ đếm mã nhân viên " + sequenceName(prefix));
        }
        long next = seq.getCurrentNumber() + 1;
        seq.setCurrentNumber(next);
        return formatEmployeeCode(prefix, next);
    }

    private static String employeeCodePrefix(LocalDate hiredDate) {
        return "PMS" + (hiredDate != null ? hiredDate : LocalDate.now()).format(YYMM);
    }

    private static String sequenceName(String prefix) {
        return "EMPLOYEE-" + prefix.substring(3);
    }

    private static String formatEmployeeCode(String prefix, long number) {
        return prefix + String.format("%03d", number);
    }

    public Optional<EmployeeConflict> findByPhone(String phone, Integer excludeId) {
        return findConflict("phone", phone, excludeId, "số điện thoại");
    }

    public Optional<EmployeeConflict> findByNationalId(String nationalId, Integer excludeId) {
        return findConflict("nationalId", nationalId, excludeId, "CCCD");
    }

    private Optional<EmployeeConflict> findConflict(String field, String value, Integer excludeId, String label) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return HibernateUtil.executeWithResult(session -> {
                String hql =
                    "SELECT new com.pharmacy.employee.dto.EmployeeConflict(e.code, e.fullName, e.status) " +
                    "FROM Employee e WHERE e." +
                    field +
                    " = :value" +
                    (excludeId != null ? " AND e.id <> :id" : "");
                var query = session
                    .createSelectionQuery(hql, EmployeeConflict.class)
                    .setParameter("value", value.trim());
                if (excludeId != null) query.setParameter("id", excludeId);
                return query.setMaxResults(1).uniqueResultOptional();
            });
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trùng {} [{}]: ", label, value, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi kiểm tra " + label + ".", e);
        }
    }

    public boolean updateOwnProfile(Integer employeeId, String fullName, String phone, String address) {
        try {
            return HibernateUtil.executeWithResult(
                session ->
                    session
                        .createMutationQuery(
                            """
                                UPDATE Employee e SET e.fullName = :fullName, e.phone = :phone, e.address = :address
                                WHERE e.id = :id
                            """
                        )
                        .setParameter("fullName", fullName)
                        .setParameter("phone", phone)
                        .setParameter("address", address)
                        .setParameter("id", employeeId)
                        .executeUpdate() > 0
            );
        } catch (RuntimeException e) {
            log.error("Lỗi khi cập nhật hồ sơ cá nhân [ID: {}]: ", employeeId, e);
            throw e;
        }
    }

    public Optional<Employee> findByIdWithDetails(Integer id) {
        if (id == null) return Optional.empty();
        return HibernateUtil.executeWithResult(session -> {
            String hql = """
                SELECT e FROM Employee e
                LEFT JOIN FETCH e.account a
                LEFT JOIN FETCH a.role r
                WHERE e.id = :id
            """;
            return session.createSelectionQuery(hql, Employee.class).setParameter("id", id).uniqueResultOptional();
        });
    }

    public List<com.pharmacy.employee.dto.EmployeeOption> findEmployeeOptions() {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            return session
                .createSelectionQuery(
                    "SELECT new com.pharmacy.employee.dto.EmployeeOption(e.id, e.code, e.fullName) FROM Employee e ORDER BY e.fullName ASC, e.code ASC",
                    com.pharmacy.employee.dto.EmployeeOption.class
                )
                .list();
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách nhân viên: ", e);
            return List.of();
        }
    }

    public EmployeeSummary getEmployeeSummary() {
        try (Session session = HibernateUtil.openReadOnlySession()) {
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

    public PageResult<Employee> getEmployeesPage(
        int page,
        int size,
        String keyword,
        String gender,
        String role,
        String status,
        String accountStatus
    ) {
        return getEmployeesPage(page, size, keyword, gender, role, status, accountStatus, null);
    }

    public PageResult<Employee> getEmployeesPage(
        int page,
        int size,
        String keyword,
        String gender,
        String role,
        String status,
        String accountStatus,
        Long cachedTotal
    ) {
        try (Session session = HibernateUtil.openReadOnlySession()) {
            StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");
            Map<String, Object> params = new HashMap<>();

            // Từ khóa chỉ gồm chữ số/dấu + → tìm theo SĐT/CCCD/mã (bỏ tên); còn lại thêm cả họ tên. Mã nhân viên khớp theo tiền tố.
            boolean digitsOnly = keyword != null && keyword.trim().matches("^[0-9+\\s]+$");
            SearchFilter search = SearchFilter.of(keyword, "kw").prefix("e.code").contains("e.phone", "e.nationalId");
            if (!digitsOnly) search.contains("e.fullName");
            whereClause.append(search.hql());

            List<String> genders = parseMultiFilter(gender);
            if (!genders.isEmpty()) {
                whereClause.append(" AND e.gender IN (:genders) ");
                params.put("genders", genders);
            }

            List<String> roleCodes = parseMultiFilter(role)
                .stream()
                .map(r -> "Quản lý".equalsIgnoreCase(r) || "MANAGER".equalsIgnoreCase(r) ? "MANAGER" : "STAFF")
                .distinct()
                .toList();
            if (!roleCodes.isEmpty()) {
                whereClause.append(" AND r.code IN (:roleCodes) ");
                params.put("roleCodes", roleCodes);
            }

            if (status != null && !status.isBlank() && !"Tất cả".equalsIgnoreCase(status)) {
                Employee.Status empStatus =
                    "Đang làm".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status)
                        ? Employee.Status.ACTIVE
                        : Employee.Status.INACTIVE;
                whereClause.append(" AND e.status = :empStatus ");
                params.put("empStatus", empStatus);
            }

            List<Account.Status> accStatuses = parseMultiFilter(accountStatus)
                .stream()
                .map(a ->
                    "Hoạt động".equalsIgnoreCase(a) || "Active".equalsIgnoreCase(a)
                        ? Account.Status.ACTIVE
                        : Account.Status.DISABLED
                )
                .distinct()
                .toList();
            if (!accStatuses.isEmpty()) {
                whereClause.append(" AND a.status IN (:accStatuses) ");
                params.put("accStatuses", accStatuses);
            }

            boolean needAccountJoin = !roleCodes.isEmpty() || !accStatuses.isEmpty();

            long total;
            if (cachedTotal != null && cachedTotal >= 0) {
                total = cachedTotal;
            } else {
                String countHql = needAccountJoin
                    ? "SELECT COUNT(e.id) FROM Employee e LEFT JOIN e.account a LEFT JOIN a.role r " + whereClause
                    : "SELECT COUNT(e.id) FROM Employee e " + whereClause;
                var countQuery = session.createSelectionQuery(countHql, Long.class);
                params.forEach(countQuery::setParameter);
                search.bind(countQuery);
                Long totalCount = countQuery.getSingleResult();
                total = totalCount != null ? totalCount : 0L;
            }

            if (total == 0) {
                return PageResult.empty(page, size);
            }

            String dataHql =
                "SELECT e FROM Employee e LEFT JOIN FETCH e.account a LEFT JOIN FETCH a.role r " +
                whereClause +
                " ORDER BY e.id DESC";
            var dataQuery = session.createSelectionQuery(dataHql, Employee.class);
            params.forEach(dataQuery::setParameter);
            search.bind(dataQuery);
            dataQuery.setFirstResult((page - 1) * size);
            dataQuery.setMaxResults(size);

            List<Employee> items = dataQuery.getResultList();
            return new PageResult<>(items, total, page, size);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang nhân viên: ", e);
            return PageResult.empty(page, size);
        }
    }

    private static List<String> parseMultiFilter(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return java.util.Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(v -> !v.isEmpty() && !"Tất cả".equalsIgnoreCase(v))
            .distinct()
            .toList();
    }

    public boolean addEmployeeWithAccount(Employee emp, String roleDisplayOrCode, String initialPasswordHash) {
        String roleCode = roleDisplayOrCode;
        if ("Quản lý".equalsIgnoreCase(roleDisplayOrCode) || "MANAGER".equalsIgnoreCase(roleDisplayOrCode)) {
            roleCode = "MANAGER";
        } else if ("Nhân viên".equalsIgnoreCase(roleDisplayOrCode) || "STAFF".equalsIgnoreCase(roleDisplayOrCode)) {
            roleCode = "STAFF";
        }
        final String code = roleCode;
        final boolean needsCode = emp.getCode() == null || emp.getCode().isBlank();
        if (emp.getHiredDate() == null) {
            emp.setHiredDate(LocalDate.now());
        }
        final String prefix = employeeCodePrefix(emp.getHiredDate());
        try {
            if (needsCode) {
                ensureEmployeeSequence(prefix);
            }
            return HibernateUtil.executeWithResult(session -> {
                String allocatedCode = needsCode ? allocateEmployeeCode(session, prefix) : null;
                Role role = session
                    .createSelectionQuery("FROM Role r WHERE r.code = :code", Role.class)
                    .setParameter("code", code)
                    .uniqueResult();
                if (role == null) {
                    // Ném lỗi để rollback, trả mã vừa cấp lại cho bộ đếm
                    throw new IllegalArgumentException("Không tìm thấy vai trò với mã: " + code);
                }
                if (allocatedCode != null) {
                    emp.setCode(allocatedCode);
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
            if (needsCode) {
                emp.setCode(null);
            }
            emp.setId(null);
            log.error("Lỗi khi thêm mới nhân viên và tài khoản: ", e);
            return false;
        }
    }

    public boolean markAsInactive(Integer employeeId) {
        try {
            HibernateUtil.execute(session -> {
                session
                    .createMutationQuery("UPDATE Employee e SET e.status = :st WHERE e.id = :id")
                    .setParameter("st", Employee.Status.INACTIVE)
                    .setParameter("id", employeeId)
                    .executeUpdate();
                session
                    .createMutationQuery("UPDATE Account a SET a.status = :st WHERE a.employee.id = :id")
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
                int rowsUpdated = session
                    .createMutationQuery(
                        """
                            UPDATE Employee e SET
                                e.fullName = :fullName, e.dob = :dob, e.gender = :gender,
                                e.nationalId = :nationalId, e.phone = :phone, e.address = :address,
                                e.status = :status
                            WHERE e.id = :id
                        """
                    )
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

                Role role = session
                    .createSelectionQuery("FROM Role r WHERE r.code = :roleVal OR r.name = :roleVal", Role.class)
                    .setParameter("roleVal", req.roleName())
                    .setMaxResults(1)
                    .uniqueResult();
                var accountUpdate = session
                    .createMutationQuery(
                        role != null
                            ? "UPDATE Account a SET a.status = :status, a.role.id = :roleId WHERE a.employee.id = :empId"
                            : "UPDATE Account a SET a.status = :status WHERE a.employee.id = :empId"
                    )
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
