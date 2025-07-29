package com.example.foodapp;

import com.example.foodapp.handler.AnalyticsHandler;
import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.RestaurantDao;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.Restaurant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.*;

public class RealDataAnalyticsTest {
    
    private AnalyticsHandler analyticsHandler;
    private OrderDao orderDao;
    private RestaurantDao restaurantDao;
    
    @BeforeEach
    void setUp() {
        analyticsHandler = new AnalyticsHandler();
        orderDao = new OrderDao();
        restaurantDao = new RestaurantDao();
    }
    
    @Test
    void testRealAnalyticsForVendor19() {
        try {
            // Get all orders for vendor 19
            List<Order> vendor19Orders = orderDao.getOrdersByVendor(19);
            
            System.out.println("=== REAL ANALYTICS FOR VENDOR 19 ===");
            System.out.println("Total orders for vendor 19: " + vendor19Orders.size());
            
            // Use reflection to access the private method for testing
            java.lang.reflect.Method method = AnalyticsHandler.class.getDeclaredMethod(
                "calculateTopSellingFoods", List.class, int.class);
            method.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(
                analyticsHandler, vendor19Orders, 19);
            
            System.out.println("Analytics result: " + result.size() + " food items");
            
            for (Map<String, Object> food : result) {
                String name = (String) food.get("name");
                Integer salesCount = (Integer) food.get("sales_count");
                Integer revenue = (Integer) food.get("revenue");
                
                System.out.println("Food: '" + name + "' - Sales Count: " + salesCount + " - Revenue: " + revenue);
            }
            
            // Verify that ghorme has orders
            boolean foundGhorme = false;
            for (Map<String, Object> food : result) {
                String name = (String) food.get("name");
                if ("ghorme".equals(name)) {
                    Integer salesCount = (Integer) food.get("sales_count");
                    System.out.println("FOUND ghorme with sales count: " + salesCount);
                    foundGhorme = true;
                    assertTrue(salesCount > 0, "ghorme should have sales count > 0");
                    break;
                }
            }
            
            assertTrue(foundGhorme, "ghorme should be found in analytics");
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    @Test
    void testDateFilteredAnalytics() {
        try {
            // Get all orders for vendor 19
            List<Order> vendor19Orders = orderDao.getOrdersByVendor(19);
            
            // Filter by recent date range (last 30 days)
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(30);
            
            List<Order> filteredOrders = vendor19Orders.stream()
                .filter(order -> {
                    LocalDate orderDate = order.getCreatedAt().toLocalDate();
                    return !orderDate.isBefore(startDate) && !orderDate.isAfter(endDate);
                })
                .collect(java.util.stream.Collectors.toList());
            
            System.out.println("=== DATE FILTERED ANALYTICS ===");
            System.out.println("Date range: " + startDate + " to " + endDate);
            System.out.println("Total orders before filtering: " + vendor19Orders.size());
            System.out.println("Total orders after filtering: " + filteredOrders.size());
            
            // Use reflection to access the private method for testing
            java.lang.reflect.Method method = AnalyticsHandler.class.getDeclaredMethod(
                "calculateTopSellingFoods", List.class, int.class);
            method.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> result = (List<Map<String, Object>>) method.invoke(
                analyticsHandler, filteredOrders, 19);
            
            System.out.println("Analytics result: " + result.size() + " food items");
            
            for (Map<String, Object> food : result) {
                String name = (String) food.get("name");
                Integer salesCount = (Integer) food.get("sales_count");
                Integer revenue = (Integer) food.get("revenue");
                
                System.out.println("Food: '" + name + "' - Sales Count: " + salesCount + " - Revenue: " + revenue);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}