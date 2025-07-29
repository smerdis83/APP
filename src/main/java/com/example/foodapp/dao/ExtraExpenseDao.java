package com.example.foodapp.dao;

import com.example.foodapp.model.entity.ExtraExpense;
import com.example.foodapp.util.JdbcUtil;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExtraExpenseDao {
    public ExtraExpenseDao() {
        try {
            createTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure extra_expenses table", e);
        }
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS extra_expenses (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "restaurant_id INT NOT NULL, " +
                "name VARCHAR(100) NOT NULL, " +
                "amount INT NOT NULL, " +
                "FOREIGN KEY (restaurant_id) REFERENCES restaurants(id) ON DELETE CASCADE" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public void addExtraExpense(ExtraExpense expense) throws SQLException {
        String sql = "INSERT INTO extra_expenses (restaurant_id, name, amount) VALUES (?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, expense.getRestaurantId());
            ps.setString(2, expense.getName());
            ps.setInt(3, expense.getAmount());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating extra expense failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    expense.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating extra expense failed, no ID obtained.");
                }
            }
        }
    }

    public List<ExtraExpense> getExtraExpensesByRestaurant(int restaurantId) throws SQLException {
        List<ExtraExpense> list = new ArrayList<>();
        String sql = "SELECT * FROM extra_expenses WHERE restaurant_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, restaurantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToExtraExpense(rs));
                }
            }
        }
        return list;
    }

    public void updateExtraExpense(ExtraExpense expense) throws SQLException {
        String sql = "UPDATE extra_expenses SET name = ?, amount = ? WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, expense.getName());
            ps.setInt(2, expense.getAmount());
            ps.setInt(3, expense.getId());
            ps.executeUpdate();
        }
    }

    public void deleteExtraExpense(int id) throws SQLException {
        String sql = "DELETE FROM extra_expenses WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private ExtraExpense mapRowToExtraExpense(ResultSet rs) throws SQLException {
        ExtraExpense e = new ExtraExpense();
        e.setId(rs.getInt("id"));
        e.setRestaurantId(rs.getInt("restaurant_id"));
        e.setName(rs.getString("name"));
        e.setAmount(rs.getInt("amount"));
        return e;
    }
}