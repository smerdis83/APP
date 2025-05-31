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
        String value = props.getProperty(key);
        return value != null ? value.trim() : null;
    }

    // Convenience getters for DB and JWT:
    public static String getDbUrl() {
        String value = get("db.url");
        if (value == null) {
            throw new RuntimeException("db.url is not set in application.properties");
        }
        return value;
    }

    public static String getDbUsername() {
        String value = get("db.username");
        if (value == null) {
            throw new RuntimeException("db.username is not set in application.properties");
        }
        return value;
    }

    public static String getDbPassword() {
        String value = get("db.password");
        if (value == null) {
            throw new RuntimeException("db.password is not set in application.properties");
        }
        return value;
    }

    public static String getJwtSecret() {
        String value = get("jwt.secret");
        if (value == null) {
            throw new RuntimeException("jwt.secret is not set in application.properties");
        }
        return value;
    }

    public static long getJwtExpirationMs() {
        String value = get("jwt.expirationMs");
        if (value == null) {
            throw new RuntimeException("jwt.expirationMs is not set in application.properties");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid jwt.expirationMs value in application.properties: " + value);
        }
    }
}
