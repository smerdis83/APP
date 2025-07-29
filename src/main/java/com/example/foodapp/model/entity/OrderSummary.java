package com.example.foodapp.model.entity;

public class OrderSummary {
    public final String id, status, payPrice;
    public OrderSummary(String id, String status, String payPrice) {
        this.id = id; this.status = status; this.payPrice = payPrice;
    }
    @Override public String toString() { return "Order #" + id + " | Status: " + status + " | Total: " + payPrice; }
} 