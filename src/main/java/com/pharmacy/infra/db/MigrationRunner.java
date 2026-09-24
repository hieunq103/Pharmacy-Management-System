package com.pharmacy.infra.db;

import com.pharmacy.infra.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationRunner {
    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    public static void migrate() {
        log.info("Bắt đầu kiểm tra và thực thi Flyway Migration...");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl(AppConfig.get("db.url"));
        hikariConfig.setUsername(AppConfig.get("db.username"));
        hikariConfig.setPassword(AppConfig.get("db.password"));
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setPoolName("PharmaOS-FlywayPool");

        try (HikariDataSource dataSource = new HikariDataSource(hikariConfig)) {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations(AppConfig.get("flyway.locations", "classpath:db/migration"))
                    .baselineOnMigrate(Boolean.parseBoolean(AppConfig.get("flyway.baselineOnMigrate", "true")))
                    .cleanDisabled(true)
                    .load();

            flyway.migrate();
            log.info("Flyway migration hoàn tất thành công.");
        } catch (Exception e) {
            log.error("Lỗi Flyway migration: ", e);
            throw new RuntimeException("Không thể khởi tạo CSDL", e);
        }
    }
}