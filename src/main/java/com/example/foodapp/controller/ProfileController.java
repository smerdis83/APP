package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.function.Consumer;

public class ProfileController {
    @FXML public Label nameLabel;
    @FXML public Label phoneLabel;
    @FXML public Label emailLabel;
    @FXML public Label roleLabel;
    @FXML public Label addressLabel;
    @FXML public Label walletLabel;
    @FXML public Label enabledLabel;
    @FXML public Label createdLabel;
    @FXML public Label updatedLabel;
    @FXML public Button backBtn;

    private Runnable onBack;
    public void setOnBack(Runnable callback) { this.onBack = callback; }

    @FXML
    public void initialize() {
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void setProfile(String name, String phone, String email, String role, String address, String wallet, String enabled, String created, String updated) {
        nameLabel.setText(emptyOr(name));
        phoneLabel.setText(emptyOr(phone));
        emailLabel.setText(emptyOr(email));
        roleLabel.setText(emptyOr(role));
        addressLabel.setText(emptyOr(address));
        walletLabel.setText(emptyOr(wallet));
        enabledLabel.setText(emptyOr(enabled));
        createdLabel.setText(emptyOr(created));
        updatedLabel.setText(emptyOr(updated));
    }

    private String emptyOr(String s) {
        return (s == null || s.isEmpty() || s.equals("null")) ? "Empty" : s;
    }
} 