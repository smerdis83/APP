package com.example.foodapp;

import com.example.foodapp.controller.SalesAnalyticsController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class FrontendParsingTest {
    
    private SalesAnalyticsController controller;
    
    @BeforeEach
    void setUp() {
        controller = new SalesAnalyticsController();
    }
    
    @Test
    void testFrontendParsing() {
        // Create a sample JSON response that matches what the backend sends
        String sampleJson = """
            {
                "summary": {
                    "total_revenue": 23682417,
                    "net_income": 23682417,
                    "total_orders": 30,
                    "successful_orders": 15,
                    "success_rate": 50.0,
                    "avg_order_value": 789414.0,
                    "total_taxes": 0,
                    "total_fees": 0
                },
                "top_selling_foods": [
                    {
                        "item_id": 17,
                        "name": "ghorme",
                        "sales_count": 29,
                        "revenue": 15845925
                    },
                    {
                        "item_id": 13,
                        "name": "POLO",
                        "sales_count": 1,
                        "revenue": 7837492
                    }
                ],
                "sales_trends": {},
                "order_status_distribution": {},
                "recent_orders": []
            }
            """;
        
        try {
            // Use reflection to access the private method for testing
            java.lang.reflect.Method method = SalesAnalyticsController.class.getDeclaredMethod(
                "updateBestSellingFoods", String.class);
            method.setAccessible(true);
            
            // Call the method
            method.invoke(controller, sampleJson);
            
            // Get the results using reflection
            java.lang.reflect.Field bestSellingFoodsField = SalesAnalyticsController.class.getDeclaredField("bestSellingFoods");
            bestSellingFoodsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            javafx.collections.ObservableList<String> results = (javafx.collections.ObservableList<String>) bestSellingFoodsField.get(controller);
            
            System.out.println("=== FRONTEND PARSING TEST RESULTS ===");
            System.out.println("Number of items parsed: " + results.size());
            
            for (int i = 0; i < results.size(); i++) {
                String item = results.get(i);
                System.out.println("Item " + (i + 1) + ": " + item);
            }
            
            // Verify that we got the expected results
            assertTrue(results.size() >= 2, "Should have at least 2 food items");
            
            // Check that ghorme is first (29 orders)
            String firstItem = results.get(0);
            assertTrue(firstItem.contains("ghorme"), "First item should be ghorme");
            assertTrue(firstItem.contains("29 orders"), "ghorme should have 29 orders");
            
            // Check that POLO is second (1 order)
            String secondItem = results.get(1);
            assertTrue(secondItem.contains("POLO"), "Second item should be POLO");
            assertTrue(secondItem.contains("1 orders"), "POLO should have 1 order");
            
            System.out.println("✅ Frontend parsing test passed!");
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    @Test
    void testExtractIntValue() {
        try {
            // Use reflection to access the private method for testing
            java.lang.reflect.Method method = SalesAnalyticsController.class.getDeclaredMethod(
                "extractIntValue", String.class, String.class);
            method.setAccessible(true);
            
            // Test cases
            String json1 = "{\"name\":\"ghorme\",\"sales_count\":29,\"revenue\":15845925}";
            String json2 = "{\"name\":\"POLO\",\"sales_count\":1,\"revenue\":7837492}";
            
            int salesCount1 = (Integer) method.invoke(controller, json1, "sales_count");
            int salesCount2 = (Integer) method.invoke(controller, json2, "sales_count");
            
            System.out.println("Extracted sales_count from json1: " + salesCount1);
            System.out.println("Extracted sales_count from json2: " + salesCount2);
            
            assertEquals(29, salesCount1, "Should extract 29 from first JSON");
            assertEquals(1, salesCount2, "Should extract 1 from second JSON");
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}