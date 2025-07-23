package com.example.foodapp.model.entity;

import java.time.LocalDateTime;

public class Transaction {
    private int id;
    private Integer orderId;
    private int userId;
    private String method; // wallet, online, etc.
    private String type;   // payment, top-up, refund, payout, etc.
    private int amount;
    private String status; // success, failed
    private LocalDateTime createdAt;

    public Transaction() {}

    public Transaction(int id, Integer orderId, int userId, String method, String type, int amount, String status, LocalDateTime createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.method = method;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 