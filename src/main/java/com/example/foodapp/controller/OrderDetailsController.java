package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.example.foodapp.dao.ExtraExpenseDao;
import com.example.foodapp.model.entity.ExtraExpense;
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

    // New method signature: pass restaurantId as well
    public void setOrderDetails(String id, String status, String total, String created, String updated, List<String> items, int restaurantId) {
        idLabel.setText(emptyOr(id));
        statusLabel.setText(emptyOr(status));
        createdLabel.setText(emptyOr(created));
        updatedLabel.setText(emptyOr(updated));
        // Add items
        itemsList.getItems().setAll(items);
        // Fetch and display extra expenses
        try {
            ExtraExpenseDao dao = new ExtraExpenseDao();
            List<ExtraExpense> extras = dao.getExtraExpensesByRestaurant(restaurantId);
            int extraTotal = 0;
            for (ExtraExpense e : extras) {
                itemsList.getItems().add("+ [Extra] " + e.getName() + ": " + e.getAmount());
                extraTotal += e.getAmount();
            }
            if (!extras.isEmpty()) {
                itemsList.getItems().add("(All extra expenses above are added to the total.)");
            }
            // Update total
            int totalInt = Integer.parseInt(total.replaceAll("[^0-9]", ""));
            int newTotal = totalInt + extraTotal;
            totalLabel.setText(String.valueOf(newTotal));
        } catch (Exception e) {
            itemsList.getItems().add("[Extra] (Failed to load extra expenses: " + e.getMessage() + ")");
            totalLabel.setText(emptyOr(total));
        }
    }

    private String emptyOr(String s) {
        return (s == null || s.isEmpty() || s.equals("null")) ? "Empty" : s;
    }
} 