package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

public class SalesAnalyticsController {
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<RestaurantItem> restaurantCombo;
    @FXML private Button generateReportBtn;
    @FXML private Button backBtn;
    @FXML private Label netIncomeLabel;
    @FXML private Label totalOrdersLabel;
    @FXML private Label avgOrderValueLabel;
    @FXML private ListView<String> bestSellingFoodsList;
    @FXML private Label messageLabel;
    
    private String jwtToken;
    private int sellerId;
    private Runnable onBack;
    private ObservableList<String> bestSellingFoods = FXCollections.observableArrayList();
    private ObservableList<RestaurantItem> restaurants = FXCollections.observableArrayList();
    private boolean adminMode = false;
    public void setAdminMode(boolean adminMode) { this.adminMode = adminMode; }

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public void setOnBack(Runnable callback) { this.onBack = callback; }

    @FXML
    public void initialize() {
        // Initialize date pickers with default values (last 30 days)
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        
        // Initialize restaurant combo
        restaurantCombo.setItems(restaurants);
        restaurantCombo.setCellFactory(list -> new ListCell<RestaurantItem>() {
            @Override
            protected void updateItem(RestaurantItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name);
                }
            }
        });
        restaurantCombo.setButtonCell(restaurantCombo.getCellFactory().call(null));
        
        // Set up event handlers
        generateReportBtn.setOnAction(e -> generateReport());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        
        // Initialize lists
        bestSellingFoodsList.setItems(bestSellingFoods);
        
        // Load restaurants first
        loadRestaurants();
    }
    
    private void loadRestaurants() {
        new Thread(() -> {
            try {
                java.net.URL url = adminMode
                    ? new java.net.URL("http://localhost:8000/restaurants")
                    : new java.net.URL("http://localhost:8000/restaurants/mine");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String response;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    response = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200) {
                    List<RestaurantItem> loadedRestaurants = parseRestaurants(response);
                    Platform.runLater(() -> {
                        restaurants.setAll(loadedRestaurants);
                        if (!loadedRestaurants.isEmpty()) {
                            restaurantCombo.setValue(loadedRestaurants.get(0));
                            messageLabel.setText("Loaded " + loadedRestaurants.size() + " restaurants");
                        } else {
                            messageLabel.setText("No restaurants found");
                        }
                    });
                } else {
                    Platform.runLater(() -> messageLabel.setText("Failed to load restaurants: " + response));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> messageLabel.setText("Error loading restaurants: " + ex.getMessage()));
            }
        }).start();
    }
    
    private List<RestaurantItem> parseRestaurants(String json) {
        List<RestaurantItem> restaurants = new ArrayList<>();
        try {
            int idx = 0;
            while ((idx = json.indexOf("{", idx)) != -1) {
                int idIdx = json.indexOf("\"id\":", idx);
                int nameIdx = json.indexOf("\"name\":", idx);
                int ownerIdx = json.indexOf("\"owner_id\":", idx);
                if (idIdx == -1 || nameIdx == -1 || ownerIdx == -1) break;
                int idStart = idIdx + 5;
                int idEnd = json.indexOf(',', idStart);
                if (idEnd == -1) idEnd = json.indexOf('}', idStart);
                String idStr = json.substring(idStart, idEnd).replaceAll("[^0-9]", "").trim();
                int id = Integer.parseInt(idStr);
                int nameStart = json.indexOf('"', nameIdx + 7) + 1;
                int nameEnd = json.indexOf('"', nameStart);
                String name = json.substring(nameStart, nameEnd);
                // For admin, do not filter by owner
                restaurants.add(new RestaurantItem(id, name));
                idx = json.indexOf('}', idx) + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return restaurants;
    }
    
    private void generateReport() {
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        RestaurantItem selectedRestaurant = restaurantCombo.getValue();
        
        if (startDate == null || endDate == null) {
            messageLabel.setText("Please select both start and end dates");
            return;
        }
        
        if (selectedRestaurant == null) {
            messageLabel.setText("Please select a restaurant");
            return;
        }
        
        if (startDate.isAfter(endDate)) {
            messageLabel.setText("Start date cannot be after end date");
            return;
        }
        
        generateReportBtn.setDisable(true);
        messageLabel.setText("Generating report...");
        
        new Thread(() -> {
            try {
                fetchAnalyticsData(startDate, endDate, selectedRestaurant.id);
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    messageLabel.setText("Error: " + ex.getMessage());
                    generateReportBtn.setDisable(false);
                });
            }
        }).start();
    }
    
    private void fetchAnalyticsData(LocalDate startDate, LocalDate endDate, int restaurantId) {
        try {
            // Build request parameters
            String params = String.format("?seller_id=%d&restaurant_id=%d&start_date=%s&end_date=%s",
                sellerId, restaurantId, startDate, endDate);
            
            java.net.URL url = new java.net.URL("http://localhost:8000/analytics/sales" + params);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
            
            int code = conn.getResponseCode();
            String response;
            try (java.util.Scanner scanner = new java.util.Scanner(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    java.nio.charset.StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                response = scanner.hasNext() ? scanner.next() : "";
            }
            System.out.println("[DEBUG] /analytics/sales response code: " + code);
            System.out.println("[DEBUG] /analytics/sales response: " + response);
            
            if (code == 200) {
                Platform.runLater(() -> updateDashboard(response));
            } else {
                Platform.runLater(() -> {
                    messageLabel.setText("Failed to fetch data (code " + code + "): " + response);
                    generateReportBtn.setDisable(false);
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Platform.runLater(() -> {
                messageLabel.setText("Error: " + ex.getMessage());
                generateReportBtn.setDisable(false);
            });
        }
    }
    
    private void updateDashboard(String jsonData) {
        try {
            // Parse the JSON response using manual parsing (avoiding Jackson)
            updateSummaryCards(jsonData);
            updateBestSellingFoods(jsonData);
            
            messageLabel.setText("Report generated successfully!");
            generateReportBtn.setDisable(false);
        } catch (Exception ex) {
            messageLabel.setText("Error parsing data: " + ex.getMessage());
            generateReportBtn.setDisable(false);
        }
    }
    
    private void updateSummaryCards(String jsonData) {
        try {
            // Extract summary data from JSON
            int summaryStart = jsonData.indexOf("\"summary\":");
            if (summaryStart != -1) {
                int summaryEnd = jsonData.indexOf("}", summaryStart);
                String summaryJson = jsonData.substring(summaryStart, summaryEnd + 1);
                
                // Extract values
                int netIncome = extractIntValue(summaryJson, "net_income");
                int totalOrders = extractIntValue(summaryJson, "total_orders");
                double avgOrderValue = extractDoubleValue(summaryJson, "avg_order_value");
                
                netIncomeLabel.setText(String.format("Net Income: %,d Toman", netIncome));
                totalOrdersLabel.setText(String.format("Total Orders: %d", totalOrders));
                avgOrderValueLabel.setText(String.format("Avg Order: %.0f Toman", avgOrderValue));
            }
        } catch (Exception e) {
            // Fallback to default values
            netIncomeLabel.setText("Net Income: 0 Toman");
            totalOrdersLabel.setText("Total Orders: 0");
            avgOrderValueLabel.setText("Avg Order: 0 Toman");
        }
    }
    
    private void updateBestSellingFoods(String jsonData) {
        bestSellingFoods.clear();
        
        System.out.println("=== FRONTEND: UPDATE BEST SELLING FOODS ===");
        System.out.println("Received JSON data length: " + jsonData.length());
        System.out.println("JSON data preview: " + jsonData.substring(0, Math.min(500, jsonData.length())));
        
        try {
            // Extract top foods from JSON
            int foodsStart = jsonData.indexOf("\"top_selling_foods\":");
            System.out.println("Found 'top_selling_foods' at index: " + foodsStart);
            
            if (foodsStart != -1) {
                int foodsEnd = jsonData.indexOf("]", foodsStart);
                if (foodsEnd == -1) foodsEnd = jsonData.indexOf("}", foodsStart);
                System.out.println("Found foods end at index: " + foodsEnd);
                
                String foodsJson = jsonData.substring(foodsStart, foodsEnd + 1);
                System.out.println("Extracted foods JSON: " + foodsJson);
                
                // Parse the foods data and collect all items
                List<FoodItemData> allFoods = new ArrayList<>();
                
                // Find all food item objects in the JSON
                int currentIndex = 0;
                while (true) {
                    int itemStart = foodsJson.indexOf("{", currentIndex);
                    if (itemStart == -1) break;
                    
                    int itemEnd = foodsJson.indexOf("}", itemStart);
                    if (itemEnd == -1) break;
                    
                    String itemJson = foodsJson.substring(itemStart, itemEnd + 1);
                    System.out.println("Processing item JSON: " + itemJson);
                    
                    if (itemJson.contains("name") && itemJson.contains("sales_count") && itemJson.contains("revenue")) {
                        String name = extractStringValue(itemJson, "name");
                        int salesCount = extractIntValue(itemJson, "sales_count");
                        int revenue = extractIntValue(itemJson, "revenue");
                        
                        System.out.println("  Extracted - Name: '" + name + "', Sales: " + salesCount + ", Revenue: " + revenue);
                        
                        if (!name.isEmpty() && salesCount >= 0) {
                            allFoods.add(new FoodItemData(name, salesCount, revenue));
                            System.out.println("  Added food item: " + name);
                        }
                    }
                    
                    currentIndex = itemEnd + 1;
                }
                
                System.out.println("Total food items found: " + allFoods.size());
                
                // Sort by sales count (most to least)
                allFoods.sort((a, b) -> Integer.compare(b.salesCount, a.salesCount));
                
                // Add all items to the list
                for (int i = 0; i < allFoods.size(); i++) {
                    FoodItemData food = allFoods.get(i);
                    String displayText = String.format("%d. %s - %d orders - %,d Toman", 
                        i + 1, food.name, food.salesCount, food.revenue);
                    bestSellingFoods.add(displayText);
                    System.out.println("Added to list: " + displayText);
                }
                
                System.out.println("Final list size: " + bestSellingFoods.size());
            } else {
                System.out.println("ERROR: Could not find 'top_selling_foods' in JSON");
                System.out.println("Available keys in JSON:");
                String[] keys = {"summary", "sales_trends", "top_selling_foods", "order_status_distribution", "recent_orders"};
                for (String key : keys) {
                    int keyIndex = jsonData.indexOf("\"" + key + "\":");
                    System.out.println("  '" + key + "' found at: " + keyIndex);
                }
            }
            
            // If no data found, show nothing (no sample data)
            if (bestSellingFoods.isEmpty()) {
                System.out.println("No food data found - showing empty list");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error parsing food data: " + e.getMessage());
        }
        
        System.out.println("=== END FRONTEND: UPDATE BEST SELLING FOODS ===");
    }
    
    private int extractIntValue(String json, String key) {
        try {
            int keyIndex = json.indexOf("\"" + key + "\":");
            if (keyIndex != -1) {
                int valueStart = keyIndex + key.length() + 3;
                int valueEnd = json.indexOf(",", valueStart);
                if (valueEnd == -1) valueEnd = json.indexOf("}", valueStart);
                String value = json.substring(valueStart, valueEnd).trim();
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0;
    }
    
    private double extractDoubleValue(String json, String key) {
        try {
            int keyIndex = json.indexOf("\"" + key + "\":");
            if (keyIndex != -1) {
                int valueStart = keyIndex + key.length() + 3;
                int valueEnd = json.indexOf(",", valueStart);
                if (valueEnd == -1) valueEnd = json.indexOf("}", valueStart);
                String value = json.substring(valueStart, valueEnd).trim();
                return Double.parseDouble(value);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return 0.0;
    }
    
    private String extractStringValue(String json, String key) {
        try {
            int keyIndex = json.indexOf("\"" + key + "\":");
            if (keyIndex != -1) {
                int valueStart = json.indexOf("\"", keyIndex + key.length() + 3) + 1;
                int valueEnd = json.indexOf("\"", valueStart);
                return json.substring(valueStart, valueEnd);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "";
    }
    
    public static class RestaurantItem {
        public final int id;
        public final String name;
        
        public RestaurantItem(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }

    private static class FoodItemData {
        final String name;
        final int salesCount;
        final int revenue;
        
        FoodItemData(String name, int salesCount, int revenue) {
            this.name = name;
            this.salesCount = salesCount;
            this.revenue = revenue;
        }
    }
}