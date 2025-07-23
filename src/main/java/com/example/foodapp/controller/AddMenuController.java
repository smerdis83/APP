package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;

public class AddMenuController {
    @FXML private ComboBox<RestaurantItem> restaurantCombo;
    @FXML private TextField titleField;
    @FXML private Button addBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private String jwtToken;
    private Runnable onBack;
    private ObservableList<RestaurantItem> restaurants = FXCollections.observableArrayList();
    private Integer defaultRestaurantId = null;
    public void setDefaultRestaurantId(int id) { this.defaultRestaurantId = id; }

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        restaurantCombo.setItems(restaurants);
        restaurantCombo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(RestaurantItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name);
            }
        });
        restaurantCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(RestaurantItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name);
            }
        });
        addBtn.setOnAction(e -> handleAddMenu());
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
                Platform.runLater(() -> {
                    restaurants.setAll(items);
                    if (defaultRestaurantId != null) {
                        for (RestaurantItem r : items) {
                            if (r.id == defaultRestaurantId) {
                                restaurantCombo.getSelectionModel().select(r);
                                break;
                            }
                        }
                    }
                });
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
            list.add(new RestaurantItem(id, name));
            idx = nameEnd;
        }
        return list;
    }

    private void handleAddMenu() {
        RestaurantItem selected = restaurantCombo.getValue();
        String title = titleField.getText().trim();
        if (selected == null || title.isEmpty()) {
            messageLabel.setText("Select a restaurant and enter a menu title.");
            return;
        }
        new Thread(() -> {
            try {
                String json = String.format("{\"title\":\"%s\"}", title);
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + selected.id + "/menu");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200 || code == 201) {
                    Platform.runLater(() -> messageLabel.setText("Menu added successfully!"));
                } else {
                    Platform.runLater(() -> messageLabel.setText("Failed: " + resp));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    public static class RestaurantItem {
        public final int id;
        public final String name;
        public RestaurantItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
} 