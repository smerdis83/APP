package com.example.foodapp.model.entity;

public class OrderItem {
    private int item_id;
    private int quantity;

    public OrderItem() {}

    public OrderItem(int item_id, int quantity) {
        this.item_id = item_id;
        this.quantity = quantity;
    }

    public int getItem_id() { return item_id; }
    public void setItem_id(int item_id) { this.item_id = item_id; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
} 