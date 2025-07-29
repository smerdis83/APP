package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import javafx.scene.layout.HBox;

public class OrderHistoryController {
    @FXML public ListView<OrderSummary> orderList;
    @FXML public Label messageLabel;
    @FXML public Button backBtn;
    @FXML public Label idLabel;
    @FXML public Label restaurantLabel;
    @FXML public Label statusLabel;
    @FXML public Label totalLabel;
    @FXML public Label createdLabel;
    @FXML public Label updatedLabel;
    @FXML public ListView<String> itemsList;

    private Runnable onBack;
    private Consumer<OrderSummary> onOrderSelected;
    private ObservableList<OrderSummary> orders = FXCollections.observableArrayList();
    private String jwtToken;
    private boolean activeOnly = false;
    private java.util.Map<String, Boolean> commentedOrders = new java.util.HashMap<>();

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable callback) { this.onBack = callback; }
    public void setOnOrderSelected(Consumer<OrderSummary> callback) { this.onOrderSelected = callback; }
    public void setActiveOnly(boolean activeOnly) { this.activeOnly = activeOnly; }

    public interface OnRateOrder {
        void rateOrder(OrderSummary order);
    }
    private OnRateOrder onRateOrder;
    public void setOnRateOrder(OnRateOrder cb) { this.onRateOrder = cb; }

    @FXML
    public void initialize() {
        orderList.setItems(orders);
        orderList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(OrderSummary order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    String restaurant = (order.restaurant == null || order.restaurant.isEmpty()) ? "Unknown Restaurant" : order.restaurant;
                    String price = (order.total == null || order.total.isEmpty()) ? "-" : order.total;
                    String status = order.status == null ? "" : order.status.toLowerCase();
                    String displayStatus = status;
                    String color = "";
                    if (status.contains("waiting for vendor") || status.contains("paid") || status.equals("submitted")) {
                        displayStatus = "Waiting for Vendor / Paid";
                        color = "#ffb6c1";
                    } else if (status.contains("waiting vendor")) {
                        displayStatus = "Waiting for Vendor";
                        color = "#ffa500";
                    } else if (status.equals("accepted")) {
                        if (order.courierId != null && !order.courierId.isEmpty() && !order.courierId.equals("0")) {
                            displayStatus = "Courier in the way of restaurant";
                            color = "#dc3545";
                        } else {
                            displayStatus = "Food is preparing in the restaurant";
                            color = "#28a745";
                        }
                    } else if (status.contains("served")) {
                        displayStatus = "Waiting for courier";
                        color = "#c71585";
                    } else if (status.contains("received")) {
                        displayStatus = "Courier on the way to the address";
                        color = "#ffe066";
                    } else if (status.contains("delivered")) {
                        displayStatus = "Delivered";
                        color = "#b3e0ff";
                    }
                    setStyle("-fx-background-color: " + color + ";");

                    Label infoLabel = new Label(restaurant + " | " + price + " | " + displayStatus);
                    infoLabel.setMaxWidth(Double.MAX_VALUE);
                    infoLabel.setStyle("-fx-padding: 0 10 0 0;");

                    if (status.contains("delivered")) {
                        Button rateBtn = new Button("Comment");
                        rateBtn.setOnAction(e -> {
                            if (onRateOrder != null) onRateOrder.rateOrder(order);
                        });
                        // Check if already commented
                        Boolean commented = commentedOrders.get(order.id);
                        if (commented == null) {
                            // Fetch from backend
                            rateBtn.setDisable(true);
                            rateBtn.setText("Checking...");
                            new Thread(() -> {
                                try {
                                    java.net.URL url = new java.net.URL("http://localhost:8000/ratings/order/" + order.id);
                                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                                    conn.setRequestMethod("GET");
                                    if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                                    int code = conn.getResponseCode();
                                    boolean isCommented = (code == 200);
                                    Platform.runLater(() -> {
                                        commentedOrders.put(order.id, isCommented);
                                        orderList.refresh();
                                    });
                                } catch (Exception ex) {
                                    Platform.runLater(() -> {
                                        commentedOrders.put(order.id, false);
                                        orderList.refresh();
                                    });
                                }
                            }).start();
                        } else if (commented) {
                            rateBtn.setDisable(true);
                            rateBtn.setText("Commented!");
                            rateBtn.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        }
                        HBox hbox = new HBox(10, infoLabel, rateBtn);
                        hbox.setFillHeight(true);
                        setGraphic(hbox);
                        setText(null);
                    } else {
                        setGraphic(infoLabel);
                        setText(null);
                    }
                }
            }
        });
        orderList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showOrderDetails(newVal);
                if (onOrderSelected != null) onOrderSelected.accept(newVal);
            }
        });
        if (backBtn != null) backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void setOrders(List<OrderSummary> orderList) {
        orders.setAll(orderList);
    }
    public void showMessage(String msg) { messageLabel.setText(msg); }
    public void clearMessage() { messageLabel.setText(""); }

    private void showOrderDetails(OrderSummary order) {
        // Show address and status for the selected order
        fetchOrderDetails(order.id);
        fetchOrderItems(order.id);
    }
    private void fetchOrderDetails(String orderId) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/orders/" + orderId);
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
                String address = "";
                String status = "";
                int addrIdx = resp.indexOf("\"delivery_address\":");
                if (addrIdx != -1) {
                    int addrStart = resp.indexOf('"', addrIdx + 19) + 1;
                    int addrEnd = resp.indexOf('"', addrStart);
                    if (addrStart > 0 && addrEnd > addrStart) address = resp.substring(addrStart, addrEnd);
                }
                int statusIdx = resp.indexOf("\"status\":");
                if (statusIdx != -1) {
                    int statusStart = resp.indexOf('"', statusIdx + 9) + 1;
                    int statusEnd = resp.indexOf('"', statusStart);
                    if (statusStart > 0 && statusEnd > statusStart) status = resp.substring(statusStart, statusEnd);
                }
                final String fAddress = address;
                final String fStatus = status;
                Platform.runLater(() -> {
                    statusLabel.setText("Status: " + (fStatus.isEmpty() ? "-" : fStatus));
                    createdLabel.setText("");
                    updatedLabel.setText("");
                    idLabel.setText("");
                    restaurantLabel.setText("");
                    totalLabel.setText("Address: " + (fAddress.isEmpty() ? "-" : fAddress));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    statusLabel.setText("Status: Error");
                    totalLabel.setText("Address: Error");
                });
            }
        }).start();
    }

    private void fetchOrderItems(String orderId) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/orders/" + orderId);
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
                List<String> items = new ArrayList<>();
                int arrIdx = resp.indexOf("\"items\":");
                if (arrIdx != -1) {
                    int arrStart = resp.indexOf('[', arrIdx);
                    int arrEnd = resp.indexOf(']', arrStart);
                    if (arrStart != -1 && arrEnd != -1) {
                        String arr = resp.substring(arrStart + 1, arrEnd);
                        int idx = 0;
                        while ((idx = arr.indexOf("{", idx)) != -1) {
                            int idIdx = arr.indexOf("\"item_id\":", idx);
                            int qtyIdx = arr.indexOf("\"quantity\":", idx);
                            if (idIdx == -1 || qtyIdx == -1) break;
                            int idStart = idIdx + 10;
                            int idEnd = arr.indexOf(',', idStart);
                            if (idEnd == -1) idEnd = arr.indexOf('}', idStart);
                            String idStr = arr.substring(idStart, idEnd).replaceAll("[^0-9]", "").trim();
                            int itemId = Integer.parseInt(idStr);
                            int qtyStart = qtyIdx + 10;
                            int qtyEnd = arr.indexOf(',', qtyStart);
                            if (qtyEnd == -1) qtyEnd = arr.indexOf('}', qtyStart);
                            String qtyStr = arr.substring(qtyStart, qtyEnd).replaceAll("[^0-9]", "").trim();
                            int quantity = Integer.parseInt(qtyStr);
                            // Fetch item name from /items/{id}
                            String name = "Item #" + itemId;
                            try {
                                java.net.URL itemUrl = new java.net.URL("http://localhost:8000/items/" + itemId);
                                java.net.HttpURLConnection itemConn = (java.net.HttpURLConnection) itemUrl.openConnection();
                                itemConn.setRequestMethod("GET");
                                itemConn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                                int itemCode = itemConn.getResponseCode();
                                String itemResp;
                                try (java.util.Scanner scanner = new java.util.Scanner(
                                        itemCode >= 200 && itemCode < 300 ? itemConn.getInputStream() : itemConn.getErrorStream(),
                                        java.nio.charset.StandardCharsets.UTF_8)) {
                                    scanner.useDelimiter("\\A");
                                    itemResp = scanner.hasNext() ? scanner.next() : "";
                                }
                                int nameIdx = itemResp.indexOf("\"name\":");
                                if (nameIdx != -1) {
                                    int nameStart = itemResp.indexOf('"', nameIdx + 7) + 1;
                                    int nameEnd = itemResp.indexOf('"', nameStart);
                                    if (nameStart > 0 && nameEnd > nameStart) {
                                        name = itemResp.substring(nameStart, nameEnd);
                                    }
                                }
                            } catch (Exception e) {
                                // fallback to Item #id
                            }
                            items.add(name + " x" + quantity);
                            idx = arr.indexOf('}', qtyEnd) + 1;
                        }
                    }
                }
                Platform.runLater(() -> itemsList.getItems().setAll(items));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> itemsList.getItems().setAll(List.of("Error loading items")));
            }
        }).start();
    }

    // OrderSummary class for display
    public static class OrderSummary {
        public final String id, restaurant, status, total, created, updated, courierId;
        public OrderSummary(String id, String restaurant, String status, String total, String created, String updated, String courierId) {
            this.id = id; this.restaurant = restaurant; this.status = status; this.total = total; this.created = created; this.updated = updated; this.courierId = courierId;
        }
        @Override public String toString() {
            return "#" + id + " | " + restaurant + " | " + status + " | " + total;
        }
    }
} 