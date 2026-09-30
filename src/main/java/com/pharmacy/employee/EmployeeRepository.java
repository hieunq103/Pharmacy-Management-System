package com.pharmacy.employee;

import com.pharmacy.infra.db.HibernateUtil;
import com.pharmacy.profile.ProfileUpdateRequest;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public boolean updateProfile(ProfileUpdateRequest req) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            String hql = "UPDATE Employee e SET e.phone = :phone, e.address = :address WHERE e.id = :id";
            int rowsUpdated = session.createMutationQuery(hql)
                    .setParameter("phone", req.phone())
                    .setParameter("address", req.address())
                    .setParameter("id", req.employeeId())
                    .executeUpdate();

            tx.commit();
            return rowsUpdated > 0;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            log.error("Lỗi khi cập nhật hồ sơ nhân viên ID [{}]: ", req.employeeId(), e);
            throw e;
        }
    }
}