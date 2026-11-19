package com.pharmacy.infra.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();
    private static Dotenv dotenv;

    static {
        loadConfiguration();
    }

    private AppConfig() {}

    private static void loadConfiguration() {
        loadFromClasspath("/application.properties");
        loadFromClasspath("/bundled-config.properties");
        loadFromUserFile();

        dotenv = Dotenv.configure().ignoreIfMissing().load();

        override("DB_URL", "db.url");
        override("DB_USER", "db.username");
        override("DB_PASSWORD", "db.password");
        override("DB_AUTO_MIGRATE", "db.autoMigrate");

        if (isBlank(properties.getProperty("db.url")) || isBlank(properties.getProperty("db.username"))) {
            log.error(
                "Thiếu cấu hình kết nối cơ sở dữ liệu: hãy tạo file .env (sao chép từ .env.example) " +
                    "và điền DB_URL, DB_USER, DB_PASSWORD, hoặc đặt các biến môi trường tương ứng."
            );
        }

        log.info("AppConfig đã nạp xong cấu hình hệ thống.");
    }

    private static void loadFromClasspath(String resource) {
        try (InputStream in = AppConfig.class.getResourceAsStream(resource)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception e) {
            log.error("Không thể đọc {}", resource, e);
        }
    }

    private static void loadFromUserFile() {
        Path file = userConfigFile();
        if (!Files.isRegularFile(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        } catch (Exception e) {
            log.error("Không thể đọc {}", file, e);
        }
    }

    public static Path userConfigDir() {
        return Path.of(System.getProperty("user.home"), ".pharmaos");
    }

    private static Path userConfigFile() {
        return userConfigDir().resolve("config.properties");
    }

    public static boolean autoMigrate() {
        return Boolean.parseBoolean(properties.getProperty("db.autoMigrate", "false"));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void override(String envKey, String propKey) {
        String val = env(envKey);
        if (val != null) {
            properties.setProperty(propKey, val);
        }
    }

    public static String env(String key) {
        String val = System.getenv(key);
        if ((val == null || val.isBlank()) && dotenv != null) {
            val = dotenv.get(key);
        }
        return val == null || val.isBlank() ? null : val.trim();
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
