package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Menu;
import com.example.foodapp.util.JdbcUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuDao {
    private final ObjectMapper mapper = new ObjectMapper();

    public MenuDao() {
        try {
            createMenuTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure menus table", e);
        }
    }

    public void createMenuTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS menus (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "restaurant_id INT NOT NULL, " +
                "title VARCHAR(100) NOT NULL, " +
                "item_ids TEXT NOT NULL, " + // JSON array
                "UNIQUE (restaurant_id, title), " +
                "FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public void addMenu(Menu menu) throws SQLException {
        String sql = "INSERT INTO menus (restaurant_id, title, item_ids) VALUES (?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, menu.getRestaurantId());
            ps.setString(2, menu.getTitle());
            ps.setString(3, mapper.writeValueAsString(menu.getItemIds()));
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating menu failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    menu.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating menu failed, no ID obtained.");
                }
            }
        } catch (Exception e) {
            throw new SQLException("Failed to add menu: " + e.getMessage(), e);
        }
    }

    public Menu getMenu(int restaurantId, String title) throws SQLException {
        String sql = "SELECT * FROM menus WHERE restaurant_id = ? AND title = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            ps.setString(2, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToMenu(rs);
                }
            }
        }
        return null;
    }

    public void updateMenu(Menu menu) throws SQLException {
        String sql = "UPDATE menus SET item_ids = ? WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mapper.writeValueAsString(menu.getItemIds()));
            ps.setInt(2, menu.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new SQLException("Failed to update menu: " + e.getMessage(), e);
        }
    }

    public void deleteMenu(int restaurantId, String title) throws SQLException {
        String sql = "DELETE FROM menus WHERE restaurant_id = ? AND title = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            ps.setString(2, title);
            ps.executeUpdate();
        }
    }

    public void removeItemFromMenu(int restaurantId, String title, int itemId) throws SQLException {
        Menu menu = getMenu(restaurantId, title);
        if (menu == null) return;
        List<Integer> items = new ArrayList<>(menu.getItemIds());
        items.removeIf(id -> id == itemId);
        menu.setItemIds(items);
        updateMenu(menu);
    }

    public void addItemToMenu(int restaurantId, String title, int itemId) throws SQLException {
        Menu menu = getMenu(restaurantId, title);
        if (menu == null) return;
        List<Integer> items = new ArrayList<>(menu.getItemIds());
        if (!items.contains(itemId)) items.add(itemId);
        menu.setItemIds(items);
        updateMenu(menu);
    }

    /**
     * Renames a menu for a given restaurant.
     * Throws SQLException if the update fails (e.g., duplicate title).
     */
    public void renameMenu(int restaurantId, String oldTitle, String newTitle) throws SQLException {
        String sql = "UPDATE menus SET title = ? WHERE restaurant_id = ? AND title = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setInt(2, restaurantId);
            ps.setString(3, oldTitle);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Renaming menu failed, no rows affected (menu not found or duplicate title)." );
            }
        }
    }

    // Returns all menus for a given restaurant
    public List<Menu> getMenusByRestaurant(int restaurantId) throws SQLException {
        String sql = "SELECT * FROM menus WHERE restaurant_id = ?";
        List<Menu> menus = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    menus.add(mapRowToMenu(rs));
                }
            }
        }
        return menus;
    }

    private Menu mapRowToMenu(ResultSet rs) throws SQLException {
        Menu menu = new Menu();
        menu.setId(rs.getInt("id"));
        menu.setRestaurantId(rs.getInt("restaurant_id"));
        menu.setTitle(rs.getString("title"));
        try {
            String itemsJson = rs.getString("item_ids");
            List<Integer> items = itemsJson != null && !itemsJson.isEmpty() ?
                Arrays.asList(mapper.readValue(itemsJson, Integer[].class)) : new ArrayList<>();
            menu.setItemIds(items);
        } catch (Exception e) {
            menu.setItemIds(new ArrayList<>());
        }
        return menu;
    }
} 