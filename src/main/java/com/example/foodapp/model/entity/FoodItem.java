package com.example.foodapp.model.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public class FoodItem {
    private int id;
    private String name;
    private String description;
    private int price;
    private int supply;
    private List<String> keywords;
    @JsonAlias({"vendor_id", "vendorId"})
    private int vendorId; // restaurant/owner id
    @JsonAlias({"image_base64", "imageBase64"})
    private String imageBase64;

    public FoodItem() {}

    public FoodItem(int id, String name, String description, int price, int supply, List<String> keywords, int vendorId, String imageBase64) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.supply = supply;
        this.keywords = keywords;
        this.vendorId = vendorId;
        this.imageBase64 = imageBase64;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public int getSupply() { return supply; }
    public void setSupply(int supply) { this.supply = supply; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public int getVendorId() { return vendorId; }
    public void setVendorId(int vendorId) { this.vendorId = vendorId; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    @Override
    public String toString() {
        return "FoodItem{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", supply=" + supply +
                ", keywords=" + keywords +
                ", vendorId=" + vendorId +
                ", imageBase64='" + (imageBase64 != null ? "[base64]" : null) + '\'' +
                '}';
    }
} 