package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class OrderDetailsController {
    @FXML public Label idLabel;
    @FXML public Label statusLabel;
    @FXML public Label totalLabel;
    @FXML public Label createdLabel;
    @FXML public Label updatedLabel;
    @FXML public ListView<String> itemsList;
    @FXML public Button backBtn;

    private Runnable onBack;
    public void setOnBack(Runnable callback) { this.onBack = callback; }

    @FXML
    public void initialize() {
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void setOrderDetails(String id, String status, String total, String created, String updated, List<String> items) {
        idLabel.setText(emptyOr(id));
        statusLabel.setText(emptyOr(status));
        totalLabel.setText(emptyOr(total));
        createdLabel.setText(emptyOr(created));
        updatedLabel.setText(emptyOr(updated));
        itemsList.getItems().setAll(items);
    }

    private String emptyOr(String s) {
        return (s == null || s.isEmpty() || s.equals("null")) ? "Empty" : s;
    }
} 