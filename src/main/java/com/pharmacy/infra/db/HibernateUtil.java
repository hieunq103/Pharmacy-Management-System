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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneId;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HibernateUtil {
    private static final Logger log = LoggerFactory.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;
    private static HikariDataSource dataSource;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            init();
        }
        return sessionFactory;
    }

    public static synchronized void init() {
        if (sessionFactory != null)
            return;
        StandardServiceRegistry registry = null;
        try {
            log.info("Khởi tạo Hibernate SessionFactory với HikariCP...");

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

            config.addDataSourceProperty("rewriteBatchedStatements", "true");

            config.setConnectionInitSql("SET time_zone = '" + sessionTimeZoneOffset(config.getJdbcUrl()) + "'");

            dataSource = new HikariDataSource(config);

            StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
            registryBuilder.applySetting(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
            registryBuilder.applySetting(AvailableSettings.HBM2DDL_AUTO, "none");

            registryBuilder.applySetting(AvailableSettings.SHOW_SQL, AppConfig.get("hibernate.show_sql", "false"));
            registryBuilder.applySetting(AvailableSettings.FORMAT_SQL, AppConfig.get("hibernate.format_sql", "false"));
            registryBuilder.applySetting(AvailableSettings.GENERATE_STATISTICS,
                    AppConfig.get("hibernate.generate_statistics", "false"));

            registryBuilder.applySetting(AvailableSettings.STATEMENT_BATCH_SIZE, AppConfig.get("hibernate.jdbc.batch_size", "50"));
            registryBuilder.applySetting(AvailableSettings.ORDER_INSERTS, "true");
            registryBuilder.applySetting(AvailableSettings.ORDER_UPDATES, "true");

            registry = registryBuilder.build();

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

    static String sessionTimeZoneOffset(String jdbcUrl) {
        ZoneId zone = ZoneId.systemDefault();
        Matcher m = Pattern.compile("[?&](?:serverTimezone|connectionTimeZone)=([^&]+)").matcher(jdbcUrl == null ? "" : jdbcUrl);
        if (m.find()) {
            String id = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
            if (!"LOCAL".equalsIgnoreCase(id) && !"SERVER".equalsIgnoreCase(id)) {
                try {
                    zone = ZoneId.of(id);
                } catch (Exception e) {
                    log.warn("Múi giờ '{}' trong db.url không hợp lệ, dùng múi giờ hệ thống {}", id, zone);
                }
            }
        }
        String offset = zone.getRules().getOffset(Instant.now()).getId();
        return "Z".equals(offset) ? "+00:00" : offset;
    }

    public static <R> R executeWithResult(Function<Session, R> function) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                R result = function.apply(session);
                tx.commit();
                return result;
            } catch (Exception e) {
                if (tx != null && tx.isActive())
                    tx.rollback();
                throw e;
            }
        }
    }

    public static void execute(Consumer<Session> consumer) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                consumer.accept(session);
                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive())
                    tx.rollback();
                throw e;
            }
        }
    }

    public static boolean isDatabaseAlive() {
        if (dataSource == null || dataSource.isClosed())
            return false;
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

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
