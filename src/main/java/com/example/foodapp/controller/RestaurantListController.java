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
import java.util.function.Consumer;
import javafx.scene.layout.HBox;

public class RestaurantListController {
    @FXML private ListView<RestaurantItem> restaurantList;
    @FXML private Button backBtn;

    private String jwtToken;
    private Runnable onBack;
    private Consumer<RestaurantItem> onRestaurantClick;
    private ObservableList<RestaurantItem> restaurants = FXCollections.observableArrayList();

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }
    public void setOnRestaurantClick(Consumer<RestaurantItem> c) { this.onRestaurantClick = c; }

    @FXML
    public void initialize() {
        restaurantList.setItems(restaurants);
        restaurantList.setCellFactory(list -> new RestaurantCell());
        restaurantList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                RestaurantItem item = restaurantList.getSelectionModel().getSelectedItem();
                if (item != null && onRestaurantClick != null) onRestaurantClick.accept(item);
            }
        });
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        fetchRestaurants();
    }

    private void fetchRestaurants() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch restaurants: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<RestaurantItem> items = parseRestaurants(json);
                Platform.runLater(() -> restaurants.setAll(items));
            } catch (Exception ex) {
                Platform.runLater(() -> restaurants.clear());
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
        private final HBox content = new HBox(16);
        private final ImageView imageView = new ImageView();
        private final Label nameLabel = new Label();
        public RestaurantCell() {
            imageView.setFitHeight(60); imageView.setFitWidth(60);
            imageView.setStyle("-fx-effect: dropshadow(gaussian, #b0b0b0, 6, 0.2, 0, 2); -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #e0e0e0; -fx-border-width: 2; -fx-padding: 6;");
            nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 0 0 0 12;");
            content.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8 0 8 0;");
            content.getChildren().addAll(imageView, nameLabel);
        }
        @Override
        protected void updateItem(RestaurantItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.name);
                if (item.logoBase64 != null && item.logoBase64.length() > 20 && !item.logoBase64.startsWith("[")) {
                    System.out.println("[DEBUG] Restaurant logoBase64 length: " + item.logoBase64.length() + ", first 20: " + item.logoBase64.substring(0, Math.min(20, item.logoBase64.length())));
                    try {
                        byte[] imgBytes = Base64.getDecoder().decode(item.logoBase64);
                        imageView.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                    } catch (Exception e) { imageView.setImage(null); }
                } else {
                    System.out.println("[DEBUG] Restaurant has no valid logoBase64: " + item.name);
                    imageView.setImage(null);
                }
                setGraphic(content);
            }
        }
    }
} 