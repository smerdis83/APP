package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.util.JdbcUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OrderDao {
    private final ObjectMapper mapper = new ObjectMapper();

    public OrderDao() {
        try {
            createOrderTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure orders table", e);
        }
    }

    public void createOrderTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS orders (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "delivery_address VARCHAR(255) NOT NULL, " +
                "customer_id INT NOT NULL, " +
                "vendor_id INT NOT NULL, " +
                "coupon_id INT, " +
                "items TEXT NOT NULL, " + // comma-separated
                "raw_price INT NOT NULL, " +
                "tax_fee INT NOT NULL, " +
                "additional_fee INT NOT NULL, " +
                "courier_fee INT NOT NULL, " +
                "pay_price INT NOT NULL, " +
                "courier_id INT, " +
                "status VARCHAR(50) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public void addOrder(Order order) throws SQLException {
        String sql = "INSERT INTO orders (delivery_address, customer_id, vendor_id, coupon_id, items, raw_price, tax_fee, additional_fee, courier_fee, pay_price, courier_id, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.getDeliveryAddress());
            ps.setInt(2, order.getCustomerId());
            ps.setInt(3, order.getVendorId());
            if (order.getCouponId() != null) ps.setInt(4, order.getCouponId()); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, mapper.writeValueAsString(order.getItems()));
            ps.setInt(6, order.getRawPrice());
            ps.setInt(7, order.getTaxFee());
            ps.setInt(8, order.getAdditionalFee());
            ps.setInt(9, order.getCourierFee());
            ps.setInt(10, order.getPayPrice());
            if (order.getCourierId() != null) ps.setInt(11, order.getCourierId()); else ps.setNull(11, Types.INTEGER);
            ps.setString(12, order.getStatus());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating order failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    order.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }
        } catch (Exception e) {
            throw new SQLException("Failed to add order: " + e.getMessage(), e);
        }
    }

    public Order getOrderById(int id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
                }
            }
        }
        return null;
    }

    public List<Order> getOrdersByCustomer(int customerId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToOrder(rs));
                }
            }
        }
        return orders;
    }

    // Returns all orders for a given restaurant (vendor)
    public List<Order> getOrdersByVendor(int vendorId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE vendor_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToOrder(rs));
                }
            }
        }
        return orders;
    }

    // Updates the status of an order
    public void updateOrderStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
    }

    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getInt("id"));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        order.setCustomerId(rs.getInt("customer_id"));
        order.setVendorId(rs.getInt("vendor_id"));
        int couponId = rs.getInt("coupon_id");
        order.setCouponId(rs.wasNull() ? null : couponId);
        try {
            String itemsJson = rs.getString("items");
            List<OrderItem> items = itemsJson != null && !itemsJson.isEmpty() ?
                Arrays.asList(mapper.readValue(itemsJson, OrderItem[].class)) : new ArrayList<>();
            order.setItems(items);
        } catch (Exception e) {
            order.setItems(new ArrayList<>());
        }
        order.setRawPrice(rs.getInt("raw_price"));
        order.setTaxFee(rs.getInt("tax_fee"));
        order.setAdditionalFee(rs.getInt("additional_fee"));
        order.setCourierFee(rs.getInt("courier_fee"));
        order.setPayPrice(rs.getInt("pay_price"));
        int courierId = rs.getInt("courier_id");
        order.setCourierId(rs.wasNull() ? null : courierId);
        order.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        order.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        Timestamp updated = rs.getTimestamp("updated_at");
        order.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);
        return order;
    }

    private String joinIntList(List<Integer> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    private List<Integer> splitIntList(String s) {
        List<Integer> list = new ArrayList<>();
        if (s == null || s.isEmpty()) return list;
        for (String part : s.split(",")) {
            try { list.add(Integer.parseInt(part)); } catch (NumberFormatException ignored) {}
        }
        return list;
    }
} 