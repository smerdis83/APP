package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Rating;
import com.example.foodapp.util.JdbcUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RatingDao {
    private final ObjectMapper mapper = new ObjectMapper();

    public RatingDao() {
        try {
            createRatingTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure ratings table", e);
        }
    }

    public void createRatingTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS ratings (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "order_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "item_id INT, " + // nullable for order-level, not null for item-level
                "rating INT NOT NULL, " +
                "comment TEXT, " +
                "image_base64 TEXT, " + // JSON array
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE (order_id, user_id, item_id), " +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public void addRating(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (order_id, user_id, rating, comment, image_base64) VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment), image_base64 = VALUES(image_base64), created_at = CURRENT_TIMESTAMP";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rating.getOrderId());
            ps.setInt(2, rating.getUserId());
            ps.setInt(3, rating.getRating());
            ps.setString(4, rating.getComment());
            String imageBase64Json = null;
            try {
                imageBase64Json = rating.getImageBase64() != null ? mapper.writeValueAsString(rating.getImageBase64()) : null;
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new SQLException("Failed to serialize imageBase64: " + e.getMessage(), e);
            }
            ps.setString(5, imageBase64Json);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    rating.setId(rs.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new SQLException("Failed to add rating: " + e.getMessage(), e);
        }
    }

    public void addItemRating(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (order_id, user_id, item_id, rating, comment, image_base64) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE rating = VALUES(rating), comment = VALUES(comment), image_base64 = VALUES(image_base64), created_at = CURRENT_TIMESTAMP";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rating.getOrderId());
            ps.setInt(2, rating.getUserId());
            ps.setInt(3, rating.getItemId());
            ps.setInt(4, rating.getRating());
            ps.setString(5, rating.getComment());
            String imageBase64Json = null;
            try {
                imageBase64Json = rating.getImageBase64() != null ? mapper.writeValueAsString(rating.getImageBase64()) : null;
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new SQLException("Failed to serialize imageBase64: " + e.getMessage(), e);
            }
            ps.setString(6, imageBase64Json);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    rating.setId(rs.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new SQLException("Failed to add item rating: " + e.getMessage(), e);
        }
    }

    public Rating getRatingByOrderAndUser(int orderId, int userId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE order_id = ? AND user_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRating(rs);
                }
            }
        }
        return null;
    }

    public List<Rating> getRatingsByOrder(int orderId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE order_id = ? ORDER BY created_at DESC";
        List<Rating> ratings = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapRowToRating(rs));
                }
            }
        }
        return ratings;
    }

    public List<Rating> getOrderLevelRatingsByOrder(int orderId) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE order_id = ? AND item_id IS NULL ORDER BY created_at DESC";
        List<Rating> ratings = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapRowToRating(rs));
                }
            }
        }
        return ratings;
    }

    public List<Rating> getRatingsByItem(int itemId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();
        String sql = "SELECT * FROM ratings WHERE item_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapRowToRating(rs));
                }
            }
        }
        return ratings;
    }
    public Rating getRatingById(int id) throws SQLException {
        String sql = "SELECT * FROM ratings WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRating(rs);
                }
            }
        }
        return null;
    }
    public void updateRating(Rating rating) throws SQLException {
        String sql = "UPDATE ratings SET rating = ?, comment = ?, image_base64 = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getRating());
            ps.setString(2, rating.getComment());
            String imageBase64Json = null;
            try {
                imageBase64Json = rating.getImageBase64() != null ? mapper.writeValueAsString(rating.getImageBase64()) : null;
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new SQLException("Failed to serialize imageBase64: " + e.getMessage(), e);
            }
            ps.setString(3, imageBase64Json);
            ps.setInt(4, rating.getId());
            ps.executeUpdate();
        }
    }
    public void deleteRating(int id) throws SQLException {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Rating mapRowToRating(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getInt("id"));
        rating.setOrderId(rs.getInt("order_id"));
        rating.setUserId(rs.getInt("user_id"));
        rating.setRating(rs.getInt("rating"));
        rating.setComment(rs.getString("comment"));
        try {
            String images = rs.getString("image_base64");
            if (images != null && !images.isEmpty()) {
                rating.setImageBase64(Arrays.asList(mapper.readValue(images, String[].class)));
            }
        } catch (Exception e) {
            rating.setImageBase64(new ArrayList<>());
        }
        Timestamp created = rs.getTimestamp("created_at");
        rating.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        return rating;
    }
} 