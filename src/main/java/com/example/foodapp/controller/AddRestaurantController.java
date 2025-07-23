package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;

public class AddRestaurantController {
    @FXML private TextField nameField;
    @FXML private TextField logoField;
    @FXML private Button addBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField taxFeeField;
    @FXML private TextField additionalFeeField;

    private String jwtToken;
    private Runnable onBack;

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        addBtn.setOnAction(e -> handleAddRestaurant());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    private void handleAddRestaurant() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String logo = logoField.getText().trim();
        String taxFeeStr = taxFeeField.getText().trim();
        String additionalFeeStr = additionalFeeField.getText().trim();
        if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            messageLabel.setText("Name, address, and phone are required.");
            return;
        }
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append(String.format("\"name\":\"%s\",\"address\":\"%s\",\"phone\":\"%s\"", name, address, phone));
        if (!logo.isEmpty()) json.append(String.format(",\"logoBase64\":\"%s\"", logo));
        if (!taxFeeStr.isEmpty()) {
            try { Integer.parseInt(taxFeeStr); json.append(String.format(",\"tax_fee\":%s", taxFeeStr)); } catch (Exception ignore) {}
        }
        if (!additionalFeeStr.isEmpty()) {
            try { Integer.parseInt(additionalFeeStr); json.append(String.format(",\"additional_fee\":%s", additionalFeeStr)); } catch (Exception ignore) {}
        }
        json.append("}");
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
                    Platform.runLater(() -> messageLabel.setText("Restaurant added successfully!"));
                } else {
                    Platform.runLater(() -> messageLabel.setText("Failed: " + resp));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }
} 