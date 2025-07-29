package com.example.foodapp;

import com.example.foodapp.handler.AnalyticsHandler;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.model.entity.FoodItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;

public class AnalyticsDebugTest {
    
    private AnalyticsHandler analyticsHandler;
    private ObjectMapper mapper;
    
    @BeforeEach
    void setUp() {
        analyticsHandler = new AnalyticsHandler();
        mapper = new ObjectMapper();
    }
    
    @Test
    void testDebugAnalyticsOutput() {
        // Create test orders with food items
        List<Order> testOrders = createTestOrders();
        
        try {
            // Use reflection to access the private method for testing
            java.lang.reflect.Method method = AnalyticsHandler.class.getDeclaredMethod(
                "calculateTopSellingFoods", List.class, int.class);
            method.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(
                analyticsHandler, testOrders, 0);
            
            // Print the raw result
            System.out.println("=== RAW ANALYTICS RESULT ===");
            for (Map<String, Object> food : result) {
                System.out.println("Food: " + food);
            }
            
            // Test JSON serialization
            System.out.println("=== JSON SERIALIZATION TEST ===");
            String json = mapper.writeValueAsString(result);
            System.out.println("JSON: " + json);
            
            // Test the specific fields
            for (Map<String, Object> food : result) {
                String name = (String) food.get("name");
                Integer salesCount = (Integer) food.get("sales_count");
                Integer revenue = (Integer) food.get("revenue");
                
                System.out.println("Name: '" + name + "'");
                System.out.println("Sales Count: " + salesCount);
                System.out.println("Revenue: " + revenue);
                System.out.println("---");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    private List<Order> createTestOrders() {
        List<Order> orders = new ArrayList<>();
        
        // Create test order 1 with "ghorme"
        Order order1 = new Order();
        order1.setId(1);
        order1.setVendorId(1);
        order1.setCreatedAt(LocalDateTime.now());
        order1.setPayPrice(10000); // Set a price for revenue calculation
        
        List<OrderItem> items1 = new ArrayList<>();
        OrderItem item1 = new OrderItem();
        item1.setItem_id(1); // This will be "ghorme"
        item1.setQuantity(2);
        items1.add(item1);
        
        order1.setItems(items1);
        orders.add(order1);
        
        // Create test order 2 with "ghorme" again
        Order order2 = new Order();
        order2.setId(2);
        order2.setVendorId(1);
        order2.setCreatedAt(LocalDateTime.now());
        order2.setPayPrice(15000);
        
        List<OrderItem> items2 = new ArrayList<>();
        OrderItem item2 = new OrderItem();
        item2.setItem_id(1); // Same "ghorme" item
        item2.setQuantity(3);
        items2.add(item2);
        
        order2.setItems(items2);
        orders.add(order2);
        
        return orders;
    }
}