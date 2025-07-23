package com.example.foodapp.model.entity;

import java.time.LocalDateTime;

public class OrderStatusHistory {
    private int id;
    private int orderId;
    private String status;
    private String changedBy;
    private LocalDateTime changedAt;

    public OrderStatusHistory() {}

    public OrderStatusHistory(int id, int orderId, String status, String changedBy, LocalDateTime changedAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
} 