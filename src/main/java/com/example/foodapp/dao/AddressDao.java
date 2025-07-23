package com.example.foodapp.dao;

import com.example.foodapp.util.JdbcUtil;
import com.example.foodapp.model.entity.Address;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AddressDao {
    public AddressDao() {
        try {
            createAddressTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure addresses table", e);
        }
    }

    public void createAddressTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS addresses (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "title VARCHAR(50) NOT NULL, " +
                "address VARCHAR(255) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public List<Address> findByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM addresses WHERE user_id = ? ORDER BY created_at DESC";
        List<Address> addresses = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Address a = new Address();
                    a.setId(rs.getInt("id"));
                    a.setUserId(rs.getInt("user_id"));
                    a.setTitle(rs.getString("title"));
                    a.setAddress(rs.getString("address"));
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    Timestamp updatedTs = rs.getTimestamp("updated_at");
                    a.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);
                    a.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);
                    addresses.add(a);
                }
            }
        }
        return addresses;
    }

    public void addAddress(Address address) throws SQLException {
        String sql = "INSERT INTO addresses (user_id, title, address) VALUES (?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, address.getUserId());
            ps.setString(2, address.getTitle());
            ps.setString(3, address.getAddress());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating address failed, no rows affected.");
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    address.setId(rs.getInt(1));
                }
            }
        }
    }
} 