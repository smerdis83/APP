package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class AddRestaurantController {
    @FXML private TextField nameField;
    @FXML private Button addBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;
    @FXML private TextField addressField;
    @FXML private TextField phoneField;
    @FXML private TextField taxFeeField;
    @FXML private TextField additionalFeeField;
    @FXML private ImageView logoImageView;
    @FXML private Button chooseLogoBtn;
    @FXML private TextArea descriptionField;
    @FXML private TextField workingHoursField;

    private String jwtToken;
    private Runnable onBack;
    private String logoBase64 = null;

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        addBtn.setOnAction(e -> handleAddRestaurant());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        chooseLogoBtn.setOnAction(e -> handleChooseLogo());
    }

    private void handleChooseLogo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Logo Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(logoImageView.getScene().getWindow());
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) baos.write(buf, 0, n);
                byte[] imgBytes = baos.toByteArray();
                this.logoBase64 = Base64.getEncoder().encodeToString(imgBytes);
                logoImageView.setImage(new Image(new java.io.ByteArrayInputStream(imgBytes)));
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to load image: " + ex.getMessage()).showAndWait();
            }
        }
    }

    private void handleAddRestaurant() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String phone = phoneField.getText().trim();
        String taxFeeStr = taxFeeField.getText().trim();
        String additionalFeeStr = additionalFeeField.getText().trim();
        String description = descriptionField.getText().trim();
        String workingHours = workingHoursField.getText().trim();
        if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
            messageLabel.setText("Name, address, and phone are required.");
            return;
        }
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append(String.format("\"name\":\"%s\",\"address\":\"%s\",\"phone\":\"%s\"", name, address, phone));
        if (logoBase64 != null && !logoBase64.isEmpty()) json.append(String.format(",\"logo_base64\":\"%s\"", logoBase64));
        if (!taxFeeStr.isEmpty()) {
            try { Integer.parseInt(taxFeeStr); json.append(String.format(",\"tax_fee\":%s", taxFeeStr)); } catch (Exception ignore) {}
        }
        if (!additionalFeeStr.isEmpty()) {
            try { Integer.parseInt(additionalFeeStr); json.append(String.format(",\"additional_fee\":%s", additionalFeeStr)); } catch (Exception ignore) {}
        }
        if (!description.isEmpty()) {
            json.append(String.format(",\"description\":\"%s\"", description.replace("\"", "\\\"")));
        }
        if (!workingHours.isEmpty()) {
            json.append(String.format(",\"working_hours\":\"%s\"", workingHours.replace("\"", "\\\"")));
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