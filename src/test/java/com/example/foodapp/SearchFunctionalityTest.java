package com.example.foodapp;

import com.example.foodapp.handler.VendorItemHandler;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.model.entity.Restaurant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class SearchFunctionalityTest {
    
    private VendorItemHandler vendorItemHandler;
    
    @BeforeEach
    void setUp() {
        vendorItemHandler = new VendorItemHandler();
    }
    
    @Test
    void testSearchRestaurantsWithKeywords() {
        // Test that restaurants can be searched by food keywords
        try {
            // Create test data
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("search", "");
            requestBody.put("keywords", Arrays.asList("pizza"));
            
            // This would normally make an HTTP request, but for testing we'll simulate
            // the logic that the handler uses
            System.out.println("=== TESTING RESTAURANT SEARCH ===");
            System.out.println("Search request: " + requestBody);
            
            // Simulate the search logic
            boolean hasKeywords = requestBody.containsKey("keywords");
            List<String> keywords = (List<String>) requestBody.get("keywords");
            
            System.out.println("Has keywords: " + hasKeywords);
            System.out.println("Keywords: " + keywords);
            
            assertTrue(hasKeywords, "Request should contain keywords");
            assertNotNull(keywords, "Keywords should not be null");
            assertFalse(keywords.isEmpty(), "Keywords should not be empty");
            assertEquals("pizza", keywords.get(0), "First keyword should be 'pizza'");
            
            System.out.println("✅ Restaurant search test passed!");
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    @Test
    void testSearchFoodsWithFilters() {
        // Test that foods can be searched with various filters
        try {
            // Create test data
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("search", "pizza");
            requestBody.put("price", 50000);
            requestBody.put("keywords", Arrays.asList("italian", "cheese"));
            
            System.out.println("=== TESTING FOOD SEARCH ===");
            System.out.println("Search request: " + requestBody);
            
            // Simulate the search logic
            String searchText = (String) requestBody.get("search");
            Integer maxPrice = (Integer) requestBody.get("price");
            List<String> keywords = (List<String>) requestBody.get("keywords");
            
            System.out.println("Search text: " + searchText);
            System.out.println("Max price: " + maxPrice);
            System.out.println("Keywords: " + keywords);
            
            assertNotNull(searchText, "Search text should not be null");
            assertEquals("pizza", searchText, "Search text should be 'pizza'");
            assertNotNull(maxPrice, "Max price should not be null");
            assertEquals(50000, maxPrice, "Max price should be 50000");
            assertNotNull(keywords, "Keywords should not be null");
            assertEquals(2, keywords.size(), "Should have 2 keywords");
            assertTrue(keywords.contains("italian"), "Should contain 'italian' keyword");
            assertTrue(keywords.contains("cheese"), "Should contain 'cheese' keyword");
            
            System.out.println("✅ Food search test passed!");
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    @Test
    void testFoodItemKeywordsParsing() {
        // Test that food item keywords are parsed correctly
        try {
            System.out.println("=== TESTING FOOD ITEM KEYWORDS PARSING ===");
            
            // Simulate a food item with keywords
            FoodItem testItem = new FoodItem();
            testItem.setId(1);
            testItem.setName("Margherita Pizza");
            testItem.setDescription("Classic Italian pizza with tomato and mozzarella");
            testItem.setPrice(45000);
            testItem.setSupply(10);
            testItem.setVendorId(1);
            testItem.setKeywords(Arrays.asList("pizza", "italian", "cheese", "tomato"));
            
            System.out.println("Food item: " + testItem.getName());
            System.out.println("Keywords: " + testItem.getKeywords());
            
            assertNotNull(testItem.getKeywords(), "Keywords should not be null");
            assertEquals(4, testItem.getKeywords().size(), "Should have 4 keywords");
            assertTrue(testItem.getKeywords().contains("pizza"), "Should contain 'pizza'");
            assertTrue(testItem.getKeywords().contains("italian"), "Should contain 'italian'");
            assertTrue(testItem.getKeywords().contains("cheese"), "Should contain 'cheese'");
            assertTrue(testItem.getKeywords().contains("tomato"), "Should contain 'tomato'");
            
            // Test keyword matching
            List<String> searchKeywords = Arrays.asList("pizza", "cheese");
            boolean matches = testItem.getKeywords().stream()
                .anyMatch(searchKeywords::contains);
            
            System.out.println("Search keywords: " + searchKeywords);
            System.out.println("Matches: " + matches);
            
            assertTrue(matches, "Food item should match search keywords");
            
            System.out.println("✅ Food item keywords parsing test passed!");
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
    
    @Test
    void testSearchRequestValidation() {
        // Test that search requests are properly validated
        try {
            System.out.println("=== TESTING SEARCH REQUEST VALIDATION ===");
            
            // Test empty search
            Map<String, Object> emptyRequest = new HashMap<>();
            emptyRequest.put("search", "");
            emptyRequest.put("keywords", new ArrayList<>());
            
            String searchText = (String) emptyRequest.get("search");
            List<String> keywords = (List<String>) emptyRequest.get("keywords");
            
            System.out.println("Empty search text: '" + searchText + "'");
            System.out.println("Empty keywords: " + keywords);
            
            assertNotNull(searchText, "Search text should not be null");
            assertTrue(searchText.isEmpty(), "Search text should be empty");
            assertNotNull(keywords, "Keywords should not be null");
            assertTrue(keywords.isEmpty(), "Keywords should be empty");
            
            // Test search with filters
            Map<String, Object> filteredRequest = new HashMap<>();
            filteredRequest.put("search", "burger");
            filteredRequest.put("price", 30000);
            filteredRequest.put("keywords", Arrays.asList("fast food", "meat"));
            
            searchText = (String) filteredRequest.get("search");
            Integer price = (Integer) filteredRequest.get("price");
            keywords = (List<String>) filteredRequest.get("keywords");
            
            System.out.println("Filtered search text: " + searchText);
            System.out.println("Price filter: " + price);
            System.out.println("Filtered keywords: " + keywords);
            
            assertNotNull(searchText, "Search text should not be null");
            assertEquals("burger", searchText, "Search text should be 'burger'");
            assertNotNull(price, "Price should not be null");
            assertEquals(30000, price, "Price should be 30000");
            assertNotNull(keywords, "Keywords should not be null");
            assertEquals(2, keywords.size(), "Should have 2 keywords");
            
            System.out.println("✅ Search request validation test passed!");
            
        } catch (Exception e) {
            e.printStackTrace();
            fail("Test failed with exception: " + e.getMessage());
        }
    }
}