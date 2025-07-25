package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

public class CourierDashboardController {
    @FXML public Label welcomeLabel;
    @FXML public Label roleLabel;
    @FXML public Button deliveryManagementBtn;
    @FXML public Button logoutBtn;
    @FXML public Button profileBtn;

    private Runnable onDeliveryManagement;
    private Runnable onLogout;
    private Runnable onProfile;
    public void setOnDeliveryManagement(Runnable callback) { this.onDeliveryManagement = callback; }
    public void setOnLogout(Runnable callback) { this.onLogout = callback; }
    public void setOnProfile(Runnable callback) { this.onProfile = callback; }

    public void setWelcome(String name) { welcomeLabel.setText("Welcome, " + name + "!"); }
    public void setRole(String role) { roleLabel.setText("Role: " + role); }

    @FXML
    public void initialize() {
        if (deliveryManagementBtn != null) deliveryManagementBtn.setOnAction(e -> { if (onDeliveryManagement != null) onDeliveryManagement.run(); });
        if (logoutBtn != null) logoutBtn.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        if (profileBtn != null) profileBtn.setOnAction(e -> { if (onProfile != null) onProfile.run(); });
    }
} 