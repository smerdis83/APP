package com.example.foodapp.model.entity;

public class ExtraExpense {
    private int id;
    private int restaurantId;
    private String name;
    private int amount;

    public ExtraExpense() {}

    public ExtraExpense(int id, int restaurantId, String name, int amount) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.amount = amount;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRestaurantId() { return restaurantId; }
    public void setRestaurantId(int restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}