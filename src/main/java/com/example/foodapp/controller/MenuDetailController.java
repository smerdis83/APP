package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

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
    @FXML private ImageView foodImageView;
    @FXML private Button chooseFoodImageBtn;
    @FXML private Button cancelBtn;
    private String foodImageBase64 = null;

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
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    // Create a horizontal box with food info and edit button
                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
                    javafx.scene.control.Label foodLabel = new javafx.scene.control.Label(item.name + " (" + item.price + ")");
                    javafx.scene.control.Button editBtn = new javafx.scene.control.Button("Edit");
                    editBtn.setOnAction(e -> handleEditFood(item));
                    hbox.getChildren().addAll(foodLabel, editBtn);
                    setGraphic(hbox);
                    setText(null);
                }
            }
        });
        addFoodBtn.setOnAction(e -> handleAddFood());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        chooseFoodImageBtn.setOnAction(e -> handleChooseFoodImage());
        cancelBtn.setOnAction(e -> clearForm());
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
            
            // Parse name
            int nameIdx = json.indexOf("\"name\":", idx);
            int nameStart = json.indexOf('"', nameIdx + 7) + 1;
            int nameEnd = json.indexOf('"', nameStart);
            String name = json.substring(nameStart, nameEnd);
            
            // Parse price
            int priceIdx = json.indexOf("\"price\":", idx);
            int priceStart = priceIdx + 8;
            int priceEnd = json.indexOf(',', priceStart);
            if (priceEnd == -1) priceEnd = json.indexOf('}', priceStart);
            String priceStr = json.substring(priceStart, priceEnd).replaceAll("[^0-9]", "").trim();
            int price = Integer.parseInt(priceStr);
            
            // Parse description
            String description = "";
            int descIdx = json.indexOf("\"description\":", idx);
            if (descIdx != -1) {
                int descStart = json.indexOf('"', descIdx + 14) + 1;
                int descEnd = json.indexOf('"', descStart);
                if (descEnd > descStart) {
                    description = json.substring(descStart, descEnd);
                }
            }
            
            // Parse supply
            int supply = 0;
            int supplyIdx = json.indexOf("\"supply\":", idx);
            if (supplyIdx != -1) {
                int supplyStart = supplyIdx + 9;
                int supplyEnd = json.indexOf(',', supplyStart);
                if (supplyEnd == -1) supplyEnd = json.indexOf('}', supplyStart);
                String supplyStr = json.substring(supplyStart, supplyEnd).replaceAll("[^0-9]", "").trim();
                if (!supplyStr.isEmpty()) supply = Integer.parseInt(supplyStr);
            }
            
            // Parse keywords
            List<String> keywords = new ArrayList<>();
            int keywordsIdx = json.indexOf("\"keywords\":", idx);
            if (keywordsIdx != -1) {
                int keywordsStart = json.indexOf('"', keywordsIdx + 11) + 1;
                int keywordsEnd = json.indexOf('"', keywordsStart);
                if (keywordsEnd > keywordsStart) {
                    String keywordsStr = json.substring(keywordsStart, keywordsEnd);
                    if (!keywordsStr.isEmpty()) {
                        keywords = Arrays.asList(keywordsStr.split(","));
                    }
                }
            }
            
            // Parse image_base64
            String imageBase64 = null;
            int imageIdx = json.indexOf("\"image_base64\":", idx);
            if (imageIdx != -1) {
                int imageStart = json.indexOf('"', imageIdx + 15) + 1;
                int imageEnd = json.indexOf('"', imageStart);
                if (imageEnd > imageStart) {
                    imageBase64 = json.substring(imageStart, imageEnd);
                }
            }
            
            FoodItem food = new FoodItem(id, name, price, description, supply, keywords, imageBase64);
            list.add(food);
            idx = nameEnd;
        }
        return list;
    }

    private void handleChooseFoodImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Food Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(foodImageView.getScene().getWindow());
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) baos.write(buf, 0, n);
                byte[] imgBytes = baos.toByteArray();
                this.foodImageBase64 = Base64.getEncoder().encodeToString(imgBytes);
                foodImageView.setImage(new Image(new java.io.ByteArrayInputStream(imgBytes)));
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to load image: " + ex.getMessage()).showAndWait();
            }
        }
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
        
        // Parse keywords into array and create JSON array
        List<String> keywords = Arrays.asList(keywordsStr.split(","));
        StringBuilder keywordsJson = new StringBuilder("[");
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) keywordsJson.append(",");
            keywordsJson.append("\"").append(keywords.get(i).trim().replace("\"", "'")).append("\"");
        }
        keywordsJson.append("]");
        
        new Thread(() -> {
            try {
                // Step 1: Create the food item
                String foodJson = String.format("{\"name\":\"%s\",\"description\":\"%s\",\"price\":%d,\"supply\":%d,\"keywords\":%s%s}",
                        name.replace("\"", "'"), 
                        desc.replace("\"", "'"), 
                        price, 
                        supply, 
                        keywordsJson.toString(),
                        (foodImageBase64 != null && !foodImageBase64.isEmpty()) ? String.format(",\"image_base64\":\"%s\"", foodImageBase64.replace("\"", "'")) : "");
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

    private void handleEditFood(FoodItem item) {
        // Populate the form fields with the current food item data
        foodNameField.setText(item.name);
        foodPriceField.setText(String.valueOf(item.price));
        foodDescField.setText(item.description != null ? item.description : "");
        foodSupplyField.setText(String.valueOf(item.supply));
        
        // Convert keywords list to comma-separated string
        String keywordsText = "";
        if (item.keywords != null && !item.keywords.isEmpty()) {
            keywordsText = String.join(",", item.keywords);
        }
        foodKeywordsField.setText(keywordsText);
        
        // Set the image if available
        if (item.imageBase64 != null && !item.imageBase64.isEmpty()) {
            try {
                byte[] imgBytes = java.util.Base64.getDecoder().decode(item.imageBase64);
                foodImageView.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imgBytes)));
                foodImageBase64 = item.imageBase64;
            } catch (Exception e) {
                foodImageView.setImage(null);
                foodImageBase64 = null;
            }
        } else {
            foodImageView.setImage(null);
            foodImageBase64 = null;
        }
        
        // Change the add button to update mode
        addFoodBtn.setText("Update Food");
        addFoodBtn.setOnAction(e -> handleUpdateFood(item.id));
        
        messageLabel.setText("Editing: " + item.name);
    }
    
    private void handleUpdateFood(int foodId) {
        String name = foodNameField.getText().trim();
        String priceStr = foodPriceField.getText().trim();
        String description = foodDescField.getText().trim();
        String supplyStr = foodSupplyField.getText().trim();
        String keywords = foodKeywordsField.getText().trim();
        
        if (name.isEmpty() || priceStr.isEmpty() || supplyStr.isEmpty()) {
            messageLabel.setText("Please fill in all required fields.");
            return;
        }
        
        try {
            int price = Integer.parseInt(priceStr);
            int supply = Integer.parseInt(supplyStr);
            
            if (price <= 0 || supply < 0) {
                messageLabel.setText("Price must be positive and supply must be non-negative.");
                return;
            }
            
            // Parse keywords into array
            List<String> keywordsList = new ArrayList<>();
            if (!keywords.isEmpty()) {
                String[] keywordArray = keywords.split(",");
                for (String keyword : keywordArray) {
                    keywordsList.add(keyword.trim());
                }
            }
            
            // Create keywords JSON array
            StringBuilder keywordsJson = new StringBuilder("[");
            for (int i = 0; i < keywordsList.size(); i++) {
                if (i > 0) keywordsJson.append(",");
                keywordsJson.append("\"").append(keywordsList.get(i).replace("\"", "'")).append("\"");
            }
            keywordsJson.append("]");
            
            // Create the update JSON
            String updateJson = String.format(
                "{\"name\":\"%s\",\"price\":%d,\"description\":\"%s\",\"supply\":%d,\"keywords\":%s,\"image_base64\":\"%s\"}",
                name.replace("\"", "'"),
                price,
                description.replace("\"", "'"),
                supply,
                keywordsJson.toString(),
                foodImageBase64 != null ? foodImageBase64.replace("\"", "'") : ""
            );
            
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + restaurantId + "/item/" + foodId);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("PUT");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                    conn.setDoOutput(true);
                    
                    try (java.io.OutputStream os = conn.getOutputStream()) {
                        os.write(updateJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    
                    int code = conn.getResponseCode();
                    if (code == 200 || code == 204) {
                        Platform.runLater(() -> {
                            messageLabel.setText("Food updated successfully!");
                            clearForm();
                            loadFoods(); // Reload the food list
                        });
                    } else {
                        String errorResponse;
                        try (java.util.Scanner scanner = new java.util.Scanner(
                                conn.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8)) {
                            scanner.useDelimiter("\\A");
                            errorResponse = scanner.hasNext() ? scanner.next() : "";
                        }
                        Platform.runLater(() -> messageLabel.setText("Failed to update food: " + errorResponse));
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> messageLabel.setText("Error: " + ex.getMessage()));
                }
            }).start();
            
        } catch (NumberFormatException e) {
            messageLabel.setText("Please enter valid numbers for price and supply.");
        }
    }
    
    private void clearForm() {
        foodNameField.clear();
        foodPriceField.clear();
        foodDescField.clear();
        foodSupplyField.clear();
        foodKeywordsField.clear();
        foodImageView.setImage(null);
        foodImageBase64 = null;
        addFoodBtn.setText("Add Food");
        addFoodBtn.setOnAction(e -> handleAddFood());
        messageLabel.setText("");
    }

    public static class FoodItem {
        public final int id;
        public final String name;
        public final int price;
        public String description;
        public int supply;
        public List<String> keywords;
        public String imageBase64;
        public FoodItem(int id, String name, int price) { this.id = id; this.name = name; this.price = price; }
        public FoodItem(int id, String name, int price, String description, int supply, List<String> keywords, String imageBase64) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.description = description;
            this.supply = supply;
            this.keywords = keywords;
            this.imageBase64 = imageBase64;
        }
        @Override public String toString() { return name + " (" + price + ")"; }
    }
} 