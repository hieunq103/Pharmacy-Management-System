package com.pharmacy.infra.db;

import com.pharmacy.account.Account;
import com.pharmacy.account.Role;
import com.pharmacy.employee.Employee;
import com.pharmacy.infra.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Function;

public final class HibernateUtil {
    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;
    private static HikariDataSource dataSource;

    private HibernateUtil() {}

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            init();
        }
        return sessionFactory;
    }

    public static synchronized void init() {
        if (sessionFactory != null) return;
        StandardServiceRegistry registry = null;
        try {
            log.info("Khởi tạo Hibernate SessionFactory với HikariCP...");

            // 1. Cấu hình HikariCP Connection Pool
            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(AppConfig.get("db.url"));
            config.setUsername(AppConfig.get("db.username"));
            config.setPassword(AppConfig.get("db.password"));

            config.setMaximumPoolSize(Integer.parseInt(AppConfig.get("db.pool.maxSize", "10")));
            config.setMinimumIdle(Integer.parseInt(AppConfig.get("db.pool.minIdle", "2")));
            config.setConnectionTimeout(Long.parseLong(AppConfig.get("db.pool.connectionTimeoutMs", "3000")));
            config.setValidationTimeout(Long.parseLong(AppConfig.get("db.pool.validationTimeoutMs", "2000")));
            config.setMaxLifetime(Long.parseLong(AppConfig.get("db.pool.maxLifetimeMs", "1800000")));
            config.setIdleTimeout(Long.parseLong(AppConfig.get("db.pool.idleTimeoutMs", "600000")));
            config.setPoolName("PharmaOS-MainPool");

            dataSource = new HikariDataSource(config);

            // 2. Cấu hình Hibernate Service Registry
            StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
            registryBuilder.applySetting(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
            registryBuilder.applySetting(AvailableSettings.HBM2DDL_AUTO, "none");
            registryBuilder.applySetting(AvailableSettings.SHOW_SQL, "false");

            registry = registryBuilder.build();

            // 3. Đăng ký các Entity ORM
            MetadataSources metadataSources = new MetadataSources(registry);
            metadataSources.addAnnotatedClass(Role.class);
            metadataSources.addAnnotatedClass(Employee.class);
            metadataSources.addAnnotatedClass(Account.class);

            sessionFactory = metadataSources.buildMetadata().buildSessionFactory();
            log.info("Hibernate SessionFactory đã sẵn sàng.");
        } catch (Exception e) {
            log.error("Khởi tạo SessionFactory thất bại: ", e);
            if (registry != null) {
                StandardServiceRegistryBuilder.destroy(registry);
            }
            shutdown();
            throw new RuntimeException("Không thể khởi động Hibernate SessionFactory", e);
        }
    }

    /**
     * Thực thi thao tác truy vấn có kết quả trả về trong một Transaction an toàn
     */
    public static <R> R executeWithResult(Function<Session, R> function) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                R result = function.apply(session);
                tx.commit();
                return result;
            } catch (Exception e) {
                if (tx != null && tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    /**
     * Thực thi thao tác thay đổi dữ liệu (Insert, Update, Delete) không cần giá trị trả về
     */
    public static void execute(Consumer<Session> consumer) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                consumer.accept(session);
                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    /**
     * Kiểm tra trạng thái sống của kết nối cơ sở dữ liệu (Heartbeat check)
     */
    public static boolean isDatabaseAlive() {
        if (dataSource == null || dataSource.isClosed()) return false;
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * R54: Lấy thời gian chuẩn từ máy chủ MySQL Server
     */
    public static LocalDateTime getCurrentDbTime() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT CURRENT_TIMESTAMP")) {
            if (rs.next()) {
                return rs.getTimestamp(1).toLocalDateTime();
            }
        } catch (Exception e) {
            log.error("Không lấy được thời gian từ DB server: {}", e.getMessage());
        }
        return LocalDateTime.now();
    }

    /**
     * R71: Đóng an toàn toàn bộ tài nguyên khi tắt ứng dụng
     */
    public static synchronized void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            sessionFactory = null;
            log.info("Hibernate SessionFactory đã đóng.");
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
            log.info("HikariDataSource đã đóng.");
        }
    }
}