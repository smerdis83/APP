package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.scene.layout.HBox;

import java.util.*;
import java.util.stream.Collectors;

public class RestaurantOrdersController {
    @FXML private ListView<OrderItem> pendingOrdersList;
    @FXML private ListView<OrderItem> acceptedOrdersList;
    @FXML private ListView<OrderItem> servedOrdersList;

    private ObservableList<OrderItem> pendingOrders = FXCollections.observableArrayList();
    private ObservableList<OrderItem> acceptedOrders = FXCollections.observableArrayList();
    private ObservableList<OrderItem> servedOrders = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Make ListViews fill available space
        pendingOrdersList.setPrefHeight(Double.MAX_VALUE);
        acceptedOrdersList.setPrefHeight(Double.MAX_VALUE);
        servedOrdersList.setPrefHeight(Double.MAX_VALUE);

        // Set cell factories for custom order display
        pendingOrdersList.setCellFactory(list -> new OrderCell("pending"));
        acceptedOrdersList.setCellFactory(list -> new OrderCell("accepted"));
        servedOrdersList.setCellFactory(list -> new OrderCell("served"));

        // Assign observable lists
        pendingOrdersList.setItems(pendingOrders);
        acceptedOrdersList.setItems(acceptedOrders);
        servedOrdersList.setItems(servedOrders);

        loadOrdersFromBackend();
    }

    private void loadOrdersFromBackend() {
        new Thread(() -> {
            try {
                // Replace with your actual backend endpoint and auth as needed
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurant/orders");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // TODO: Set JWT token if needed
                // conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                // TODO: Parse JSON response into List<OrderItem>
                // For now, fallback to mock if parsing fails
                List<OrderItem> all = parseOrdersFromJson(resp);
                Platform.runLater(() -> {
                    pendingOrders.setAll(all.stream().filter(o -> o.status.equals("pending")).collect(Collectors.toList()));
                    acceptedOrders.setAll(all.stream().filter(o -> o.status.equals("accepted")).collect(Collectors.toList()));
                    servedOrders.setAll(all.stream().filter(o -> o.status.equals("served")).collect(Collectors.toList()));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(this::loadMockOrders);
            }
        }).start();
    }

    private List<OrderItem> parseOrdersFromJson(String json) {
        // TODO: Implement real JSON parsing based on your backend response
        // For now, fallback to mock
        List<OrderItem> all = new ArrayList<>();
        all.add(new OrderItem(1, "Alice", "123 Main St", "Pizza x2, Coke x1", 250, "pending"));
        all.add(new OrderItem(2, "Bob", "456 Oak Ave", "Burger x1, Fries x2", 180, "accepted"));
        all.add(new OrderItem(3, "Charlie", "789 Pine Rd", "Salad x1", 90, "served"));
        all.add(new OrderItem(4, "Dana", "321 Maple St", "Pizza x1, Salad x1", 170, "pending"));
        all.add(new OrderItem(5, "Eve", "654 Elm St", "Burger x2", 200, "accepted"));
        return all;
    }

    private void updateOrderStatus(OrderItem order, String newStatus) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/orders/" + order.id + "/status");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                // TODO: Set JWT token if needed
                // conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);
                String json = String.format("{\"status\":\"%s\"}", newStatus);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                if (code == 200 || code == 204) {
                    Platform.runLater(this::loadOrdersFromBackend);
                } else {
                    // Optionally show error
                    Platform.runLater(() -> {
                        // TODO: Show error message
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    // TODO: Show error message
                });
            }
        }).start();
    }

    // Mock order loading for demonstration
    private void loadMockOrders() {
        List<OrderItem> all = new ArrayList<>();
        all.add(new OrderItem(1, "Alice", "123 Main St", "Pizza x2, Coke x1", 250, "pending"));
        all.add(new OrderItem(2, "Bob", "456 Oak Ave", "Burger x1, Fries x2", 180, "accepted"));
        all.add(new OrderItem(3, "Charlie", "789 Pine Rd", "Salad x1", 90, "served"));
        all.add(new OrderItem(4, "Dana", "321 Maple St", "Pizza x1, Salad x1", 170, "pending"));
        all.add(new OrderItem(5, "Eve", "654 Elm St", "Burger x2", 200, "accepted"));
        // Group by status
        pendingOrders.setAll(all.stream().filter(o -> o.status.equals("pending")).collect(Collectors.toList()));
        acceptedOrders.setAll(all.stream().filter(o -> o.status.equals("accepted")).collect(Collectors.toList()));
        servedOrders.setAll(all.stream().filter(o -> o.status.equals("served")).collect(Collectors.toList()));
    }

    // Order item for UI
    public static class OrderItem {
        public final int id;
        public final String customer;
        public final String address;
        public final String items;
        public final int total;
        public String status;
        public OrderItem(int id, String customer, String address, String items, int total, String status) {
            this.id = id; this.customer = customer; this.address = address; this.items = items; this.total = total; this.status = status;
        }
    }

    // Custom cell for displaying orders
    private class OrderCell extends ListCell<OrderItem> {
        private final String column;
        public OrderCell(String column) { this.column = column; }
        @Override
        protected void updateItem(OrderItem order, boolean empty) {
            super.updateItem(order, empty);
            if (empty || order == null) {
                setGraphic(null);
            } else {
                VBox box = new VBox(5);
                box.setPadding(new Insets(8));
                box.getChildren().add(new Label("Order #" + order.id + " - " + order.customer));
                box.getChildren().add(new Label("Address: " + order.address));
                box.getChildren().add(new Label("Items: " + order.items));
                box.getChildren().add(new Label("Total: " + order.total));
                box.getChildren().add(new Label("Status: " + order.status));
                if (column.equals("pending")) {
                    Button acceptBtn = new Button("Accept");
                    Button rejectBtn = new Button("Reject");
                    acceptBtn.setOnAction(e -> updateOrderStatus(order, "accepted"));
                    rejectBtn.setOnAction(e -> updateOrderStatus(order, "rejected"));
                    box.getChildren().add(new HBox(10, acceptBtn, rejectBtn));
                } else if (column.equals("accepted")) {
                    Button serveBtn = new Button("Mark as Served");
                    serveBtn.setOnAction(e -> updateOrderStatus(order, "served"));
                    box.getChildren().add(serveBtn);
                }
                setGraphic(box);
            }
        }
    }
} 