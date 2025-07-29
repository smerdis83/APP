package com.example.foodapp;

import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.model.entity.FoodItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class DatabaseFoodItemsTest {
    
    private FoodItemDao foodItemDao;
    
    @BeforeEach
    void setUp() {
        foodItemDao = new FoodItemDao();
    }
    
    @Test
    void testListAllFoodItems() {
        try {
            List<FoodItem> allFoodItems = foodItemDao.getAllFoodItems();
            
            System.out.println("=== ALL FOOD ITEMS IN DATABASE ===");
            System.out.println("Total food items: " + allFoodItems.size());
            
            for (FoodItem food : allFoodItems) {
                System.out.println("ID: " + food.getId() + 
                                 " | Name: '" + food.getName() + "'" + 
                                 " | Vendor: " + food.getVendorId() + 
                                 " | Price: " + food.getPrice());
            }
            
            // Look specifically for "ghorme"
            System.out.println("\n=== SEARCHING FOR 'ghorme' ===");
            for (FoodItem food : allFoodItems) {
                if (food.getName().toLowerCase().contains("ghorme")) {
                    System.out.println("FOUND 'ghorme': ID=" + food.getId() + ", Name='" + food.getName() + "'");
                }
            }
            
            // Look for items with similar names
            System.out.println("\n=== ITEMS WITH SIMILAR NAMES ===");
            for (FoodItem food : allFoodItems) {
                if (food.getName().toLowerCase().contains("ghorm") || 
                    food.getName().toLowerCase().contains("polo")) {
                    System.out.println("Similar item: ID=" + food.getId() + ", Name='" + food.getName() + "'");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    @Test
    void testGetFoodItemById() {
        try {
            // Test getting food item by ID 1
            FoodItem food1 = foodItemDao.getFoodItemById(1);
            if (food1 != null) {
                System.out.println("Food item ID 1: " + food1.getName());
            } else {
                System.out.println("Food item ID 1: NOT FOUND");
            }
            
            // Test getting food item by ID 2
            FoodItem food2 = foodItemDao.getFoodItemById(2);
            if (food2 != null) {
                System.out.println("Food item ID 2: " + food2.getName());
            } else {
                System.out.println("Food item ID 2: NOT FOUND");
            }
            
            // Test getting food item by ID 3
            FoodItem food3 = foodItemDao.getFoodItemById(3);
            if (food3 != null) {
                System.out.println("Food item ID 3: " + food3.getName());
            } else {
                System.out.println("Food item ID 3: NOT FOUND");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}