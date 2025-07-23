package com.example.foodapp.model.entity;

import java.util.List;

public class Menu {
    private int id;
    private int restaurantId;
    private String title;
    private List<Integer> itemIds;

    public Menu() {}

    public Menu(int id, int restaurantId, String title, List<Integer> itemIds) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.title = title;
        this.itemIds = itemIds;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRestaurantId() { return restaurantId; }
    public void setRestaurantId(int restaurantId) { this.restaurantId = restaurantId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<Integer> getItemIds() { return itemIds; }
    public void setItemIds(List<Integer> itemIds) { this.itemIds = itemIds; }

    @Override
    public String toString() {
        return "Menu{" +
                "id=" + id +
                ", restaurantId=" + restaurantId +
                ", title='" + title + '\'' +
                ", itemIds=" + itemIds +
                '}';
    }
} 