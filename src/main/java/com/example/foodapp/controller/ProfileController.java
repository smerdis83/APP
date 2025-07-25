package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.function.Consumer;

public class ProfileController {
    @FXML public TextField nameField;
    @FXML public TextField phoneField;
    @FXML public TextField emailField;
    @FXML public Label roleLabel;
    @FXML public TextField addressField;
    @FXML public Label walletLabel;
    @FXML public Label enabledLabel;
    @FXML public Label createdLabel;
    @FXML public Label updatedLabel;
    @FXML public Button backBtn;
    @FXML public Button saveBtn;
    @FXML public ImageView profileImageView;
    @FXML public Button changePicBtn;

    private Runnable onBack;
    private String profileImageBase64 = null;
    private Consumer<ProfileData> onSave;
    public void setOnBack(Runnable callback) { this.onBack = callback; }
    public void setOnSave(Consumer<ProfileData> callback) { this.onSave = callback; }

    @FXML
    public void initialize() {
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        saveBtn.setOnAction(e -> handleSave());
        changePicBtn.setOnAction(e -> handleChangePicture());
    }

    public void setProfile(String name, String phone, String email, String role, String address, String wallet, String enabled, String created, String updated, String profileImageBase64) {
        nameField.setText(emptyOr(name));
        phoneField.setText(emptyOr(phone));
        emailField.setText(emptyOr(email));
        roleLabel.setText(emptyOr(role));
        addressField.setText(emptyOr(address));
        walletLabel.setText(emptyOr(wallet));
        enabledLabel.setText(emptyOr(enabled));
        this.profileImageBase64 = profileImageBase64;
        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(profileImageBase64);
                profileImageView.setImage(new Image(new java.io.ByteArrayInputStream(imgBytes)));
            } catch (Exception e) { profileImageView.setImage(null); }
        } else {
            profileImageView.setImage(null);
        }
    }

    private void handleChangePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());
        if (file != null) {
            try (FileInputStream fis = new FileInputStream(file); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) baos.write(buf, 0, n);
                byte[] imgBytes = baos.toByteArray();
                this.profileImageBase64 = Base64.getEncoder().encodeToString(imgBytes);
                profileImageView.setImage(new Image(new java.io.ByteArrayInputStream(imgBytes)));
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to load image: " + ex.getMessage()).showAndWait();
            }
        }
    }

    private void handleSave() {
        if (onSave != null) {
            onSave.accept(new ProfileData(
                nameField.getText(),
                phoneField.getText(),
                emailField.getText(),
                addressField.getText(),
                profileImageBase64
            ));
        }
    }

    public static class ProfileData {
        public final String name, phone, email, address, profileImageBase64;
        public ProfileData(String name, String phone, String email, String address, String profileImageBase64) {
            this.name = name; this.phone = phone; this.email = email; this.address = address; this.profileImageBase64 = profileImageBase64;
        }
    }

    private String emptyOr(String s) {
        return (s == null || s.isEmpty() || s.equals("null")) ? "" : s;
    }
} 