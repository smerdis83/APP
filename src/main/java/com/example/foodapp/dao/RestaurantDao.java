package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Restaurant;
import com.example.foodapp.util.JdbcUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RestaurantDao {

    public RestaurantDao() {
        try {
            createRestaurantTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure restaurants table", e);
        }
    }

    public void createRestaurantTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS restaurants (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "address VARCHAR(255), " +
                "phone VARCHAR(20), " +
                "owner_id INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Inserts a new restaurant row into the DB.
     * On success, sets the generated ID on the Restaurant object.
     */
    public void createRestaurant(Restaurant restaurant) throws SQLException {
        String sql = "INSERT INTO restaurants (name, address, phone, owner_id, logo_base64) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, restaurant.getName());
            ps.setString(2, restaurant.getAddress());
            ps.setString(3, restaurant.getPhone());
            ps.setInt(4, restaurant.getOwnerId());
            ps.setString(5, restaurant.getLogoBase64());
            System.out.println("[DEBUG] logoBase64 in DAO: " + (restaurant.getLogoBase64() == null ? "null" : restaurant.getLogoBase64().substring(0, Math.min(40, restaurant.getLogoBase64().length()))));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating restaurant failed, no rows affected.");
            }

            // Retrieve generated ID
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    restaurant.setId(rs.getInt(1));
                } else {
                    throw new SQLException("Creating restaurant failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Returns a list of all restaurants in the database.
     * Used for the buyer-side "browse all restaurants" view.
     */
    public List<Restaurant> findAll() throws SQLException {
        List<Restaurant> restaurants = new ArrayList<>();
        String sql = "SELECT id, name, address, phone, owner_id, created_at, updated_at, logo_base64 FROM restaurants";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String address = rs.getString("address");
                String phone = rs.getString("phone");
                int ownerId = rs.getInt("owner_id");

                Timestamp createdTs = rs.getTimestamp("created_at");
                Timestamp updatedTs = rs.getTimestamp("updated_at");
                LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
                LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;
                String logoBase64 = rs.getString("logo_base64");

                Restaurant r = new Restaurant(id, name, address, phone, ownerId, createdAt, updatedAt);
                r.setLogoBase64(logoBase64);
                restaurants.add(r);
            }
        }

        return restaurants;
    }

    /**
     * Returns a list of restaurants owned by the given seller (owner_id).
     * Useful for "my restaurants" in seller's dashboard.
     */
    public List<Restaurant> findByOwner(int ownerId) throws SQLException {
        List<Restaurant> restaurants = new ArrayList<>();
        String sql = "SELECT id, name, address, phone, owner_id, created_at, updated_at, logo_base64 FROM restaurants WHERE owner_id = ?";

        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String address = rs.getString("address");
                    String phone = rs.getString("phone");

                    Timestamp createdTs = rs.getTimestamp("created_at");
                    Timestamp updatedTs = rs.getTimestamp("updated_at");
                    LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
                    LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;
                    String logoBase64 = rs.getString("logo_base64");

                    Restaurant r = new Restaurant(id, name, address, phone, ownerId, createdAt, updatedAt);
                    r.setLogoBase64(logoBase64);
                    restaurants.add(r);
                }
            }
        }

        return restaurants;
    }

    public Restaurant findById(int id) throws SQLException {
        String sql = "SELECT id, name, address, phone, owner_id, created_at, updated_at FROM restaurants WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int rid = rs.getInt("id");
                    String name = rs.getString("name");
                    String address = rs.getString("address");
                    String phone = rs.getString("phone");
                    int ownerId = rs.getInt("owner_id");
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    Timestamp updatedTs = rs.getTimestamp("updated_at");
                    java.time.LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;
                    java.time.LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : null;
                    return new Restaurant(rid, name, address, phone, ownerId, createdAt, updatedAt);
                }
            }
        }
        return null;
    }

    /**
     * Updates all editable fields of a restaurant by id.
     * Used for PUT /restaurants/{id}.
     */
    public void updateRestaurant(Restaurant restaurant) throws SQLException {
        String sql = "UPDATE restaurants SET name = ?, address = ?, phone = ?, logo_base64 = ?, tax_fee = ?, additional_fee = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, restaurant.getName());
            ps.setString(2, restaurant.getAddress());
            ps.setString(3, restaurant.getPhone());
            ps.setString(4, restaurant.getLogoBase64());
            ps.setInt(5, restaurant.getTaxFee());
            ps.setInt(6, restaurant.getAdditionalFee());
            ps.setInt(7, restaurant.getId());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating restaurant failed, no rows affected.");
            }
        }
    }

    /**
     * Deletes a restaurant by its ID.
     * Used for DELETE /restaurants/{id}.
     */
    public void deleteRestaurant(int id) throws SQLException {
        String sql = "DELETE FROM restaurants WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting restaurant failed, no rows affected.");
            }
        }
    }
} 