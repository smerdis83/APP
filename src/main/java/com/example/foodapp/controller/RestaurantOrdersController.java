package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import javafx.scene.control.Label;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;

public class RestaurantOrdersController {
    @FXML private ComboBox<RestaurantItem> restaurantCombo;
    @FXML private ListView<OrderItem> submittedList;
    @FXML private ListView<OrderItem> acceptedList;
    @FXML private ListView<OrderItem> servedList;
    @FXML private Label messageLabel;
    @FXML private Button backBtn;
    private Runnable onBack;
    public void setOnBack(Runnable callback) { this.onBack = callback; }

    private ObservableList<RestaurantItem> restaurants = FXCollections.observableArrayList();
    private ObservableList<OrderItem> submittedOrders = FXCollections.observableArrayList();
    private ObservableList<OrderItem> acceptedOrders = FXCollections.observableArrayList();
    private ObservableList<OrderItem> servedOrders = FXCollections.observableArrayList();
    private Integer selectedRestaurantId = null;
    private String jwtToken;

    public void setJwtToken(String token) { this.jwtToken = token; }

    @FXML
    public void initialize() {
        restaurantCombo.setItems(restaurants);
        submittedList.setItems(submittedOrders);
        acceptedList.setItems(acceptedOrders);
        servedList.setItems(servedOrders);
        if (backBtn != null) backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        submittedList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderItem order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.getChildren().add(new javafx.scene.control.Label("Address: " + order.address));
                    box.getChildren().add(new javafx.scene.control.Label("Total: " + order.total));
                    // Only show Accept/Reject for 'waiting vendor' orders
                    String normalized = order.status.trim().replaceAll("\\s+", " ").toLowerCase();
                    if (normalized.equals("waiting vendor")) {
                        Button acceptBtn = new Button("Accept");
                        Button rejectBtn = new Button("Reject");
                        acceptBtn.setOnAction(e -> updateOrderStatus(order, "accepted"));
                        rejectBtn.setOnAction(e -> updateOrderStatus(order, "rejected"));
                        box.getChildren().add(new HBox(10, acceptBtn, rejectBtn));
                    }
                    setGraphic(box);
                }
            }
        });
        acceptedList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderItem order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.getChildren().add(new javafx.scene.control.Label("Address: " + order.address));
                    box.getChildren().add(new javafx.scene.control.Label("Total: " + order.total));
                    Button serveBtn = new Button("Mark as Served");
                    serveBtn.setOnAction(e -> updateOrderStatus(order, "served"));
                    box.getChildren().add(serveBtn);
                    setGraphic(box);
                }
            }
        });
        servedList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderItem order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.getChildren().add(new javafx.scene.control.Label("Address: " + order.address));
                    box.getChildren().add(new javafx.scene.control.Label("Total: " + order.total));
                    box.getChildren().add(new javafx.scene.control.Label("Status: Served"));
                    setGraphic(box);
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
                System.out.println("[DEBUG] After reload, orders:");
                for (OrderItem o : all) System.out.println("[DEBUG] Order: id=" + o.id + ", status=" + o.status);
                Platform.runLater(() -> {
                    submittedOrders.setAll(all.stream().filter(o -> o.status.trim().replaceAll("\\s+", " ").equalsIgnoreCase("waiting vendor")).toList());
                    acceptedOrders.setAll(all.stream().filter(o -> o.status.equals("accepted")).toList());
                    servedOrders.setAll(all.stream().filter(o -> o.status.equals("served")).toList());
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private List<OrderItem> parseOrdersFromJson(String json) {
        List<OrderItem> all = new ArrayList<>();
        try {
            System.out.println("[DEBUG] Raw JSON response: " + json);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode arr = mapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode node : arr) {
                    int id = node.path("id").asInt();
                    String address = node.path("delivery_address").asText("");
                    String status = node.path("status").asText("");
                    
                    // Check for different possible field names for the total amount
                    int total = 0;
                    if (node.has("pay_price")) {
                        total = node.path("pay_price").asInt();
                        System.out.println("[DEBUG] Order " + id + ": Using pay_price = " + total);
                    } else if (node.has("payPrice")) {
                        total = node.path("payPrice").asInt();
                        System.out.println("[DEBUG] Order " + id + ": Using payPrice = " + total);
                    } else if (node.has("total")) {
                        total = node.path("total").asInt();
                        System.out.println("[DEBUG] Order " + id + ": Using total = " + total);
                    } else {
                        System.out.println("[DEBUG] Order " + id + ": No amount field found! Available fields: " + node.fieldNames());
                    }
                    
                    all.add(new OrderItem(id, address, total, status));
                }
            }
        } catch (Exception ex) { 
            System.err.println("[ERROR] Failed to parse orders JSON: " + ex.getMessage());
            ex.printStackTrace(); 
        }
        return all;
    }

    private void showMessage(String msg) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> messageLabel.setText(""));
            }).start();
        }
    }

    private void updateOrderStatus(OrderItem order, String newStatus) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String url = "http://localhost:8000/restaurants/orders/" + order.id;
                String json = String.format("{\"status\":\"%s\"}", newStatus);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .method("PATCH", BodyPublishers.ofString(json));
                if (jwtToken != null) builder.header("Authorization", "Bearer " + jwtToken);
                HttpRequest request = builder.build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();
                System.out.println("[DEBUG] PATCH response code: " + code);
                System.out.println("[DEBUG] PATCH response body: " + response.body());
                if (code == 200 || code == 204) {
                    Platform.runLater(() -> {
                        loadOrdersFromBackend();
                        if ("accepted".equals(newStatus)) showMessage("Order accepted!");
                        else if ("rejected".equals(newStatus)) showMessage("Order rejected.");
                        else if ("served".equals(newStatus)) showMessage("Order marked as served.");
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
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
        public final int total;
        public final String status;
        public OrderItem(int id, String address, int total, String status) {
            this.id = id; this.address = address; this.total = total; this.status = status;
        }
    }
} 