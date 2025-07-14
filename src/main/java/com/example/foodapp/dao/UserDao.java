package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Role;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.util.JdbcUtil;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Data Access Object for the 'users' table, using plain JDBC.
 */
public class UserDao {

    public UserDao() {
        try {
            createUserTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure users table", e);
        }
    }

    public void createUserTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "full_name VARCHAR(100) NOT NULL, " +
                "phone VARCHAR(20) NOT NULL UNIQUE, " +
                "email VARCHAR(100), " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "role ENUM('ADMIN', 'USER') NOT NULL, " +
                "enabled BOOLEAN NOT NULL DEFAULT TRUE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
    /**
     * Inserts a new user into the database.
     * On success, sets the generated ID on the provided User object.
     */
    public void createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, phone, email, password_hash, role, enabled) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name()); // store enum as string
            ps.setBoolean(6, user.isEnabled());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            // Retrieve the generated ID
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Finds a user by phone number. Returns null if no such user.
     */
    public User findByPhone(String phone) throws SQLException {
        String sql = "SELECT id, full_name, phone, email, password_hash, role, enabled, created_at, updated_at "
                   + "FROM users WHERE phone = ?";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
                return null;
            }
        }
    }

    /**
     * Finds a user by their ID. Returns null if no such user.
     */
    public User findById(int id) throws SQLException {
        String sql = "SELECT id, full_name, phone, email, password_hash, role, enabled, created_at, updated_at "
                   + "FROM users WHERE id = ?";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
                return null;
            }
        }
    }

    /**
     * Helper method to build a User object from a ResultSet row.
     */
    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        int userId = rs.getInt("id");
        String fullName = rs.getString("full_name");
        String phone = rs.getString("phone");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        Role role = Role.valueOf(rs.getString("role"));
        boolean enabled = rs.getBoolean("enabled");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
        LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;

        return new User(userId, fullName, phone, email, passwordHash, role, enabled, createdAt, updatedAt);
    }
} 