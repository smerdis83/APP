package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;

public class MenuDetailController {
    @FXML private Label menuLabel;
    @FXML private ListView<FoodItem> foodList;
    @FXML private TextField foodNameField;
    @FXML private TextField foodPriceField;
    @FXML private TextField foodDescField;
    @FXML private TextField foodSupplyField;
    @FXML private TextField foodKeywordsField;
    @FXML private Button addFoodBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private String jwtToken;
    private int restaurantId;
    private String menuTitle;
    private String encodedMenuTitle;
    private ObservableList<FoodItem> foods = FXCollections.observableArrayList();
    private Runnable onBack;

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setRestaurantAndMenu(int restaurantId, String menuTitle) {
        this.restaurantId = restaurantId;
        this.menuTitle = menuTitle;
        try { this.encodedMenuTitle = java.net.URLEncoder.encode(menuTitle, "UTF-8"); } catch (Exception e) { this.encodedMenuTitle = menuTitle; }
    }
    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        foodList.setItems(foods);
        foodList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(FoodItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name + " (" + item.price + ")");
            }
        });
        addFoodBtn.setOnAction(e -> handleAddFood());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void loadFoods() {
        menuLabel.setText("Menu: " + menuTitle);
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + restaurantId + "/menus/" + encodedMenuTitle + "/items");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch foods: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<FoodItem> items = parseFoods(json);
                Platform.runLater(() -> foods.setAll(items));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    foods.clear();
                    messageLabel.setText("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private List<FoodItem> parseFoods(String json) {
        List<FoodItem> list = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"id\":", idx)) != -1) {
            int id = Integer.parseInt(json.substring(idx + 5, json.indexOf(',', idx + 5)).replaceAll("[^0-9]", ""));
            int nameIdx = json.indexOf("\"name\":", idx);
            int nameStart = json.indexOf('"', nameIdx + 7) + 1;
            int nameEnd = json.indexOf('"', nameStart);
            String name = json.substring(nameStart, nameEnd);
            int priceIdx = json.indexOf("\"price\":", idx);
            int priceStart = priceIdx + 8;
            int priceEnd = json.indexOf(',', priceStart);
            if (priceEnd == -1) priceEnd = json.indexOf('}', priceStart);
            String priceStr = json.substring(priceStart, priceEnd).replaceAll("[^0-9]", "").trim();
            int price = Integer.parseInt(priceStr);
            list.add(new FoodItem(id, name, price));
            idx = nameEnd;
        }
        return list;
    }

    private void handleAddFood() {
        String name = foodNameField.getText().trim();
        String priceStr = foodPriceField.getText().trim();
        String desc = foodDescField.getText().trim();
        String supplyStr = foodSupplyField.getText().trim();
        String keywordsStr = foodKeywordsField.getText().trim();
        if (name.isEmpty() || priceStr.isEmpty() || supplyStr.isEmpty() || keywordsStr.isEmpty()) {
            messageLabel.setText("Name, price, supply, and keywords are required.");
            return;
        }
        int price, supply;
        try { price = Integer.parseInt(priceStr); supply = Integer.parseInt(supplyStr); }
        catch (Exception e) { messageLabel.setText("Price and supply must be integers."); return; }
        List<String> keywords = Arrays.asList(keywordsStr.split(","));
        new Thread(() -> {
            try {
                // Step 1: Create the food item
                String foodJson = String.format("{\"name\":\"%s\",\"description\":\"%s\",\"price\":%d,\"supply\":%d,\"keywords\":%s}",
                        name, desc, price, supply, new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(keywords));
                java.net.URL foodUrl = new java.net.URL("http://localhost:8000/restaurants/" + restaurantId + "/item");
                java.net.HttpURLConnection foodConn = (java.net.HttpURLConnection) foodUrl.openConnection();
                foodConn.setRequestMethod("POST");
                foodConn.setRequestProperty("Content-Type", "application/json");
                foodConn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                foodConn.setDoOutput(true);
                try (java.io.OutputStream os = foodConn.getOutputStream()) {
                    os.write(foodJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int foodCode = foodConn.getResponseCode();
                String foodResp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        foodCode >= 200 && foodCode < 300 ? foodConn.getInputStream() : foodConn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    foodResp = scanner.hasNext() ? scanner.next() : "";
                }
                if (foodCode != 200 && foodCode != 201) {
                    Platform.runLater(() -> messageLabel.setText("Failed to create food: " + foodResp));
                    return;
                }
                // Parse item_id from response
                int itemId = -1;
                int idIdx = foodResp.indexOf("\"id\":");
                if (idIdx != -1) {
                    int idStart = idIdx + 5;
                    int idEnd = foodResp.indexOf(',', idStart);
                    if (idEnd == -1) idEnd = foodResp.indexOf('}', idStart);
                    String idStr = foodResp.substring(idStart, idEnd).replaceAll("[^0-9]", "").trim();
                    itemId = Integer.parseInt(idStr);
                }
                if (itemId == -1) {
                    Platform.runLater(() -> messageLabel.setText("Could not parse new food item id."));
                    return;
                }
                // Step 2: Add the food item to the menu
                java.net.URL menuUrl = new java.net.URL("http://localhost:8000/restaurants/" + restaurantId + "/menu/" + encodedMenuTitle);
                java.net.HttpURLConnection menuConn = (java.net.HttpURLConnection) menuUrl.openConnection();
                menuConn.setRequestMethod("PUT");
                menuConn.setRequestProperty("Content-Type", "application/json");
                menuConn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                menuConn.setDoOutput(true);
                String menuJson = String.format("{\"item_id\":%d}", itemId);
                try (java.io.OutputStream os = menuConn.getOutputStream()) {
                    os.write(menuJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int menuCode = menuConn.getResponseCode();
                String menuResp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        menuCode >= 200 && menuCode < 300 ? menuConn.getInputStream() : menuConn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    menuResp = scanner.hasNext() ? scanner.next() : "";
                }
                if (menuCode == 200) {
                    Platform.runLater(() -> {
                        messageLabel.setText("Food added to menu successfully!");
                        loadFoods();
                    });
                } else {
                    Platform.runLater(() -> messageLabel.setText("Failed to add food to menu: " + menuResp));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    public static class FoodItem {
        public final int id;
        public final String name;
        public final int price;
        public FoodItem(int id, String name, int price) { this.id = id; this.name = name; this.price = price; }
        @Override public String toString() { return name + " (" + price + ")"; }
    }
} 