package com.example.foodapp;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.FoodItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class OrderDataTest {
    
    private OrderDao orderDao;
    private FoodItemDao foodItemDao;
    
    @BeforeEach
    void setUp() {
        orderDao = new OrderDao();
        foodItemDao = new FoodItemDao();
    }
    
    @Test
    void testListAllOrders() {
        try {
            List<Order> allOrders = orderDao.getAllOrders();
            
            System.out.println("=== ALL ORDERS IN DATABASE ===");
            System.out.println("Total orders: " + allOrders.size());
            
            for (Order order : allOrders) {
                System.out.println("Order ID: " + order.getId() + 
                                 " | Vendor: " + order.getVendorId() + 
                                 " | Status: " + order.getStatus() + 
                                 " | Pay Price: " + order.getPayPrice() +
                                 " | Items: " + order.getItems().size());
                
                for (com.example.foodapp.model.entity.OrderItem item : order.getItems()) {
                    System.out.println("  - Item ID: " + item.getItem_id() + 
                                     " | Quantity: " + item.getQuantity());
                    
                    // Try to get the food item name
                    try {
                        FoodItem foodItem = foodItemDao.getFoodItemById(item.getItem_id());
                        if (foodItem != null) {
                            System.out.println("    Food Name: '" + foodItem.getName() + "'");
                        } else {
                            System.out.println("    Food Name: NOT FOUND (ID " + item.getItem_id() + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("    Food Name: ERROR getting food item " + item.getItem_id());
                    }
                }
                System.out.println("---");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    @Test
    void testOrdersByVendor() {
        try {
            // Test orders for vendor 19 (which has POLO and ghorme)
            List<Order> vendor19Orders = orderDao.getOrdersByVendor(19);
            
            System.out.println("=== ORDERS FOR VENDOR 19 ===");
            System.out.println("Total orders: " + vendor19Orders.size());
            
            for (Order order : vendor19Orders) {
                System.out.println("Order ID: " + order.getId() + 
                                 " | Status: " + order.getStatus() + 
                                 " | Pay Price: " + order.getPayPrice() +
                                 " | Items: " + order.getItems().size());
                
                for (com.example.foodapp.model.entity.OrderItem item : order.getItems()) {
                    System.out.println("  - Item ID: " + item.getItem_id() + 
                                     " | Quantity: " + item.getQuantity());
                    
                    // Try to get the food item name
                    try {
                        FoodItem foodItem = foodItemDao.getFoodItemById(item.getItem_id());
                        if (foodItem != null) {
                            System.out.println("    Food Name: '" + foodItem.getName() + "'");
                        } else {
                            System.out.println("    Food Name: NOT FOUND (ID " + item.getItem_id() + ")");
                        }
                    } catch (Exception e) {
                        System.out.println("    Food Name: ERROR getting food item " + item.getItem_id());
                    }
                }
                System.out.println("---");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}