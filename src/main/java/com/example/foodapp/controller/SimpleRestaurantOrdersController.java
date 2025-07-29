package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class SimpleRestaurantOrdersController {
    @FXML private ComboBox<RestaurantItem> restaurantCombo;
    @FXML private ListView<OrderItem> ordersList;

    private ObservableList<RestaurantItem> restaurants = FXCollections.observableArrayList();
    private ObservableList<OrderItem> orders = FXCollections.observableArrayList();
    private Integer selectedRestaurantId = null;
    private String jwtToken;

    public void setJwtToken(String token) { this.jwtToken = token; }

    @FXML
    public void initialize() {
        restaurantCombo.setItems(restaurants);
        ordersList.setItems(orders);
        ordersList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderItem order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setText(null);
                } else {
                    setText("Order #" + order.id + " | Status: " + order.status + " | Address: " + order.address + " | Items: " + order.items + " | Total: " + order.total);
                }
            }
        });
        restaurantCombo.setOnAction(e -> {
            RestaurantItem selected = restaurantCombo.getValue();
            if (selected != null) {
                selectedRestaurantId = selected.id;
                loadOrdersFromBackend();
            }
        });
        fetchRestaurants();
    }

    private void fetchRestaurants() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/mine");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                List<RestaurantItem> list = parseRestaurantsFromJson(resp);
                Platform.runLater(() -> {
                    restaurants.setAll(list);
                    if (!list.isEmpty()) {
                        restaurantCombo.getSelectionModel().selectFirst();
                        selectedRestaurantId = list.get(0).id;
                        loadOrdersFromBackend();
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private List<RestaurantItem> parseRestaurantsFromJson(String json) {
        List<RestaurantItem> list = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    int id = node.path("id").asInt();
                    String name = node.path("name").asText("");
                    list.add(new RestaurantItem(id, name));
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return list;
    }

    private void loadOrdersFromBackend() {
        if (selectedRestaurantId == null) return;
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + selectedRestaurantId + "/orders");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                List<OrderItem> all = parseOrdersFromJson(resp);
                Platform.runLater(() -> orders.setAll(all));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private List<OrderItem> parseOrdersFromJson(String json) {
        List<OrderItem> all = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    int id = node.path("id").asInt();
                    String address = node.path("delivery_address").asText("");
                    String status = node.path("status").asText("");
                    int total = node.path("pay_price").asInt();
                    String items = node.path("items").isArray() ?
                        java.util.stream.StreamSupport.stream(node.path("items").spliterator(), false)
                            .map(item -> "ID:" + item.path("item_id").asInt() + " x" + item.path("quantity").asInt())
                            .collect(java.util.stream.Collectors.joining(", ")) :
                        node.path("items").asText("");
                    all.add(new OrderItem(id, address, items, total, status));
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
        return all;
    }

    public static class RestaurantItem {
        public final int id;
        public final String name;
        public RestaurantItem(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return name; }
    }
    public static class OrderItem {
        public final int id;
        public final String address;
        public final String items;
        public final int total;
        public final String status;
        public OrderItem(int id, String address, String items, int total, String status) {
            this.id = id; this.address = address; this.items = items; this.total = total; this.status = status;
        }
    }
} 