package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import java.util.function.Consumer;
import com.example.foodapp.model.OrderSummary;

public class OrderHistoryController {
    @FXML public ListView<OrderSummary> orderList;
    @FXML public Label messageLabel;
    @FXML public Button backBtn;

    private Consumer<OrderSummary> onOrderSelected;
    private Runnable onBack;

    public void setOnOrderSelected(Consumer<OrderSummary> callback) { this.onOrderSelected = callback; }
    public void setOnBack(Runnable callback) { this.onBack = callback; }

    @FXML
    public void initialize() {
        orderList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && onOrderSelected != null) onOrderSelected.accept(newVal);
        });
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void setOrders(java.util.List<OrderSummary> orders) {
        ObservableList<OrderSummary> items = FXCollections.observableArrayList(orders);
        orderList.setItems(items);
    }
    public void showMessage(String msg) {
        messageLabel.setText(msg);
    }
    public void clearMessage() {
        messageLabel.setText("");
    }
} 