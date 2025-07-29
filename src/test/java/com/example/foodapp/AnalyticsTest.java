package com.example.foodapp;

import com.example.foodapp.handler.AnalyticsHandler;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.model.entity.FoodItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;

public class AnalyticsTest {
    
    private AnalyticsHandler analyticsHandler;
    
    @BeforeEach
    void setUp() {
        analyticsHandler = new AnalyticsHandler();
    }
    
    @Test
    void testCalculateTopSellingFoods() {
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
            
            // Verify that we get results
            assertNotNull(result, "Result should not be null");
            assertFalse(result.isEmpty(), "Result should not be empty");
            
            // Verify that sales counts are not zero
            for (Map<String, Object> food : result) {
                Integer salesCount = (Integer) food.get("sales_count");
                assertNotNull(salesCount, "Sales count should not be null");
                assertTrue(salesCount > 0, "Sales count should be greater than 0");
                
                String name = (String) food.get("name");
                assertNotNull(name, "Food name should not be null");
                assertFalse(name.isEmpty(), "Food name should not be empty");
                
                System.out.println("Food: " + name + " - Sales: " + salesCount);
            }
            
        } catch (Exception e) {
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    private List<Order> createTestOrders() {
        List<Order> orders = new ArrayList<>();
        
        // Create test order 1
        Order order1 = new Order();
        order1.setId(1);
        order1.setVendorId(1);
        order1.setCreatedAt(LocalDateTime.now());
        
        List<OrderItem> items1 = new ArrayList<>();
        OrderItem item1 = new OrderItem();
        item1.setItem_id(1);
        item1.setQuantity(2);
        items1.add(item1);
        
        OrderItem item2 = new OrderItem();
        item2.setItem_id(2);
        item2.setQuantity(1);
        items1.add(item2);
        
        order1.setItems(items1);
        orders.add(order1);
        
        // Create test order 2
        Order order2 = new Order();
        order2.setId(2);
        order2.setVendorId(1);
        order2.setCreatedAt(LocalDateTime.now());
        
        List<OrderItem> items2 = new ArrayList<>();
        OrderItem item3 = new OrderItem();
        item3.setItem_id(1);
        item3.setQuantity(3);
        items2.add(item3);
        
        OrderItem item4 = new OrderItem();
        item4.setItem_id(3);
        item4.setQuantity(2);
        items2.add(item4);
        
        order2.setItems(items2);
        orders.add(order2);
        
        return orders;
    }
}