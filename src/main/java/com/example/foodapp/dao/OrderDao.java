package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.model.entity.OrderStatusHistory;
import com.example.foodapp.model.entity.Transaction;
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
            createOrderStatusHistoryTable();
            createTransactionTable();
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

    // Returns all available deliveries for couriers (status = 'served' and courier_id IS NULL)
    public List<Order> getAvailableDeliveries() throws SQLException {
        String sql = "SELECT * FROM orders WHERE status = ? AND courier_id IS NULL ORDER BY created_at ASC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "served"); // or 'ready' if that's your status
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToOrder(rs));
                }
            }
        }
        return orders;
    }

    // Assigns a courier to an order and updates status, only if available
    public Order assignCourierToOrder(int orderId, int courierId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET courier_id = ?, status = ? WHERE id = ? AND courier_id IS NULL AND status = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courierId);
            ps.setString(2, newStatus);
            ps.setInt(3, orderId);
            ps.setString(4, "served");
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // Either not found, already assigned, or wrong status
                return null;
            }
        }
        // Return the updated order
        return getOrderById(orderId);
    }

    // Updates courier status to 'received' or 'delivered' if courier is assigned and current status is correct
    public Order updateCourierOrderStatus(int orderId, int courierId, String newStatus) throws SQLException {
        String requiredCurrentStatus = null;
        if ("received".equals(newStatus)) requiredCurrentStatus = "accepted";
        else if ("delivered".equals(newStatus)) requiredCurrentStatus = "received";
        else return null;
        String sql = "UPDATE orders SET status = ? WHERE id = ? AND courier_id = ? AND status = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            ps.setInt(3, courierId);
            ps.setString(4, requiredCurrentStatus);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                return null;
            }
        }
        return getOrderById(orderId);
    }

    // Returns all orders for a given courier
    public List<Order> getOrdersByCourier(int courierId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE courier_id = ? ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToOrder(rs));
                }
            }
        }
        return orders;
    }

    public void createOrderStatusHistoryTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS order_status_history (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "order_id INT NOT NULL, " +
                "status VARCHAR(50) NOT NULL, " +
                "changed_by VARCHAR(20) NOT NULL, " +
                "changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public void insertOrderStatusHistory(int orderId, String status, String changedBy) throws SQLException {
        String sql = "INSERT INTO order_status_history (order_id, status, changed_by) VALUES (?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, status);
            ps.setString(3, changedBy);
            ps.executeUpdate();
        }
    }

    public List<OrderStatusHistory> getOrderStatusHistory(int orderId) throws SQLException {
        String sql = "SELECT * FROM order_status_history WHERE order_id = ? ORDER BY changed_at ASC, id ASC";
        List<OrderStatusHistory> history = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderStatusHistory h = new OrderStatusHistory();
                    h.setId(rs.getInt("id"));
                    h.setOrderId(rs.getInt("order_id"));
                    h.setStatus(rs.getString("status"));
                    h.setChangedBy(rs.getString("changed_by"));
                    Timestamp changedAt = rs.getTimestamp("changed_at");
                    h.setChangedAt(changedAt != null ? changedAt.toLocalDateTime() : null);
                    history.add(h);
                }
            }
        }
        return history;
    }

    public void createTransactionTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "order_id INT, " +
                "user_id INT NOT NULL, " +
                "method VARCHAR(20) NOT NULL, " +
                "type VARCHAR(20) NOT NULL, " +
                "amount INT NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public void insertTransaction(Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions (order_id, user_id, method, type, amount, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (tx.getOrderId() != null) ps.setInt(1, tx.getOrderId()); else ps.setNull(1, java.sql.Types.INTEGER);
            ps.setInt(2, tx.getUserId());
            ps.setString(3, tx.getMethod());
            ps.setString(4, tx.getType());
            ps.setInt(5, tx.getAmount());
            ps.setString(6, tx.getStatus());
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating transaction failed, no rows affected.");
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) tx.setId(generatedKeys.getInt(1));
            }
        }
    }

    public List<Transaction> getTransactionsByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY created_at DESC";
        List<Transaction> txs = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) txs.add(mapRowToTransaction(rs));
            }
        }
        return txs;
    }

    public List<Transaction> getAllTransactions() throws SQLException {
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        List<Transaction> txs = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) txs.add(mapRowToTransaction(rs));
            }
        }
        return txs;
    }

    public List<Transaction> getTransactionsByOrder(int orderId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE order_id = ? ORDER BY created_at DESC";
        List<Transaction> txs = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) txs.add(mapRowToTransaction(rs));
            }
        }
        return txs;
    }

    public List<Order> getAllOrders() throws SQLException {
        String sql = "SELECT * FROM orders ORDER BY created_at DESC";
        List<Order> orders = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRowToOrder(rs));
                }
            }
        }
        return orders;
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

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getInt("id"));
        int orderId = rs.getInt("order_id");
        tx.setOrderId(rs.wasNull() ? null : orderId);
        tx.setUserId(rs.getInt("user_id"));
        tx.setMethod(rs.getString("method"));
        tx.setType(rs.getString("type"));
        tx.setAmount(rs.getInt("amount"));
        tx.setStatus(rs.getString("status"));
        Timestamp created = rs.getTimestamp("created_at");
        tx.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        return tx;
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