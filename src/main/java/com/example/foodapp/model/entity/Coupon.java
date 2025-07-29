package com.example.foodapp.model.entity;

import java.time.LocalDate;

public class Coupon {
    private int id;
    private String couponCode;
    private String type; // "fixed" or "percent"
    private double value;
    private int minPrice;
    private int userCount;
    private int maxUsesPerUser; // New field: how many times a single user can use this coupon
    private LocalDate startDate;
    private LocalDate endDate;

    public Coupon() {}

    public Coupon(int id, String couponCode, String type, double value, int minPrice, int userCount, int maxUsesPerUser, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.couponCode = couponCode;
        this.type = type;
        this.value = value;
        this.minPrice = minPrice;
        this.userCount = userCount;
        this.maxUsesPerUser = maxUsesPerUser;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public int getMinPrice() { return minPrice; }
    public void setMinPrice(int minPrice) { this.minPrice = minPrice; }

    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }

    public int getMaxUsesPerUser() { return maxUsesPerUser; }
    public void setMaxUsesPerUser(int maxUsesPerUser) { this.maxUsesPerUser = maxUsesPerUser; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    @Override
    public String toString() {
        return "Coupon{" +
                "id=" + id +
                ", couponCode='" + couponCode + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                ", minPrice=" + minPrice +
                ", userCount=" + userCount +
                ", maxUsesPerUser=" + maxUsesPerUser +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                '}';
    }
} 