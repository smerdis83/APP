package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PaymentController {
    @FXML private Label restaurantLabel;
    @FXML private Label addressLabel;
    @FXML private ListView<Item> itemList;
    @FXML private Label totalLabel;
    @FXML private TextField cardField;
    @FXML private Button payBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;
    @FXML private VBox paymentOptionsBox;
    @FXML private Button onlinePaymentBtn;
    @FXML private Button walletPaymentBtn;
    @FXML private Button topUpBtn;
    @FXML private VBox onlinePaymentBox;
    @FXML private VBox walletPaymentBox;
    @FXML private Button payWalletBtn;
    @FXML private Button backWalletBtn;
    @FXML private Label walletErrorLabel;
    @FXML private Button topUpWalletBtn;
    @FXML private Button backToOrderBtn;

    private String jwtToken;
    private Runnable onBack;
    private Runnable onSuccess;
    private Runnable onTopUp;
    private String restaurantName;
    private int restaurantId;
    private String address;
    private List<Item> items = new ArrayList<>();
    private int total = 0;
    private ObservableList<Item> observableItems = FXCollections.observableArrayList();
    private com.example.foodapp.LoginApp app;
    private int lackingAmount = 0;
    private String logoBase64;

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }
    public void setOnSuccess(Runnable r) { this.onSuccess = r; }
    public void setOnTopUp(Runnable r) { this.onTopUp = r; }
    public void setApp(com.example.foodapp.LoginApp app) { this.app = app; }
    public void setOrderDetails(String restaurantName, int restaurantId, String address, List<Item> items, int total) {
        this.restaurantName = restaurantName;
        this.restaurantId = restaurantId;
        this.address = address;
        this.items = items;
        this.total = total;
        restaurantLabel.setText("Restaurant: " + restaurantName);
        addressLabel.setText("Address: " + address);
        observableItems.setAll(items);
        itemList.setItems(observableItems);
        itemList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name + " x" + item.quantity + " (" + item.price + ")");
            }
        });
        totalLabel.setText("Total: " + total);
        showPaymentOptions();
    }
    public void setLogoBase64(String logoBase64) { this.logoBase64 = logoBase64; }

    @FXML
    public void initialize() {
        onlinePaymentBtn.setOnAction(e -> showOnlinePayment());
        walletPaymentBtn.setOnAction(e -> showWalletPayment());
        topUpBtn.setOnAction(e -> { if (onTopUp != null) onTopUp.run(); });
        payBtn.setOnAction(e -> handlePayOnline());
        backBtn.setOnAction(e -> showPaymentOptions());
        payWalletBtn.setOnAction(e -> handlePayWallet());
        backWalletBtn.setOnAction(e -> showPaymentOptions());
        topUpWalletBtn.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) topUpWalletBtn.getScene().getWindow();
            // Fetch wallet balance before opening top-up page
            new Thread(() -> {
                try {
                    java.net.URL url = new java.net.URL("http://localhost:8000/wallet/balance");
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                    int code = conn.getResponseCode();
                    String resp;
                    try (java.util.Scanner scanner = new java.util.Scanner(
                            code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                            java.nio.charset.StandardCharsets.UTF_8)) {
                        scanner.useDelimiter("\\A");
                        resp = scanner.hasNext() ? scanner.next() : "";
                    }
                    int balance = 0;
                    if (code == 200 && resp.contains("wallet_balance")) {
                        int idx = resp.indexOf(":");
                        int end = resp.indexOf("}", idx);
                        String balStr = resp.substring(idx + 1, end).replaceAll("[^0-9]", "").trim();
                        balance = balStr.isEmpty() ? 0 : Integer.parseInt(balStr);
                    }
                    int lacking = total - balance;
                    if (lacking < 0) lacking = 0;
                    int finalLacking = lacking;
                    Platform.runLater(() -> {
                        java.util.List<com.example.foodapp.controller.RestaurantPageController.BasketItem> basketItems = items.stream()
                            .map(i -> new com.example.foodapp.controller.RestaurantPageController.BasketItem(
                                new com.example.foodapp.controller.RestaurantPageController.FoodItem(i.id, i.name, i.price, i.quantity),
                                i.quantity))
                            .collect(Collectors.toList());
                        app.showTopUpWalletPage(stage, jwtToken, () -> app.showPaymentPage(stage, restaurantId, restaurantName, logoBase64, address, basketItems, jwtToken, onBack), finalLacking);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        java.util.List<com.example.foodapp.controller.RestaurantPageController.BasketItem> basketItems = items.stream()
                            .map(i -> new com.example.foodapp.controller.RestaurantPageController.BasketItem(
                                new com.example.foodapp.controller.RestaurantPageController.FoodItem(i.id, i.name, i.price, i.quantity),
                                i.quantity))
                            .collect(Collectors.toList());
                        app.showTopUpWalletPage(stage, jwtToken, () -> app.showPaymentPage(stage, restaurantId, restaurantName, logoBase64, address, basketItems, jwtToken, onBack), lackingAmount);
                    });
                }
            }).start();
        });
        walletErrorLabel.setVisible(false); walletErrorLabel.setManaged(false);
        topUpWalletBtn.setVisible(false); topUpWalletBtn.setManaged(false);
        showPaymentOptions();
        backToOrderBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    private void showPaymentOptions() {
        paymentOptionsBox.setVisible(true); paymentOptionsBox.setManaged(true);
        onlinePaymentBox.setVisible(false); onlinePaymentBox.setManaged(false);
        walletPaymentBox.setVisible(false); walletPaymentBox.setManaged(false);
        walletErrorLabel.setVisible(false); walletErrorLabel.setManaged(false);
        topUpWalletBtn.setVisible(false); topUpWalletBtn.setManaged(false);
        messageLabel.setText("");
    }
    private void showOnlinePayment() {
        paymentOptionsBox.setVisible(false); paymentOptionsBox.setManaged(false);
        onlinePaymentBox.setVisible(true); onlinePaymentBox.setManaged(true);
        walletPaymentBox.setVisible(false); walletPaymentBox.setManaged(false);
        messageLabel.setText("");
    }
    private void showWalletPayment() {
        paymentOptionsBox.setVisible(false); paymentOptionsBox.setManaged(false);
        onlinePaymentBox.setVisible(false); onlinePaymentBox.setManaged(false);
        walletPaymentBox.setVisible(true); walletPaymentBox.setManaged(true);
        messageLabel.setText("");
    }

    private void handlePayOnline() {
        handleOrder(false);
    }
    private void handlePayWallet() {
        handleOrder(true);
    }
    private void handleOrder(boolean useWallet) {
        if (items.isEmpty()) {
            messageLabel.setText("No items to order.");
            return;
        }
        if (address == null || address.isEmpty()) {
            messageLabel.setText("No address selected.");
            return;
        }
        // Build order JSON
        StringBuilder itemsJson = new StringBuilder();
        itemsJson.append("[");
        boolean first = true;
        for (Item b : items) {
            if (!first) itemsJson.append(",");
            itemsJson.append(String.format("{\"item_id\":%d,\"quantity\":%d}", b.id, b.quantity));
            first = false;
        }
        itemsJson.append("]");
        String orderJson = String.format("{\"delivery_address\":\"%s\",\"vendor_id\":%d,\"coupon_id\":0,\"items\":%s,\"use_wallet\":%s}",
                address.replace("\"", "'"), restaurantId, itemsJson, useWallet ? "true" : "false");
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/orders");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(orderJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
                    // Parse order ID from response
                    int orderId = -1;
                    try {
                        int idx = resp.indexOf("\"id\":");
                        if (idx != -1) {
                            int end = resp.indexOf(',', idx);
                            if (end == -1) end = resp.indexOf('}', idx);
                            String idStr = resp.substring(idx + 5, end).replaceAll("[^0-9]", "").trim();
                            orderId = Integer.parseInt(idStr);
                        }
                    } catch (Exception ignore) {}
                    if (useWallet && orderId != -1) {
                        // Now POST to /payment/online
                        int finalOrderId = orderId;
                        new Thread(() -> {
                            try {
                                java.net.URL payUrl = new java.net.URL("http://localhost:8000/payment/online");
                                java.net.HttpURLConnection payConn = (java.net.HttpURLConnection) payUrl.openConnection();
                                payConn.setRequestMethod("POST");
                                payConn.setRequestProperty("Content-Type", "application/json");
                                payConn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                                payConn.setDoOutput(true);
                                String payJson = String.format("{\"order_id\":%d,\"method\":\"wallet\"}", finalOrderId);
                                try (java.io.OutputStream os = payConn.getOutputStream()) {
                                    os.write(payJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                }
                                int payCode = payConn.getResponseCode();
                                String payResp;
                                try (java.util.Scanner scanner = new java.util.Scanner(
                                        payCode >= 200 && payCode < 300 ? payConn.getInputStream() : payConn.getErrorStream(),
                                        java.nio.charset.StandardCharsets.UTF_8)) {
                                    scanner.useDelimiter("\\A");
                                    payResp = scanner.hasNext() ? scanner.next() : "";
                                }
                                if (payCode == 200 || payCode == 201) {
                                    Platform.runLater(() -> {
                                        walletErrorLabel.setVisible(false); walletErrorLabel.setManaged(false);
                                        topUpWalletBtn.setVisible(false); topUpWalletBtn.setManaged(false);
                                        messageLabel.setText("");
                                        if (onSuccess != null) onSuccess.run();
                                    });
                                } else if (payResp.contains("Insufficient wallet balance")) {
                                    int lacking = total;
                                    if (payResp.matches(".*current balance ([0-9]+).*")) {
                                        try {
                                            String[] parts = payResp.split("current balance ");
                                            int bal = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                                            lacking = total - bal;
                                        } catch (Exception ignore) {}
                                    }
                                    int finalLacking = lacking;
                                    Platform.runLater(() -> {
                                        walletErrorLabel.setText("Your wallet does not have enough money. You need " + finalLacking + " more to complete this order.");
                                        walletErrorLabel.setVisible(true); walletErrorLabel.setManaged(true);
                                        topUpWalletBtn.setVisible(true); topUpWalletBtn.setManaged(true);
                                        lackingAmount = finalLacking;
                                    });
                                } else {
                                    Platform.runLater(() -> messageLabel.setText("Payment failed: " + payResp));
                                }
                            } catch (Exception ex) {
                                Platform.runLater(() -> messageLabel.setText("Payment error: " + ex.getMessage()));
                            }
                        }).start();
                    } else {
                        Platform.runLater(() -> {
                            messageLabel.setText("");
                            if (onSuccess != null) onSuccess.run();
                        });
                    }
                } else if (useWallet && (resp.contains("Insufficient wallet balance") || resp.contains("insufficient wallet balance"))) {
                    int lacking = total;
                    if (resp.matches(".*current balance ([0-9]+).*")) {
                        try {
                            String[] parts = resp.split("current balance ");
                            int bal = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
                            lacking = total - bal;
                        } catch (Exception ignore) {}
                    }
                    int finalLacking = lacking;
                    Platform.runLater(() -> {
                        walletErrorLabel.setText("Your wallet does not have enough money. You need " + finalLacking + " more to complete this order.");
                        walletErrorLabel.setVisible(true); walletErrorLabel.setManaged(true);
                        topUpWalletBtn.setVisible(true); topUpWalletBtn.setManaged(true);
                        lackingAmount = finalLacking;
                    });
                } else {
                    Platform.runLater(() -> {
                        walletErrorLabel.setVisible(false); walletErrorLabel.setManaged(false);
                        topUpWalletBtn.setVisible(false); topUpWalletBtn.setManaged(false);
                        messageLabel.setText("Order failed: " + resp);
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Order error: " + ex.getMessage()));
            }
        }).start();
    }

    public static class Item {
        public final int id;
        public final String name;
        public final int quantity;
        public final int price;
        public Item(int id, String name, int quantity, int price) {
            this.id = id; this.name = name; this.quantity = quantity; this.price = price;
        }
        @Override public String toString() { return name + " x" + quantity + " (" + price + ")"; }
    }
} 