package com.example.foodapp.dao;

import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.util.JdbcUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FoodItemDao {
    public FoodItemDao() {
        try {
            createFoodItemTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure food_items table", e);
        }
    }

    public void createFoodItemTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS food_items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "price INT NOT NULL, " +
                "supply INT NOT NULL, " +
                "keywords TEXT, " + // comma-separated
                "vendor_id INT NOT NULL, " +
                "image_base64 LONGTEXT, " +
                "FOREIGN KEY (vendor_id) REFERENCES restaurants(id) ON DELETE CASCADE" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public void addFoodItem(FoodItem item) throws SQLException {
        String sql = "INSERT INTO food_items (name, description, price, supply, keywords, vendor_id, image_base64) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setInt(3, item.getPrice());
            ps.setInt(4, item.getSupply());
            ps.setString(5, String.join(",", item.getKeywords()));
            ps.setInt(6, item.getVendorId());
            ps.setString(7, item.getImageBase64());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating food item failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating food item failed, no ID obtained.");
                }
            }
        }
    }

    public FoodItem getFoodItemById(int id) throws SQLException {
        String sql = "SELECT * FROM food_items WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFoodItem(rs);
                }
            }
        }
        return null;
    }

    public List<FoodItem> getFoodItemsByVendor(int vendorId) throws SQLException {
        String sql = "SELECT * FROM food_items WHERE vendor_id = ?";
        List<FoodItem> items = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRowToFoodItem(rs));
                }
            }
        }
        return items;
    }

    public void updateFoodItem(FoodItem item) throws SQLException {
        String sql = "UPDATE food_items SET name=?, description=?, price=?, supply=?, keywords=?, image_base64=? WHERE id=?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setInt(3, item.getPrice());
            ps.setInt(4, item.getSupply());
            ps.setString(5, String.join(",", item.getKeywords()));
            ps.setString(6, item.getImageBase64());
            ps.setInt(7, item.getId());
            ps.executeUpdate();
        }
    }

    public void deleteFoodItem(int id) throws SQLException {
        String sql = "DELETE FROM food_items WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<FoodItem> getAllFoodItems() throws SQLException {
        List<FoodItem> items = new ArrayList<>();
        String sql = "SELECT * FROM food_items";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapRowToFoodItem(rs));
            }
        }
        return items;
    }

    private FoodItem mapRowToFoodItem(ResultSet rs) throws SQLException {
        FoodItem item = new FoodItem();
        item.setId(rs.getInt("id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setPrice(rs.getInt("price"));
        item.setSupply(rs.getInt("supply"));
        String keywordsStr = rs.getString("keywords");
        item.setKeywords(keywordsStr != null && !keywordsStr.isEmpty() ? Arrays.asList(keywordsStr.split(",")) : new ArrayList<>());
        item.setVendorId(rs.getInt("vendor_id"));
        item.setImageBase64(rs.getString("image_base64"));
        return item;
    }
} 