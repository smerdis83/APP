package com.example.foodapp.controller;

import com.example.foodapp.handler.CouponHandler;
import com.example.foodapp.model.entity.Coupon;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.SQLException;
import java.util.List;

public class CouponController {
    @FXML private TableView<CouponTableItem> couponsTable;
    @FXML private TextField couponCodeField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField valueField;
    @FXML private TextField minPriceField;
    @FXML private TextField userCountField;
    @FXML private TextField maxUsesPerUserField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button createCouponBtn;
    @FXML private Button updateCouponBtn;
    @FXML private Button deleteCouponBtn;
    @FXML private Button clearFormBtn;
    @FXML private Label statusLabel;

    private CouponHandler couponHandler;
    private CouponTableItem selectedCoupon;

    public CouponController() {
        this.couponHandler = new CouponHandler();
    }

    @FXML
    public void initialize() {
        setupTable();
        setupForm();
        loadCoupons();
    }

    private void setupTable() {
        // Setup table columns
        TableColumn<CouponTableItem, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCouponCode()));

        TableColumn<CouponTableItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));

        TableColumn<CouponTableItem, String> valueCol = new TableColumn<>("Value");
        valueCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getValue()));

        TableColumn<CouponTableItem, String> minPriceCol = new TableColumn<>("Min Price");
        minPriceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMinPrice()));

        TableColumn<CouponTableItem, String> userCountCol = new TableColumn<>("User Count");
        userCountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUserCount()));

        TableColumn<CouponTableItem, String> maxUsesPerUserCol = new TableColumn<>("Max Uses/User");
        maxUsesPerUserCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMaxUsesPerUser()));

        TableColumn<CouponTableItem, String> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStartDate()));

        TableColumn<CouponTableItem, String> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEndDate()));

        couponsTable.getColumns().addAll(codeCol, typeCol, valueCol, minPriceCol, userCountCol, maxUsesPerUserCol, startDateCol, endDateCol);

        // Handle row selection
        couponsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedCoupon = newSelection;
                populateForm(newSelection);
            }
        });
    }

    private void setupForm() {
        typeComboBox.getItems().addAll("fixed", "percent");
        typeComboBox.setValue("fixed");

        // Set default dates
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(30));
        
        // Set default max uses per user
        maxUsesPerUserField.setText("1");
    }

    @FXML
    private void handleCreateCoupon() {
        try {
            Coupon coupon = createCouponFromForm();
            if (coupon != null) {
                couponHandler.createCoupon(coupon);
                showStatus("Coupon created successfully!", true);
                clearForm();
                loadCoupons();
            }
        } catch (SQLException e) {
            showStatus("Error creating coupon: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleUpdateCoupon() {
        if (selectedCoupon == null) {
            showStatus("Please select a coupon to update", false);
            return;
        }

        try {
            Coupon coupon = createCouponFromForm();
            if (coupon != null) {
                coupon.setId(selectedCoupon.getId());
                couponHandler.updateCoupon(coupon);
                showStatus("Coupon updated successfully!", true);
                clearForm();
                loadCoupons();
            }
        } catch (SQLException e) {
            showStatus("Error updating coupon: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleDeleteCoupon() {
        if (selectedCoupon == null) {
            showStatus("Please select a coupon to delete", false);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Coupon");
        alert.setContentText("Are you sure you want to delete coupon '" + selectedCoupon.getCouponCode() + "'?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    couponHandler.deleteCoupon(selectedCoupon.getId());
                    showStatus("Coupon deleted successfully!", true);
                    clearForm();
                    loadCoupons();
                } catch (SQLException e) {
                    showStatus("Error deleting coupon: " + e.getMessage(), false);
                }
            }
        });
    }

    @FXML
    private void handleClearForm() {
        clearForm();
    }

    private Coupon createCouponFromForm() {
        String code = couponCodeField.getText().trim();
        String type = typeComboBox.getValue();
        String valueStr = valueField.getText().trim();
        String minPriceStr = minPriceField.getText().trim();
        String userCountStr = userCountField.getText().trim();
        String maxUsesPerUserStr = maxUsesPerUserField.getText().trim();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();

        if (code.isEmpty() || valueStr.isEmpty() || minPriceStr.isEmpty() || userCountStr.isEmpty() || maxUsesPerUserStr.isEmpty()) {
            showStatus("Please fill all required fields", false);
            return null;
        }

        try {
            double value = Double.parseDouble(valueStr);
            int minPrice = Integer.parseInt(minPriceStr);
            int userCount = Integer.parseInt(userCountStr);
            int maxUsesPerUser = Integer.parseInt(maxUsesPerUserStr);

            if (value <= 0 || minPrice < 0 || userCount < 0 || maxUsesPerUser < 1) {
                showStatus("Please enter valid positive numbers (max uses per user must be at least 1)", false);
                return null;
            }

            if (startDate == null || endDate == null) {
                showStatus("Please select start and end dates", false);
                return null;
            }

            if (startDate.isAfter(endDate)) {
                showStatus("Start date cannot be after end date", false);
                return null;
            }

            return new Coupon(0, code, type, value, minPrice, userCount, maxUsesPerUser, startDate, endDate);
        } catch (NumberFormatException e) {
            showStatus("Please enter valid numbers for value, min price, user count, and max uses per user", false);
            return null;
        }
    }

    private void populateForm(CouponTableItem coupon) {
        couponCodeField.setText(coupon.getCouponCode());
        typeComboBox.setValue(coupon.getType());
        valueField.setText(coupon.getValue());
        minPriceField.setText(coupon.getMinPrice());
        userCountField.setText(coupon.getUserCount());
        maxUsesPerUserField.setText(coupon.getMaxUsesPerUser());
        startDatePicker.setValue(LocalDate.parse(coupon.getStartDate()));
        endDatePicker.setValue(LocalDate.parse(coupon.getEndDate()));
    }

    private void clearForm() {
        couponCodeField.clear();
        typeComboBox.setValue("fixed");
        valueField.clear();
        minPriceField.clear();
        userCountField.clear();
        maxUsesPerUserField.setText("1");
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(30));
        selectedCoupon = null;
        couponsTable.getSelectionModel().clearSelection();
    }

    private void loadCoupons() {
        try {
            List<Coupon> coupons = couponHandler.getAllCoupons();
            ObservableList<CouponTableItem> tableData = FXCollections.observableArrayList();
            
            for (Coupon coupon : coupons) {
                tableData.add(new CouponTableItem(
                    coupon.getId(),
                    coupon.getCouponCode(),
                    coupon.getType(),
                    String.valueOf(coupon.getValue()),
                    String.valueOf(coupon.getMinPrice()),
                    String.valueOf(coupon.getUserCount()),
                    String.valueOf(coupon.getMaxUsesPerUser()),
                    coupon.getStartDate().toString(),
                    coupon.getEndDate().toString()
                ));
            }
            
            couponsTable.setItems(tableData);
        } catch (SQLException e) {
            showStatus("Error loading coupons: " + e.getMessage(), false);
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        statusLabel.setText(message);
        statusLabel.setStyle(isSuccess ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }

    // Table item class for displaying coupons
    public static class CouponTableItem {
        private final int id;
        private final String couponCode;
        private final String type;
        private final String value;
        private final String minPrice;
        private final String userCount;
        private final String maxUsesPerUser;
        private final String startDate;
        private final String endDate;

        public CouponTableItem(int id, String couponCode, String type, String value, 
                             String minPrice, String userCount, String maxUsesPerUser, String startDate, String endDate) {
            this.id = id;
            this.couponCode = couponCode;
            this.type = type;
            this.value = value;
            this.minPrice = minPrice;
            this.userCount = userCount;
            this.maxUsesPerUser = maxUsesPerUser;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public int getId() { return id; }
        public String getCouponCode() { return couponCode; }
        public String getType() { return type; }
        public String getValue() { return value; }
        public String getMinPrice() { return minPrice; }
        public String getUserCount() { return userCount; }
        public String getMaxUsesPerUser() { return maxUsesPerUser; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
    }
} 