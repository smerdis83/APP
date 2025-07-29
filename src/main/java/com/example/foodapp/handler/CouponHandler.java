package com.example.foodapp.handler;

import com.example.foodapp.dao.CouponDao;
import com.example.foodapp.model.entity.Coupon;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CouponHandler {
    private final CouponDao couponDao;

    public CouponHandler() {
        this.couponDao = new CouponDao();
    }

    public CouponHandler(CouponDao couponDao) {
        this.couponDao = couponDao;
    }

    public void createCoupon(Coupon coupon) throws SQLException {
        couponDao.createCoupon(coupon);
    }

    public Coupon getCouponById(int id) throws SQLException {
        return couponDao.findById(id);
    }

    public Coupon getCouponByCode(String code) throws SQLException {
        return couponDao.findByCode(code);
    }

    public List<Coupon> getAllCoupons() throws SQLException {
        return couponDao.findAll();
    }

    public void updateCoupon(Coupon coupon) throws SQLException {
        couponDao.updateCoupon(coupon);
    }

    public void deleteCoupon(int id) throws SQLException {
        couponDao.deleteCoupon(id);
    }

    /**
     * Validates a coupon for a given order price and current date.
     * Returns null if invalid, or the Coupon if valid.
     */
    public Coupon validateCoupon(String code, int orderPrice) throws SQLException {
        Coupon coupon = couponDao.findByCode(code);
        if (coupon == null) return null;
        
        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getStartDate()) || today.isAfter(coupon.getEndDate())) {
            return null; // expired or not started
        }
        
        if (orderPrice < coupon.getMinPrice()) {
            return null; // order too cheap
        }
        
        if (coupon.getUserCount() <= 0) {
            return null; // no more uses left
        }
        
        return coupon;
    }

    /**
     * Validates a coupon for a specific user, checking if they've reached their usage limit
     */
    public Coupon validateCouponForUser(String code, int orderPrice, int userId) throws SQLException {
        Coupon coupon = validateCoupon(code, orderPrice);
        if (coupon == null) return null;
        
        // Check if user has reached their usage limit for this coupon
        int userUsageCount = couponDao.getUserUsageCount(coupon.getId(), userId);
        if (userUsageCount >= coupon.getMaxUsesPerUser()) {
            return null; // user has reached their usage limit
        }
        
        return coupon;
    }

    /**
     * Records coupon usage and decrements the user count after successful order
     */
    public void recordCouponUsage(int couponId, int userId) throws SQLException {
        int userUsageCount = couponDao.getUserUsageCount(couponId, userId);
        couponDao.recordCouponUsage(couponId, userId);
        if (userUsageCount == 0) {
            // This is the user's first use, decrement the global count
            couponDao.decrementUserCount(couponId);
        }
    }

    /**
     * Calculates the discount for a given coupon and order price.
     */
    public int calculateDiscount(Coupon coupon, int orderPrice) {
        if (coupon.getType().equals("fixed")) {
            return (int) Math.min(orderPrice, coupon.getValue());
        } else if (coupon.getType().equals("percent")) {
            return (int) Math.round(orderPrice * (coupon.getValue() / 100.0));
        }
        return 0;
    }
} 