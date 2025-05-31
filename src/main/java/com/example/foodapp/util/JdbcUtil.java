package com.example.foodapp.util;

import com.example.foodapp.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JdbcUtil {
    /**
     * Opens and returns a new JDBC Connection to the MySQL database,
     * using parameters from application.properties.
     */
    public static Connection getConnection() throws SQLException {
        String url = AppConfig.getDbUrl();
        String user = AppConfig.getDbUsername();
        String pass = AppConfig.getDbPassword();
        return DriverManager.getConnection(url, user, pass);
    }

    /** 
     * Convenience: close a Connection without throwing checked exceptions.
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }
} 