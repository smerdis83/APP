package com.example.foodapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new RuntimeException("Cannot find application.properties in classpath.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }

    /** Returns the raw property value or null if not found. */
    public static String get(String key) {
        return props.getProperty(key);
    }

    // Convenience getters for DB and JWT:
    public static String getDbUrl() {
        return get("db.url");
    }

    public static String getDbUsername() {
        return get("db.username");
    }

    public static String getDbPassword() {
        return get("db.password");
    }

    public static String getJwtSecret() {
        return get("jwt.secret");
    }

    public static long getJwtExpirationMs() {
        return Long.parseLong(get("jwt.expirationMs"));
    }
}
