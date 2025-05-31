package com.example.foodapp.model.entity;

import java.time.LocalDateTime;

public class Restaurant {
    private int id;
    private String name;
    private String address;
    private String phone;
    private int ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Empty constructor
    public Restaurant() { }

    // Constructor for creating a new restaurant (before we know id, timestamps)
    public Restaurant(String name, String address, String phone, int ownerId) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.ownerId = ownerId;
    }

    // Full constructor (including id & timestamps)
    public Restaurant(int id, String name, String address, String phone,
                      int ownerId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters & setters
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Restaurant{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", address='" + address + '\'' +
               ", phone='" + phone + '\'' +
               ", ownerId=" + ownerId +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               '}';
    }
} 