package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.util.function.Consumer;

public class RegisterController {
    @FXML public TextField nameField;
    @FXML public TextField phoneField;
    @FXML public PasswordField passwordField;
    @FXML public ComboBox<String> roleCombo;
    @FXML public Button registerBtn;
    @FXML public Label messageLabel;
    @FXML public Button backBtn;

    private Consumer<RegisterData> onRegister;
    private Runnable onBack;

    public void setOnRegister(Consumer<RegisterData> callback) { this.onRegister = callback; }
    public void setOnBack(Runnable callback) { this.onBack = callback; }

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("BUYER", "SELLER", "COURIER");
        roleCombo.setValue("BUYER");
        messageLabel.setText("");
        registerBtn.setOnAction(this::handleRegister);
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    private void handleRegister(ActionEvent event) {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String role = roleCombo.getValue();
        if (name.isEmpty() || phone.isEmpty() || password.isEmpty() || role == null) {
            messageLabel.setText("All fields are required.");
            return;
        }
        messageLabel.setText("");
        if (onRegister != null) {
            try {
                onRegister.accept(new RegisterData(name, phone, password, role));
                // Only navigate to login on successful registration
                // The navigation will be handled by the LoginApp.handleRegister method
            } catch (Exception e) {
                showError("Registration failed: " + e.getMessage());
            }
        }
    }

    public void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
    }
    public void clearError() {
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    public static class RegisterData {
        public final String name, phone, password, role;
        public RegisterData(String name, String phone, String password, String role) {
            this.name = name; this.phone = phone; this.password = password; this.role = role;
        }
    }
} 