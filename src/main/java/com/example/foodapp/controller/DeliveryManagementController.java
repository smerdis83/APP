package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpRequest.BodyPublishers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class DeliveryManagementController {
    @FXML private ListView<OrderItem> availableList;
    @FXML private ListView<OrderItem> receivedList;
    @FXML private ListView<OrderItem> deliveredList;
    @FXML private ListView<OrderItem> allList;
    @FXML private ListView<OrderItem> acceptedList;
    @FXML private Label messageLabel;
    @FXML private Button backBtn;

    private ObservableList<OrderItem> allOrders = FXCollections.observableArrayList();
    private ObservableList<OrderItem> acceptedOrders = FXCollections.observableArrayList();
    private ObservableList<OrderItem> receivedOrders = FXCollections.observableArrayList();
    private ObservableList<OrderItem> deliveredOrders = FXCollections.observableArrayList();
    private String jwtToken;
    private Runnable onBack;

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        if (backBtn != null) backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        allList.setItems(allOrders);
        acceptedList.setItems(acceptedOrders);
        receivedList.setItems(receivedOrders);
        deliveredList.setItems(deliveredOrders);
        allList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderItem order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.getChildren().add(new Label("Address: " + order.address));
                    box.getChildren().add(new Label("Total: " + order.total));
                    box.getChildren().add(new Label("Status: " + order.status));
                    if (order.status.equals("served")) {
                        HBox btnBox = new HBox(10);
                        Button acceptBtn = new Button("Accept");
                        acceptBtn.setOnAction(e -> updateOrderStatus(order, "accepted"));
                        Button rejectBtn = new Button("Reject");
                        rejectBtn.setOnAction(e -> updateOrderStatus(order, "rejected"));
                        btnBox.getChildren().addAll(acceptBtn, rejectBtn);
                        box.getChildren().add(btnBox);
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
                    box.getChildren().add(new Label("Address: " + order.address));
                    box.getChildren().add(new Label("Total: " + order.total));
                    Button receivedBtn = new Button("Mark as Received");
                    receivedBtn.setOnAction(e -> updateOrderStatus(order, "received"));
                    box.getChildren().add(receivedBtn);
                    setGraphic(box);
                }
            }
        });
        receivedList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderItem order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.getChildren().add(new Label("Address: " + order.address));
                    box.getChildren().add(new Label("Total: " + order.total));
                    Button deliveredBtn = new Button("Mark as Delivered");
                    deliveredBtn.setOnAction(e -> updateOrderStatus(order, "delivered"));
                    box.getChildren().add(deliveredBtn);
                    setGraphic(box);
                }
            }
        });
        deliveredList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(OrderItem order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    VBox box = new VBox(5);
                    box.getChildren().add(new Label("Address: " + order.address));
                    box.getChildren().add(new Label("Total: " + order.total));
                    box.getChildren().add(new Label("Status: Delivered"));
                    setGraphic(box);
                }
            }
        });
        fetchAvailableOrders();
        fetchMyDeliveries();
    }

    private void fetchAvailableOrders() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/deliveries/available"))
                    .GET();
                if (jwtToken != null) builder.header("Authorization", "Bearer " + jwtToken);
                HttpRequest request = builder.build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                List<OrderItem> all = parseOrdersFromJson(response.body());
                Platform.runLater(() -> allOrders.setAll(
                    all.stream().filter(o -> o.status.equals("served")).toList()
                ));
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void fetchMyDeliveries() {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8000/deliveries/history"))
                    .GET();
                if (jwtToken != null) builder.header("Authorization", "Bearer " + jwtToken);
                HttpRequest request = builder.build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                List<OrderItem> all = parseOrdersFromJson(response.body());
                Platform.runLater(() -> {
                    acceptedOrders.setAll(all.stream().filter(o -> o.status.equals("accepted")).toList());
                    receivedOrders.setAll(all.stream().filter(o -> o.status.equals("received")).toList());
                    deliveredOrders.setAll(all.stream().filter(o -> o.status.equals("delivered")).toList());
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }).start();
    }

    private void updateOrderStatus(OrderItem order, String newStatus) {
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                String url = "http://localhost:8000/deliveries/" + order.id;
                String json = String.format("{\"status\":\"%s\"}", newStatus);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .method("PATCH", BodyPublishers.ofString(json));
                if (jwtToken != null) builder.header("Authorization", "Bearer " + jwtToken);
                HttpRequest request = builder.build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                int code = response.statusCode();
                if (code == 200 || code == 204) {
                    Platform.runLater(() -> {
                        fetchAvailableOrders();
                        fetchMyDeliveries();
                        showMessage("Order status updated: " + newStatus);
                    });
                }
            } catch (Exception ex) { ex.printStackTrace(); }
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
                    all.add(new OrderItem(id, address, total, status));
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
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