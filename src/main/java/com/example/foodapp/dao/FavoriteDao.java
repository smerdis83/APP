package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Restaurant;
import com.example.foodapp.util.JdbcUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FavoriteDao {
    public FavoriteDao() {
        try {
            createFavoriteTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure favorites table", e);
        }
    }

    public void createFavoriteTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS favorites (" +
                "user_id INT NOT NULL, " +
                "restaurant_id INT NOT NULL, " +
                "PRIMARY KEY (user_id, restaurant_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE" +
                ")";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public void addFavorite(int userId, int restaurantId) throws SQLException {
        String sql = "INSERT IGNORE INTO favorites (user_id, restaurant_id) VALUES (?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, restaurantId);
            ps.executeUpdate();
        }
    }

    public void removeFavorite(int userId, int restaurantId) throws SQLException {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND restaurant_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, restaurantId);
            ps.executeUpdate();
        }
    }

    public List<Integer> getFavoritesByUser(int userId) throws SQLException {
        String sql = "SELECT restaurant_id FROM favorites WHERE user_id = ?";
        List<Integer> restaurantIds = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    restaurantIds.add(rs.getInt("restaurant_id"));
                }
            }
        }
        return restaurantIds;
    }

    public boolean isFavorite(int userId, int restaurantId) throws SQLException {
        String sql = "SELECT 1 FROM favorites WHERE user_id = ? AND restaurant_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
} 