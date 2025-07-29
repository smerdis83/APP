package com.example.foodapp.controller;

import com.example.foodapp.dao.ExtraExpenseDao;
import com.example.foodapp.dao.RestaurantDao;
import com.example.foodapp.model.entity.ExtraExpense;
import com.example.foodapp.model.entity.Restaurant;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.List;

public class ExtraExpenseManagementController {
    @FXML private ComboBox<Restaurant> restaurantCombo;
    @FXML private TableView<ExtraExpense> expenseTable;
    @FXML private TableColumn<ExtraExpense, String> nameCol;
    @FXML private TableColumn<ExtraExpense, Integer> amountCol;
    @FXML private TableColumn<ExtraExpense, Void> actionsCol;
    @FXML private Button addBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private ObservableList<ExtraExpense> expenses = FXCollections.observableArrayList();
    private ObservableList<Restaurant> restaurants = FXCollections.observableArrayList();
    private Runnable onBack;
    private int sellerId;

    public void setOnBack(Runnable onBack) { this.onBack = onBack; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    @FXML
    public void initialize() {
        expenseTable.setItems(expenses);
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            {
                editBtn.setOnAction(e -> editExpense(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteExpense(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, editBtn, deleteBtn);
                    setGraphic(box);
                }
            }
        });
        restaurantCombo.setItems(restaurants);
        restaurantCombo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Restaurant item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });
        restaurantCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Restaurant item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });
        restaurantCombo.setOnAction(e -> loadExpenses());
        addBtn.setOnAction(e -> addExpense());
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void loadRestaurants(List<Restaurant> restaurantList) {
        restaurants.setAll(restaurantList);
        if (!restaurants.isEmpty()) {
            restaurantCombo.getSelectionModel().selectFirst();
            loadExpenses();
        }
    }

    private void loadExpenses() {
        Restaurant selected = restaurantCombo.getSelectionModel().getSelectedItem();
        if (selected == null) {
            expenses.clear();
            return;
        }
        try {
            ExtraExpenseDao dao = new ExtraExpenseDao();
            expenses.setAll(dao.getExtraExpensesByRestaurant(selected.getId()));
            messageLabel.setText("");
        } catch (SQLException e) {
            messageLabel.setText("Failed to load expenses: " + e.getMessage());
        }
    }

    private void addExpense() {
        Restaurant selected = restaurantCombo.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        TextInputDialog nameDialog = new TextInputDialog();
        nameDialog.setHeaderText("Enter expense name:");
        nameDialog.setTitle("Add Extra Expense");
        nameDialog.showAndWait().ifPresent(name -> {
            TextInputDialog amountDialog = new TextInputDialog();
            amountDialog.setHeaderText("Enter amount:");
            amountDialog.setTitle("Add Extra Expense");
            amountDialog.showAndWait().ifPresent(amountStr -> {
                try {
                    int amount = Integer.parseInt(amountStr);
                    ExtraExpense expense = new ExtraExpense(0, selected.getId(), name, amount);
                    ExtraExpenseDao dao = new ExtraExpenseDao();
                    dao.addExtraExpense(expense);
                    loadExpenses();
                } catch (Exception ex) {
                    messageLabel.setText("Invalid amount or error: " + ex.getMessage());
                }
            });
        });
    }

    private void editExpense(ExtraExpense expense) {
        TextInputDialog nameDialog = new TextInputDialog(expense.getName());
        nameDialog.setHeaderText("Edit expense name:");
        nameDialog.setTitle("Edit Extra Expense");
        nameDialog.showAndWait().ifPresent(name -> {
            TextInputDialog amountDialog = new TextInputDialog(String.valueOf(expense.getAmount()));
            amountDialog.setHeaderText("Edit amount:");
            amountDialog.setTitle("Edit Extra Expense");
            amountDialog.showAndWait().ifPresent(amountStr -> {
                try {
                    int amount = Integer.parseInt(amountStr);
                    expense.setName(name);
                    expense.setAmount(amount);
                    ExtraExpenseDao dao = new ExtraExpenseDao();
                    dao.updateExtraExpense(expense);
                    loadExpenses();
                } catch (Exception ex) {
                    messageLabel.setText("Invalid amount or error: " + ex.getMessage());
                }
            });
        });
    }

    private void deleteExpense(ExtraExpense expense) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this extra expense?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Delete Extra Expense");
        alert.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                try {
                    ExtraExpenseDao dao = new ExtraExpenseDao();
                    dao.deleteExtraExpense(expense.getId());
                    loadExpenses();
                } catch (Exception ex) {
                    messageLabel.setText("Delete failed: " + ex.getMessage());
                }
            }
        });
    }
}