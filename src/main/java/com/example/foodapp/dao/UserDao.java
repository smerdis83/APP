package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Role;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.util.JdbcUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
                "address VARCHAR(255), " +
                "profile_image_base64 LONGTEXT, " +
                "bank_name VARCHAR(100), " +
                "account_number VARCHAR(100), " +
                "wallet_balance INT NOT NULL DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
        // Ensure wallet_balance column exists (for upgrades)
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "ALTER TABLE users ADD COLUMN IF NOT EXISTS wallet_balance INT NOT NULL DEFAULT 0")) {
            statement.executeUpdate();
        } catch (SQLException ignore) {}
    }
    /**
     * Inserts a new user into the database.
     * On success, sets the generated ID on the provided User object.
     */
    public void createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, phone, email, password_hash, role, enabled, address, profile_image_base64, bank_name, account_number) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name()); // store enum as string
            ps.setBoolean(6, user.isEnabled());
            ps.setString(7, user.getAddress());
            ps.setString(8, user.getProfileImageBase64());
            ps.setString(9, user.getBankInfo() != null ? user.getBankInfo().getBankName() : null);
            ps.setString(10, user.getBankInfo() != null ? user.getBankInfo().getAccountNumber() : null);

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
        String sql = "SELECT id, full_name, phone, email, password_hash, role, enabled, address, profile_image_base64, bank_name, account_number, created_at, updated_at "
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
        String sql = "SELECT id, full_name, phone, email, password_hash, role, enabled, address, profile_image_base64, bank_name, account_number, created_at, updated_at "
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

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(extractUserFromResultSet(rs));
                }
            }
        }
        return users;
    }

    /**
     * Updates user profile fields (full_name, phone, email) by user ID.
     */
    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, email = ?, address = ?, profile_image_base64 = ?, bank_name = ?, account_number = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getPhone());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getAddress());
            ps.setString(5, user.getProfileImageBase64());
            ps.setString(6, user.getBankInfo() != null ? user.getBankInfo().getBankName() : null);
            ps.setString(7, user.getBankInfo() != null ? user.getBankInfo().getAccountNumber() : null);
            ps.setInt(8, user.getId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }
        }
    }

    public void updateWalletBalance(int userId, int newBalance) throws SQLException {
        String sql = "UPDATE users SET wallet_balance = ? WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newBalance);
            ps.setInt(2, userId);
            int affectedRows = ps.executeUpdate();
            System.out.println("[DEBUG] updateWalletBalance: User ID=" + userId + ", New Balance=" + newBalance + ", Affected Rows=" + affectedRows);
            if (affectedRows == 0) {
                System.err.println("[ERROR] updateWalletBalance: No rows affected for user ID=" + userId);
            }
        }
    }
    public int getWalletBalance(int userId) throws SQLException {
        String sql = "SELECT wallet_balance FROM users WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int balance = rs.getInt("wallet_balance");
                    System.out.println("[DEBUG] getWalletBalance: User ID=" + userId + ", Balance=" + balance);
                    return balance;
                } else {
                    System.err.println("[ERROR] getWalletBalance: No user found with ID=" + userId);
                    return 0;
                }
            }
        }
    }

    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void enableUser(int userId) throws SQLException {
        String sql = "UPDATE users SET enabled = TRUE WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
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
        String address = rs.getString("address");
        String profileImageBase64 = rs.getString("profile_image_base64");
        String bankName = rs.getString("bank_name");
        String accountNumber = rs.getString("account_number");
        int walletBalance = 0;
        try { walletBalance = rs.getInt("wallet_balance"); } catch (Exception ignore) {}
        User.BankInfo bankInfo = (bankName != null || accountNumber != null) ? new User.BankInfo(bankName, accountNumber) : null;
        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
        LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;
        User user = new User(userId, fullName, phone, email, passwordHash, role, enabled, createdAt, updatedAt, address, profileImageBase64, bankInfo);
        user.setWalletBalance(walletBalance);
        return user;
    }
} 