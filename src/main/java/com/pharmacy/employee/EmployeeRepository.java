package com.pharmacy.employee;

import com.pharmacy.account.Account;
import com.pharmacy.account.Role;
import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.profile.ProfileUpdateRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class EmployeeRepository {

    private static final Logger log = LoggerFactory.getLogger(EmployeeRepository.class);

    public boolean existsByPhoneAndNotId(String phone, Integer currentEmployeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(e.id) FROM Employee e WHERE e.phone = :phone AND e.id != :id";
            Long count = session.createSelectionQuery(hql, Long.class)
                    .setParameter("phone", phone.trim())
                    .setParameter("id", currentEmployeeId)
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trùng số điện thoại [{}]: ", phone, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi kiểm tra số điện thoại.", e);
        }
    }

    public boolean existsByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(e.id) FROM Employee e WHERE e.phone = :phone";
            Long count = session.createSelectionQuery(hql, Long.class)
                    .setParameter("phone", phone.trim())
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trùng số điện thoại [{}]: ", phone, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi kiểm tra số điện thoại.", e);
        }
    }

    public boolean existsByNationalId(String nationalId) {
        if (nationalId == null || nationalId.trim().isEmpty()) return false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(e.id) FROM Employee e WHERE e.nationalId = :nationalId";
            Long count = session.createSelectionQuery(hql, Long.class)
                    .setParameter("nationalId", nationalId.trim())
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trùng CCCD [{}]: ", nationalId, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi kiểm tra CCCD.", e);
        }
    }

    /**
     * Dùng cho CẬP NHẬT: Kiểm tra CCCD có bị trùng với NGƯỜI KHÁC hay không
     */
    public boolean existsByNationalIdAndNotId(String nationalId, Integer employeeId) {
        if (nationalId == null || nationalId.trim().isEmpty() || employeeId == null) {
            return false;
        }
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(e.id) FROM Employee e WHERE e.nationalId = :nationalId AND e.id != :id";
            Long count = session.createSelectionQuery(hql, Long.class)
                    .setParameter("nationalId", nationalId.trim())
                    .setParameter("id", employeeId)
                    .getSingleResult();
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra trùng CCCD [{}] cho Employee ID [{}]: ", nationalId, employeeId, e);
            throw new RuntimeException("Lỗi truy vấn cơ sở dữ liệu khi kiểm tra CCCD.", e);
        }
    }

    public boolean updateStaffProfile(Integer employeeId, String phone, String address) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String hql = "UPDATE Employee e SET e.phone = :phone, e.address = :address WHERE e.id = :id";
            int rowsUpdated = session.createMutationQuery(hql)
                    .setParameter("phone", phone)
                    .setParameter("address", address)
                    .setParameter("id", employeeId)
                    .executeUpdate();

            tx.commit();
            return rowsUpdated > 0;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Lỗi khi cập nhật hồ sơ Staff [ID: {}]: ", employeeId, e);
            throw e;
        }
    }

    public boolean updateManagerProfile(ProfileUpdateRequest req) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String hql = "UPDATE Employee e SET " +
                    "e.phone = :phone, " +
                    "e.address = :address, " +
                    "e.fullName = :fullName, " +
                    "e.nationalId = :nationalId, " +
                    "e.dob = :dob, " +
                    "e.gender = :gender " +
                    "WHERE e.id = :id";

            int rowsUpdated = session.createMutationQuery(hql)
                    .setParameter("phone", req.phone())
                    .setParameter("address", req.address())
                    .setParameter("fullName", req.fullName())
                    .setParameter("nationalId", req.nationalId())
                    .setParameter("dob", req.dob())
                    .setParameter("gender", req.gender())
                    .setParameter("id", req.employeeId())
                    .executeUpdate();

            tx.commit();
            return rowsUpdated > 0;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Lỗi khi cập nhật hồ sơ Manager [ID: {}]: ", req.employeeId(), e);
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
            return session.createSelectionQuery(hql, Employee.class)
                    .setParameter("id", id)
                    .uniqueResultOptional();
        });
    }

    public List<Object[]> getLoginHistory(Integer accountId) {
        if (accountId == null) return List.of();
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

    public List<Employee> findAllWithDetails() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT DISTINCT e FROM Employee e " +
                    "LEFT JOIN FETCH e.account a " +
                    "LEFT JOIN FETCH a.role r " +
                    "ORDER BY e.id DESC";
            return session.createSelectionQuery(hql, Employee.class).getResultList();
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách nhân viên: ", e);
            return Collections.emptyList();
        }
    }

    public boolean addEmployeeWithAccount(Employee emp, String roleDisplayOrCode, String initialPassword) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String roleCode = roleDisplayOrCode;
            if ("Quản lý".equalsIgnoreCase(roleDisplayOrCode) || "MANAGER".equalsIgnoreCase(roleDisplayOrCode)) {
                roleCode = "MANAGER";
            } else if ("Nhân viên".equalsIgnoreCase(roleDisplayOrCode) || "STAFF".equalsIgnoreCase(roleDisplayOrCode)) {
                roleCode = "STAFF";
            }

            String hqlRole = "FROM Role r WHERE UPPER(r.code) = :code";
            Role role = session.createSelectionQuery(hqlRole, Role.class)
                    .setParameter("code", roleCode)
                    .uniqueResult();

            if (role == null) {
                log.error("Không tìm thấy vai trò với mã: {}", roleCode);
                return false;
            }

            session.persist(emp);

            // 3. Khởi tạo Account đúng theo các setter của Account.java
            Account account = new Account();
            account.setEmployee(emp);
            account.setRole(role);
            account.setPasswordHash(initialPassword);
            account.setStatus(Account.Status.ACTIVE);
            account.setMustChangePassword(true);

            session.persist(account);

            tx.commit();
            return true;

        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Lỗi khi thêm mới nhân viên và tài khoản: ", e);
            return false;
        }
    }

    public boolean markAsInactive(Integer employeeId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            session.createMutationQuery("UPDATE Employee e SET e.status = 'INACTIVE' WHERE e.id = :id")
                    .setParameter("id", employeeId)
                    .executeUpdate();

            session.createMutationQuery("UPDATE Account a SET a.status = 'DISABLED' WHERE a.employee.id = :id")
                    .setParameter("id", employeeId)
                    .executeUpdate();

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            log.error("Lỗi khi đánh dấu nhân viên đã nghỉ [ID: {}]: ", employeeId, e);
            return false;
        }
    }

    public boolean updateEmployeeByManager(EmployeeUpdateRequest req) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            // 1. Cập nhật bảng employees
            String hqlEmployee = """
            UPDATE Employee e SET
                e.fullName = :fullName,
                e.dob = :dob,
                e.gender = :gender,
                e.nationalId = :nationalId,
                e.phone = :phone,
                e.address = :address,
                e.status = :status
            WHERE e.id = :id
        """;

            int rowsUpdated = session.createMutationQuery(hqlEmployee)
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
                tx.rollback();
                return false;
            }

            // 2. Tìm Role theo code hoặc name
            String hqlRole = "FROM Role r WHERE r.code = :roleVal OR r.name = :roleVal";
            Role role = session.createQuery(hqlRole, Role.class)
                    .setParameter("roleVal", req.roleName())
                    .setMaxResults(1)
                    .uniqueResult();

            // 3. Cập nhật bảng accounts
            String hqlAccount = """
            UPDATE Account a SET
                a.status = :status,
                a.role.id = :roleId
            WHERE a.employee.id = :empId
        """;

            if (role != null) {
                session.createMutationQuery(hqlAccount)
                        .setParameter("status", Account.Status.valueOf(req.accountStatus()))
                        .setParameter("roleId", role.getId())
                        .setParameter("empId", req.employeeId())
                        .executeUpdate();
            } else {
                // Trường hợp không đổi role, chỉ cập nhật status
                String hqlAccountStatusOnly = """
                UPDATE Account a SET
                    a.status = :status
                WHERE a.employee.id = :empId
            """;
                session.createMutationQuery(hqlAccountStatusOnly)
                        .setParameter("status", Account.Status.valueOf(req.accountStatus()))
                        .setParameter("empId", req.employeeId())
                        .executeUpdate();
            }

            tx.commit();
            return true;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Lỗi cập nhật nhân viên bởi Manager [ID: {}]: ", req.employeeId(), e);
            throw e;
        }
    }
}