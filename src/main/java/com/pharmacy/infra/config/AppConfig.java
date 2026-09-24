package com.pharmacy.infra.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();

    static {
        loadConfiguration();
    }

    private AppConfig() {}

    private static void loadConfiguration() {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception e) {
            log.error("Không thể đọc application.properties", e);
        }

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        override("DB_URL", "db.url", dotenv);
        override("DB_USER", "db.username", dotenv);
        override("DB_PASSWORD", "db.password", dotenv);

        log.info("AppConfig đã nạp xong cấu hình hệ thống.");
    }

    private static void override(String envKey, String propKey, Dotenv dotenv) {
        String val = System.getenv(envKey);
        if (val == null || val.isBlank()) {
            val = dotenv.get(envKey);
        }
        if (val != null && !val.isBlank()) {
            properties.setProperty(propKey, val);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}