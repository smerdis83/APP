package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import java.util.*;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javafx.scene.layout.HBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class RestaurantPageController {
    @FXML private ImageView restaurantLogo;
    @FXML private Label restaurantNameLabel;
    @FXML private ListView<FoodItem> foodList;
    @FXML private ListView<BasketItem> basketList;
    @FXML private Label totalPrice;
    @FXML private Button orderBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;
    @FXML private Button prevMenuBtn;
    @FXML private Button nextMenuBtn;
    @FXML private Label menuNameLabel;

    private int restaurantId;
    private String jwtToken;
    private ObservableList<FoodItem> menuItems = FXCollections.observableArrayList();
    private ObservableList<BasketItem> basketItems = FXCollections.observableArrayList();
    private Map<Integer, Integer> basketMap = new HashMap<>(); // foodId -> quantity
    private Runnable onBack;
    private String selectedMenuTitle = null;
    // Remove: menuItems.clear(); from setupMenuTabs or fetchMenuItems
    // Only update menuItems (foodList) when switching tabs, but do not clear basketMap or basketItems
    // When adding a food, use its id as the key in basketMap, regardless of menu
    // The basket (basketItems) is always updated from basketMap and menuItems from all menus
    // To do this, keep a masterFoodMap of all fetched foods by id
    private Map<Integer, FoodItem> masterFoodMap = new HashMap<>();
    private List<String> menuTitles = new ArrayList<>();
    private int currentMenuIndex = 0;
    private com.example.foodapp.LoginApp app;
    public void setApp(com.example.foodapp.LoginApp app) { this.app = app; }

    private String restaurantName;
    private String logoBase64;

    public void setRestaurant(int id, String name, String logoBase64, String jwtToken) {
        this.restaurantId = id;
        this.restaurantName = name;
        this.logoBase64 = logoBase64;
        this.jwtToken = jwtToken;
        restaurantNameLabel.setText(name);
        if (logoBase64 != null && !logoBase64.isEmpty()) {
            try {
                byte[] imgBytes = java.util.Base64.getDecoder().decode(logoBase64);
                restaurantLogo.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imgBytes)));
            } catch (Exception e) { restaurantLogo.setImage(null); }
        } else {
            restaurantLogo.setImage(null);
        }
        fetchMenus();
    }

    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        foodList.setItems(menuItems);
        foodList.setCellFactory(list -> new FoodCell());
        basketList.setItems(basketItems);
        basketList.setCellFactory(list -> new BasketCell());
        updateBasket();
        orderBtn.setOnAction(e -> handleOrder());
        if (backBtn != null) backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    private void fetchMenus() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + restaurantId + "/menus");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch menus: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<String> titles = parseMenuTitles(json);
                Platform.runLater(() -> setupMenuNavigation(titles));
            } catch (Exception ex) {
                Platform.runLater(() -> setupMenuNavigation(Collections.emptyList()));
            }
        }).start();
    }

    private List<String> parseMenuTitles(String json) {
        List<String> titles = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"title\":", idx)) != -1) {
            int start = json.indexOf('"', idx + 8) + 1;
            int end = json.indexOf('"', start);
            String title = json.substring(start, end);
            titles.add(title);
            idx = end;
        }
        return titles;
    }

    private void setupMenuNavigation(List<String> titles) {
        menuTitles.clear();
        menuTitles.addAll(titles);
        currentMenuIndex = 0;
        if (menuTitles.isEmpty()) {
            menuNameLabel.setText("No menu found");
            menuItems.clear();
            prevMenuBtn.setDisable(true);
            nextMenuBtn.setDisable(true);
        } else {
            menuNameLabel.setText(menuTitles.get(currentMenuIndex));
            prevMenuBtn.setDisable(menuTitles.size() == 1);
            nextMenuBtn.setDisable(menuTitles.size() == 1);
            fetchMenuItems(menuTitles.get(currentMenuIndex));
        }
        prevMenuBtn.setOnAction(e -> showPrevMenu());
        nextMenuBtn.setOnAction(e -> showNextMenu());
    }

    private void showPrevMenu() {
        if (menuTitles.isEmpty()) return;
        currentMenuIndex = (currentMenuIndex - 1 + menuTitles.size()) % menuTitles.size();
        menuNameLabel.setText(menuTitles.get(currentMenuIndex));
        fetchMenuItems(menuTitles.get(currentMenuIndex));
    }

    private void showNextMenu() {
        if (menuTitles.isEmpty()) return;
        currentMenuIndex = (currentMenuIndex + 1) % menuTitles.size();
        menuNameLabel.setText(menuTitles.get(currentMenuIndex));
        fetchMenuItems(menuTitles.get(currentMenuIndex));
    }

    private void fetchMenuItems(String menuTitle) {
        new Thread(() -> {
            try {
                String encodedTitle = java.net.URLEncoder.encode(menuTitle, "UTF-8");
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + restaurantId + "/menus/" + encodedTitle + "/items");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch menu items: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<FoodItem> items = parseMenu(json);
                Platform.runLater(() -> {
                    menuItems.setAll(items);
                    for (FoodItem f : items) masterFoodMap.put(f.id, f);
                    updateBasket();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> menuItems.clear());
            }
        }).start();
    }

    private List<FoodItem> parseMenu(String json) {
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
            int supplyIdx = json.indexOf("\"supply\":", idx);
            int supply = 0;
            if (supplyIdx != -1) {
                int supplyStart = supplyIdx + 9;
                int supplyEnd = json.indexOf(',', supplyStart);
                if (supplyEnd == -1) supplyEnd = json.indexOf('}', supplyStart);
                String supplyStr = json.substring(supplyStart, supplyEnd).replaceAll("[^0-9]", "").trim();
                if (!supplyStr.isEmpty()) supply = Integer.parseInt(supplyStr);
            }
            // Parse image_base64 if present
            int imageIdx = json.indexOf("\"image_base64\":", idx);
            String imageBase64 = null;
            if (imageIdx != -1) {
                int imageStart = json.indexOf('"', imageIdx + 15) + 1;
                int imageEnd = json.indexOf('"', imageStart);
                imageBase64 = json.substring(imageStart, imageEnd);
            }
            list.add(new FoodItem(id, name, price, supply, imageBase64));
            idx = nameEnd;
        }
        return list;
    }

    private void updateBasket() {
        basketItems.clear();
        int total = 0;
        for (Map.Entry<Integer, Integer> entry : basketMap.entrySet()) {
            int foodId = entry.getKey();
            int qty = entry.getValue();
            FoodItem item = masterFoodMap.get(foodId);
            if (item != null && qty > 0) {
                basketItems.add(new BasketItem(item, qty));
                total += item.price * qty;
            }
        }
        totalPrice.setText(total + "");
    }

    private void handleOrder() {
        if (basketItems.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Your basket is empty.").showAndWait();
            return;
        }
        javafx.stage.Stage stage = (javafx.stage.Stage) orderBtn.getScene().getWindow();
        app.showSelectAddressScreen(stage, address -> {
            Platform.runLater(() -> {
                app.showPaymentPage(stage, restaurantId, restaurantName, logoBase64, address, new java.util.ArrayList<>(basketItems), jwtToken, () -> {
                    // After payment, reload the restaurant page
                    app.showRestaurantPage(stage, restaurantId, restaurantName, logoBase64, onBack);
                    basketMap.clear();
                    updateBasket();
                });
            });
        }, () -> {
            // On back, reload the restaurant page
            Platform.runLater(() -> {
                app.showRestaurantPage(stage, restaurantId, restaurantName, logoBase64, onBack);
            });
        });
    }

    private class FoodCell extends ListCell<FoodItem> {
        private final HBox content = new HBox(20);
        private final ImageView foodImageView = new ImageView();
        private final Label nameLabel = new Label();
        private final Label priceLabel = new Label();
        private final Button plusBtn = new Button("+");
        private final Button minusBtn = new Button("–");
        private final Label qtyLabel = new Label("0");
        public FoodCell() {
            foodImageView.setFitHeight(50);
            foodImageView.setFitWidth(50);
            nameLabel.setMinWidth(120);
            nameLabel.setMaxWidth(120);
            nameLabel.setWrapText(true);
            priceLabel.setMinWidth(60);
            priceLabel.setMaxWidth(60);
            qtyLabel.setMinWidth(30);
            qtyLabel.setMaxWidth(30);
            plusBtn.setText("+");
            plusBtn.setMinWidth(32);
            plusBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            minusBtn.setMinWidth(32);
            minusBtn.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            content.setSpacing(8);
            content.getChildren().addAll(foodImageView, nameLabel, priceLabel, minusBtn, qtyLabel, plusBtn);
            plusBtn.setOnAction(e -> changeQty(1));
            minusBtn.setOnAction(e -> changeQty(-1));
        }
        @Override
        protected void updateItem(FoodItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.name);
                priceLabel.setText(item.price + "");
                int qty = basketMap.getOrDefault(item.id, 0);
                qtyLabel.setText(qty + "");
                plusBtn.setDisable(qty >= item.supply);
                // Set food image if available
                if (item.imageBase64 != null && !item.imageBase64.isEmpty()) {
                    try {
                        byte[] imgBytes = java.util.Base64.getDecoder().decode(item.imageBase64);
                        foodImageView.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(imgBytes)));
                    } catch (Exception e) { foodImageView.setImage(null); }
                } else {
                    foodImageView.setImage(null);
                }
                setGraphic(content);
            }
        }
        private void changeQty(int delta) {
            FoodItem item = getItem();
            if (item == null) return;
            int qty = basketMap.getOrDefault(item.id, 0) + delta;
            if (qty < 0) qty = 0;
            basketMap.put(item.id, qty);
            masterFoodMap.put(item.id, item); // ensure it's in the master map
            updateBasket();
            foodList.refresh();
            if (messageLabel != null) {
                if (delta > 0) messageLabel.setText(item.name + " added to basket.");
                else if (delta < 0) messageLabel.setText(item.name + " removed from basket.");
            }
        }
    }

    private class BasketCell extends ListCell<BasketItem> {
        private final HBox content = new HBox(10);
        private final Label nameLabel = new Label();
        private final Label qtyLabel = new Label();
        private final Label priceLabel = new Label();
        public BasketCell() {
            content.getChildren().addAll(nameLabel, qtyLabel, priceLabel);
        }
        @Override
        protected void updateItem(BasketItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.food.name);
                qtyLabel.setText(" x" + item.quantity);
                priceLabel.setText(" = " + (item.food.price * item.quantity));
                setGraphic(content);
            }
        }
    }

    public static class FoodItem {
        public final int id;
        public final String name;
        public final int price;
        public final int supply;
        public final String imageBase64;
        public FoodItem(int id, String name, int price, int supply, String imageBase64) {
            this.id = id; this.name = name; this.price = price; this.supply = supply; this.imageBase64 = imageBase64;
        }
        public FoodItem(int id, String name, int price, int supply) {
            this(id, name, price, supply, null);
        }
    }
    public static class BasketItem {
        public final FoodItem food;
        public final int quantity;
        public BasketItem(FoodItem food, int quantity) {
            this.food = food; this.quantity = quantity;
        }
    }
} 