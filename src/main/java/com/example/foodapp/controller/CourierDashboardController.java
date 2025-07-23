package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class CourierDashboardController {
    @FXML public Label welcomeLabel;
    @FXML public Label roleLabel;
    @FXML public Button profileBtn;
    @FXML public Button logoutBtn;
    @FXML public VBox roleContent;

    private Consumer<Void> onProfile;
    public void setOnProfile(Consumer<Void> callback) { this.onProfile = callback; }

    private Runnable onLogout;
    public void setOnLogout(Runnable callback) { this.onLogout = callback; }

    @FXML
    public void initialize() {
        if (profileBtn != null) profileBtn.setOnAction(e -> { if (onProfile != null) onProfile.accept(null); });
        if (logoutBtn != null) logoutBtn.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
    }

    public void setWelcome(String name) { welcomeLabel.setText("Welcome, " + name + "!"); }
    public void setRole(String role) { roleLabel.setText("Role: " + role); }
    public void setRoleContent(javafx.scene.Node... nodes) { roleContent.getChildren().setAll(nodes); }
} 