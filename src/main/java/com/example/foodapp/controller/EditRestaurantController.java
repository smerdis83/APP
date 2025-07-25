package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import com.example.foodapp.controller.MyRestaurantsController.RestaurantItem;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class EditRestaurantController {
    @FXML private ImageView logoImageView;
    @FXML private Button selectImageBtn;
    @FXML private TextField nameField;
    @FXML private TextArea addressField;
    @FXML private TextField phoneField;
    @FXML private TextField taxFeeField;
    @FXML private TextField additionalFeeField;
    @FXML private Button saveBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private String jwtToken;
    private RestaurantItem restaurantItem;
    private Runnable onBack;
    private String selectedImageBase64;
    private ObjectMapper objectMapper = new ObjectMapper();

    public void setJwtToken(String token) {
        this.jwtToken = token;
    }

    public void setRestaurantItem(RestaurantItem item) {
        this.restaurantItem = item;
        loadRestaurantData();
    }

    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }

    @FXML
    public void initialize() {
        selectImageBtn.setOnAction(e -> selectImage());
        saveBtn.setOnAction(e -> saveChanges());
        backBtn.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });

        // Set up numeric validation for fee fields
        taxFeeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                taxFeeField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        additionalFeeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                additionalFeeField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private void loadRestaurantData() {
        if (restaurantItem == null) return;

        // Fetch detailed restaurant information from backend
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + restaurantItem.id);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                
                int code = conn.getResponseCode();
                if (code != 200) {
                    Platform.runLater(() -> messageLabel.setText("Failed to load restaurant details"));
                    return;
                }

                java.util.Scanner scanner = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String response = scanner.useDelimiter("\\A").next();
                scanner.close();

                // Parse the response and populate fields
                Platform.runLater(() -> populateFields(response));

            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Error loading restaurant: " + ex.getMessage()));
            }
        }).start();
    }

    private void populateFields(String jsonResponse) {
        try {
            // Simple JSON parsing for restaurant details
            nameField.setText(extractJsonValue(jsonResponse, "name"));
            addressField.setText(extractJsonValue(jsonResponse, "address"));
            phoneField.setText(extractJsonValue(jsonResponse, "phone"));
            
            String taxFee = extractJsonValue(jsonResponse, "tax_fee");
            if (taxFee != null && !taxFee.equals("null")) {
                taxFeeField.setText(taxFee);
            }
            
            String additionalFee = extractJsonValue(jsonResponse, "additional_fee");
            if (additionalFee != null && !additionalFee.equals("null")) {
                additionalFeeField.setText(additionalFee);
            }

            // Load existing logo if available
            String logoBase64 = extractJsonValue(jsonResponse, "logo_base64");
            if (logoBase64 != null && !logoBase64.equals("null") && logoBase64.length() > 20) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(logoBase64);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    logoImageView.setImage(image);
                    selectedImageBase64 = logoBase64;
                } catch (Exception e) {
                    // Ignore invalid base64 data
                }
            }

        } catch (Exception e) {
            messageLabel.setText("Error parsing restaurant data: " + e.getMessage());
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        startIndex += searchKey.length();
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }
        
        if (startIndex >= json.length()) return null;
        
        char startChar = json.charAt(startIndex);
        if (startChar == '"') {
            // String value
            startIndex++;
            int endIndex = json.indexOf('"', startIndex);
            if (endIndex == -1) return null;
            return json.substring(startIndex, endIndex);
        } else {
            // Numeric value
            int endIndex = startIndex;
            while (endIndex < json.length() && 
                   (Character.isDigit(json.charAt(endIndex)) || json.charAt(endIndex) == '.')) {
                endIndex++;
            }
            if (endIndex == startIndex) return null;
            return json.substring(startIndex, endIndex);
        }
    }

    private void selectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Restaurant Logo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) selectImageBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            try {
                // Read and encode image to base64
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                byte[] imageBytes = fileInputStream.readAllBytes();
                fileInputStream.close();

                selectedImageBase64 = Base64.getEncoder().encodeToString(imageBytes);
                
                // Display image preview
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                logoImageView.setImage(image);
                
                messageLabel.setText("Image selected successfully");
                messageLabel.setStyle("-fx-text-fill: #4CAF50;");

            } catch (Exception e) {
                messageLabel.setText("Error loading image: " + e.getMessage());
                messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        }
    }

    private void saveChanges() {
        // Validate required fields
        if (nameField.getText().trim().isEmpty()) {
            messageLabel.setText("Restaurant name is required");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        if (addressField.getText().trim().isEmpty()) {
            messageLabel.setText("Address is required");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        if (phoneField.getText().trim().isEmpty()) {
            messageLabel.setText("Phone number is required");
            messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        // Prepare update data
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", nameField.getText().trim());
        updateData.put("address", addressField.getText().trim());
        updateData.put("phone", phoneField.getText().trim());

        // Add tax fee if provided
        if (!taxFeeField.getText().trim().isEmpty()) {
            try {
                int taxFee = Integer.parseInt(taxFeeField.getText().trim());
                updateData.put("tax_fee", taxFee);
            } catch (NumberFormatException e) {
                messageLabel.setText("Tax fee must be a valid number");
                messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }
        }

        // Add additional fee if provided
        if (!additionalFeeField.getText().trim().isEmpty()) {
            try {
                int additionalFee = Integer.parseInt(additionalFeeField.getText().trim());
                updateData.put("additional_fee", additionalFee);
            } catch (NumberFormatException e) {
                messageLabel.setText("Additional fee must be a valid number");
                messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }
        }

        // Add logo if selected
        if (selectedImageBase64 != null) {
            updateData.put("logoBase64", selectedImageBase64);
        }

        // Disable save button during request
        saveBtn.setDisable(true);
        messageLabel.setText("Saving changes...");
        messageLabel.setStyle("-fx-text-fill: #2196f3;");

        // Send update request
        new Thread(() -> {
            try {
                String jsonData = objectMapper.writeValueAsString(updateData);
                
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + restaurantItem.id);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);

                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(jsonData.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                String response;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    response = scanner.hasNext() ? scanner.next() : "";
                }

                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    if (code == 200) {
                        messageLabel.setText("Restaurant updated successfully!");
                        messageLabel.setStyle("-fx-text-fill: #4CAF50;");
                        
                        // Auto-navigate back after 2 seconds
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> {
                                    if (onBack != null) onBack.run();
                                });
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }).start();
                    } else {
                        String errorMsg = "Failed to update restaurant";
                        if (response.contains("error")) {
                            try {
                                String error = extractJsonValue(response, "error");
                                if (error != null) errorMsg = error;
                            } catch (Exception e) {
                                // Use default error message
                            }
                        }
                        messageLabel.setText(errorMsg);
                        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    messageLabel.setText("Error: " + ex.getMessage());
                    messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                });
            }
        }).start();
    }
}