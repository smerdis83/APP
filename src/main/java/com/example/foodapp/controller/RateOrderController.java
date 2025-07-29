package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

public class RateOrderController {
    @FXML private RadioButton star1, star2, star3, star4, star5;
    @FXML private ToggleGroup ratingGroup;
    @FXML private TextArea commentField;
    @FXML private ImageView imageView1, imageView2, imageView3;
    @FXML private Button uploadBtn1, uploadBtn2, uploadBtn3;
    @FXML private Button submitBtn, backBtn;
    @FXML private Label messageLabel;

    private String jwtToken;
    private int orderId;
    private Consumer<Void> onBack;
    private List<String> imageBase64List = new ArrayList<>();

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setOnBack(Consumer<Void> cb) { this.onBack = cb; }

    @FXML
    public void initialize() {
        // Initialize the toggle group to ensure only one radio button can be selected
        if (ratingGroup == null) {
            ratingGroup = new ToggleGroup();
        }
        
        // Ensure all radio buttons are in the same toggle group
        star1.setToggleGroup(ratingGroup);
        star2.setToggleGroup(ratingGroup);
        star3.setToggleGroup(ratingGroup);
        star4.setToggleGroup(ratingGroup);
        star5.setToggleGroup(ratingGroup);
        
        uploadBtn1.setOnAction(e -> uploadImage(0, imageView1));
        uploadBtn2.setOnAction(e -> uploadImage(1, imageView2));
        uploadBtn3.setOnAction(e -> uploadImage(2, imageView3));
        submitBtn.setOnAction(e -> submitRating());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.accept(null); });
        imageBase64List.add(null); imageBase64List.add(null); imageBase64List.add(null);
    }

    private void uploadImage(int idx, ImageView imageView) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        Stage stage = (Stage) uploadBtn1.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                FileInputStream fis = new FileInputStream(selectedFile);
                byte[] bytes = fis.readAllBytes();
                fis.close();
                String base64 = Base64.getEncoder().encodeToString(bytes);
                imageBase64List.set(idx, base64);
                imageView.setImage(new Image(new ByteArrayInputStream(bytes)));
                messageLabel.setText("");
            } catch (Exception e) {
                messageLabel.setText("Error loading image: " + e.getMessage());
            }
        }
    }

    private void submitRating() {
        int rating = getSelectedRating();
        if (rating < 1 || rating > 5) {
            messageLabel.setText("Please select a rating (1-5 stars)");
            return;
        }
        String comment = commentField.getText();
        if (comment == null || comment.trim().isEmpty()) {
            messageLabel.setText("Please enter a comment");
            return;
        }
        List<String> images = new ArrayList<>();
        for (String b64 : imageBase64List) if (b64 != null) images.add(b64);
        submitBtn.setDisable(true);
        messageLabel.setText("Submitting...");
        new Thread(() -> {
            try {
                String json = buildJson(orderId, rating, comment, images);
                java.net.URL url = new java.net.URL("http://localhost:8000/ratings");
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
                Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                    if (code == 200) {
                        messageLabel.setStyle("-fx-text-fill: #27ae60;");
                        messageLabel.setText("Thank you for your rating!");
                        if (onBack != null) onBack.accept(null);
                    } else {
                        messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                        messageLabel.setText("Failed to submit rating: " + resp);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                    messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                    messageLabel.setText("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private int getSelectedRating() {
        if (star1.isSelected()) return 1;
        if (star2.isSelected()) return 2;
        if (star3.isSelected()) return 3;
        if (star4.isSelected()) return 4;
        if (star5.isSelected()) return 5;
        return 0;
    }

    private String buildJson(int orderId, int rating, String comment, List<String> images) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"order_id\":").append(orderId)
          .append(",\"rating\":").append(rating)
          .append(",\"comment\":\"").append(escapeJson(comment)).append("\"");
        if (!images.isEmpty()) {
            sb.append(",\"imageBase64\":[");
            for (int i = 0; i < images.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(images.get(i)).append("\"");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
} 