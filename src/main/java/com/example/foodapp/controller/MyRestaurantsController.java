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
import java.util.function.Consumer;

public class MyRestaurantsController {
    @FXML private ListView<RestaurantItem> restaurantList;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private String jwtToken;
    private Runnable onBack;
    private ObservableList<RestaurantItem> restaurants = FXCollections.observableArrayList();
    private Consumer<RestaurantItem> onManageMenus;
    public void setOnManageMenus(Consumer<RestaurantItem> c) { this.onManageMenus = c; }

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        restaurantList.setItems(restaurants);
        restaurantList.setCellFactory(list -> new RestaurantCell());
        restaurantList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                RestaurantItem item = restaurantList.getSelectionModel().getSelectedItem();
                if (item != null && onManageMenus != null) onManageMenus.accept(item);
            }
        });
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        fetchMyRestaurants();
    }

    private void fetchMyRestaurants() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/mine");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch your restaurants: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<RestaurantItem> items = parseRestaurants(json);
                Platform.runLater(() -> restaurants.setAll(items));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    restaurants.clear();
                    messageLabel.setText("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private List<RestaurantItem> parseRestaurants(String json) {
        List<RestaurantItem> list = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"id\":", idx)) != -1) {
            int id = Integer.parseInt(json.substring(idx + 5, json.indexOf(',', idx + 5)).replaceAll("[^0-9]", ""));
            int nameIdx = json.indexOf("\"name\":", idx);
            int nameStart = json.indexOf('"', nameIdx + 7) + 1;
            int nameEnd = json.indexOf('"', nameStart);
            String name = json.substring(nameStart, nameEnd);
            int logoIdx = json.indexOf("\"logo_base64\":", idx);
            String logo = null;
            if (logoIdx != -1) {
                int logoStart = json.indexOf('"', logoIdx + 14) + 1;
                int logoEnd = json.indexOf('"', logoStart);
                logo = json.substring(logoStart, logoEnd);
            }
            list.add(new RestaurantItem(id, name, logo));
            idx = nameEnd;
        }
        return list;
    }

    public static class RestaurantItem {
        public final int id;
        public final String name;
        public final String logoBase64;
        public RestaurantItem(int id, String name, String logoBase64) {
            this.id = id; this.name = name; this.logoBase64 = logoBase64;
        }
    }

    public static class RestaurantCell extends ListCell<RestaurantItem> {
        private final HBox content = new HBox(10);
        private final ImageView imageView = new ImageView();
        private final Label nameLabel = new Label();
        public RestaurantCell() {
            imageView.setFitHeight(50); imageView.setFitWidth(50);
            content.getChildren().addAll(imageView, nameLabel);
        }
        @Override
        protected void updateItem(RestaurantItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.name);
                if (item.logoBase64 != null && !item.logoBase64.isEmpty()) {
                    try {
                        byte[] imgBytes = Base64.getDecoder().decode(item.logoBase64);
                        imageView.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                    } catch (Exception e) { imageView.setImage(null); }
                } else {
                    imageView.setImage(null);
                }
                setGraphic(content);
            }
        }
    }
} 