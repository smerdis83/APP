package com.example.foodapp.dao;

import com.example.foodapp.model.entity.Coupon;
import com.example.foodapp.util.JdbcUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CouponDao {
    public CouponDao() {
        try {
            createCouponTable();
            createCouponUsageTable();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to ensure coupons table", e);
        }
    }

    public void createCouponTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS coupons (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "coupon_code VARCHAR(50) NOT NULL UNIQUE, " +
                "type ENUM('fixed', 'percent') NOT NULL, " +
                "value DOUBLE NOT NULL, " +
                "min_price INT NOT NULL, " +
                "user_count INT NOT NULL, " +
                "max_uses_per_user INT NOT NULL DEFAULT 1, " +
                "start_date DATE NOT NULL, " +
                "end_date DATE NOT NULL" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
        
        // Try to add the column for existing databases (ignore error if it already exists)
        try (Connection connection = JdbcUtil.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE coupons ADD COLUMN max_uses_per_user INT NOT NULL DEFAULT 1");
        } catch (SQLException e) {
            // 1060: Duplicate column name
            if (!e.getMessage().contains("Duplicate column name")) {
                throw e;
            }
        }
    }

    public void createCouponUsageTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS coupon_usage (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "coupon_id INT NOT NULL, " +
                "user_id INT NOT NULL, " +
                "used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE" +
                ")";
        try (Connection connection = JdbcUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }

    public void createCoupon(Coupon coupon) throws SQLException {
        String sql = "INSERT INTO coupons (coupon_code, type, value, min_price, user_count, max_uses_per_user, start_date, end_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, coupon.getCouponCode());
            ps.setString(2, coupon.getType());
            ps.setDouble(3, coupon.getValue());
            ps.setInt(4, coupon.getMinPrice());
            ps.setInt(5, coupon.getUserCount());
            ps.setInt(6, coupon.getMaxUsesPerUser());
            ps.setDate(7, Date.valueOf(coupon.getStartDate()));
            ps.setDate(8, Date.valueOf(coupon.getEndDate()));
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating coupon failed, no rows affected.");
            }
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    coupon.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating coupon failed, no ID obtained.");
                }
            }
        }
    }

    public Coupon findById(int id) throws SQLException {
        String sql = "SELECT * FROM coupons WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractCouponFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public Coupon findByCode(String code) throws SQLException {
        String sql = "SELECT * FROM coupons WHERE coupon_code = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractCouponFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public List<Coupon> findAll() throws SQLException {
        String sql = "SELECT * FROM coupons";
        List<Coupon> coupons = new ArrayList<>();
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                coupons.add(extractCouponFromResultSet(rs));
            }
        }
        return coupons;
    }

    public void updateCoupon(Coupon coupon) throws SQLException {
        String sql = "UPDATE coupons SET coupon_code=?, type=?, value=?, min_price=?, user_count=?, max_uses_per_user=?, start_date=?, end_date=? WHERE id=?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coupon.getCouponCode());
            ps.setString(2, coupon.getType());
            ps.setDouble(3, coupon.getValue());
            ps.setInt(4, coupon.getMinPrice());
            ps.setInt(5, coupon.getUserCount());
            ps.setInt(6, coupon.getMaxUsesPerUser());
            ps.setDate(7, Date.valueOf(coupon.getStartDate()));
            ps.setDate(8, Date.valueOf(coupon.getEndDate()));
            ps.setInt(9, coupon.getId());
            ps.executeUpdate();
        }
    }

    public void deleteCoupon(int id) throws SQLException {
        String sql = "DELETE FROM coupons WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Decrements the user count for a coupon after successful usage
     */
    public void decrementUserCount(int couponId) throws SQLException {
        String sql = "UPDATE coupons SET user_count = GREATEST(0, user_count - 1) WHERE id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.executeUpdate();
        }
    }

    /**
     * Records that a user has used a specific coupon
     */
    public void recordCouponUsage(int couponId, int userId) throws SQLException {
        String sql = "INSERT INTO coupon_usage (coupon_id, user_id) VALUES (?, ?)";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /**
     * Checks if a user has already used a specific coupon
     */
    public boolean hasUserUsedCoupon(int couponId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coupon_usage WHERE coupon_id = ? AND user_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Gets the number of times a user has used a specific coupon
     */
    public int getUserUsageCount(int couponId, int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM coupon_usage WHERE coupon_id = ? AND user_id = ?";
        try (Connection conn = JdbcUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, couponId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private Coupon extractCouponFromResultSet(ResultSet rs) throws SQLException {
        Coupon coupon = new Coupon();
        coupon.setId(rs.getInt("id"));
        coupon.setCouponCode(rs.getString("coupon_code"));
        coupon.setType(rs.getString("type"));
        coupon.setValue(rs.getDouble("value"));
        coupon.setMinPrice(rs.getInt("min_price"));
        coupon.setUserCount(rs.getInt("user_count"));
        coupon.setMaxUsesPerUser(rs.getInt("max_uses_per_user"));
        coupon.setStartDate(rs.getDate("start_date").toLocalDate());
        coupon.setEndDate(rs.getDate("end_date").toLocalDate());
        return coupon;
    }
} 