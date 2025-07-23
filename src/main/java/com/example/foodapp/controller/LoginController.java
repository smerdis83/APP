package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.util.prefs.Preferences;
import java.util.function.BiConsumer;

public class LoginController {
    @FXML public TextField phoneField;
    @FXML public PasswordField passwordField;
    @FXML public CheckBox rememberMe;
    @FXML public Button loginBtn;
    @FXML public Label messageLabel;
    @FXML public Hyperlink registerLink;

    private static final String PREF_KEY_PHONE = "rememberedPhone";
    private static final String PREF_KEY_REMEMBER = "rememberMe";
    private Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    // Callback to be set by main app for navigation
    private BiConsumer<String, String> onLoginSuccess; // (phone, password)
    private Runnable onRegister;

    public void setOnLoginSuccess(BiConsumer<String, String> callback) { this.onLoginSuccess = callback; }
    public void setOnRegister(Runnable callback) { this.onRegister = callback; }

    @FXML
    public void initialize() {
        boolean remembered = prefs.getBoolean(PREF_KEY_REMEMBER, false);
        if (remembered) {
            phoneField.setText(prefs.get(PREF_KEY_PHONE, ""));
            rememberMe.setSelected(true);
        }
        messageLabel.setText("");
        loginBtn.setOnAction(this::handleLogin);
        registerLink.setOnAction(e -> { if (onRegister != null) onRegister.run(); });
    }

    private void handleLogin(ActionEvent event) {
        String phone = phoneField.getText();
        String password = passwordField.getText();
        if (phone.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Phone and password required.");
            return;
        }
        if (rememberMe.isSelected()) {
            prefs.put(PREF_KEY_PHONE, phone);
            prefs.putBoolean(PREF_KEY_REMEMBER, true);
        } else {
            prefs.remove(PREF_KEY_PHONE);
            prefs.putBoolean(PREF_KEY_REMEMBER, false);
        }
        messageLabel.setText("");
        if (onLoginSuccess != null) onLoginSuccess.accept(phone, password);
    }

    public void showError(String msg) {
        messageLabel.setText(msg);
    }
    public void clearError() {
        messageLabel.setText("");
    }
} 