package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class SellerDashboardController {
    @FXML public Label welcomeLabel;
    @FXML public Label roleLabel;
    @FXML public Button profileBtn;
    @FXML public Button logoutBtn;
    @FXML public Button addRestaurantBtn;
    @FXML public Button myRestaurantsBtn;
    @FXML public Button restaurantListBtn;
    @FXML public Button orderManagementBtn;
    @FXML public Button simpleOrderViewerBtn;
    @FXML public Button salesAnalyticsBtn;
    @FXML public Button extraExpensesBtn;
    @FXML public VBox roleContent;
    @FXML private VBox sideMenu;
    @FXML private Button burgerBtn;

    private Consumer<Void> onProfile;
    public void setOnProfile(Consumer<Void> callback) { this.onProfile = callback; }

    private Runnable onLogout;
    public void setOnLogout(Runnable callback) { this.onLogout = callback; }

    private Runnable onAddRestaurant;
    public void setOnAddRestaurant(Runnable callback) { this.onAddRestaurant = callback; }

    private Runnable onMyRestaurants;
    public void setOnMyRestaurants(Runnable callback) { this.onMyRestaurants = callback; }

    private Runnable onRestaurantList;
    public void setOnRestaurantList(Runnable callback) { this.onRestaurantList = callback; }

    private Runnable onOrderManagement;
    public void setOnOrderManagement(Runnable callback) { this.onOrderManagement = callback; }

    private Runnable onSimpleOrderViewer;
    public void setOnSimpleOrderViewer(Runnable callback) { this.onSimpleOrderViewer = callback; }

    private Runnable onSalesAnalytics;
    public void setOnSalesAnalytics(Runnable callback) { this.onSalesAnalytics = callback; }

    private Runnable onExtraExpenses;
    public void setOnExtraExpenses(Runnable callback) { this.onExtraExpenses = callback; }

    @FXML
    public void initialize() {
        if (burgerBtn != null && sideMenu != null) {
            burgerBtn.setOnAction(e -> sideMenu.setVisible(!sideMenu.isVisible()));
            sideMenu.setVisible(true);
        }
        if (profileBtn != null) profileBtn.setOnAction(e -> { if (onProfile != null) onProfile.accept(null); });
        if (logoutBtn != null) logoutBtn.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        if (addRestaurantBtn != null) addRestaurantBtn.setOnAction(e -> { if (onAddRestaurant != null) onAddRestaurant.run(); });
        if (myRestaurantsBtn != null) myRestaurantsBtn.setOnAction(e -> { if (onMyRestaurants != null) onMyRestaurants.run(); });
        if (restaurantListBtn != null) restaurantListBtn.setOnAction(e -> { if (onRestaurantList != null) onRestaurantList.run(); });
        if (orderManagementBtn != null) orderManagementBtn.setOnAction(e -> { if (onOrderManagement != null) onOrderManagement.run(); });
        if (simpleOrderViewerBtn != null) simpleOrderViewerBtn.setOnAction(e -> { if (onSimpleOrderViewer != null) onSimpleOrderViewer.run(); });
        if (salesAnalyticsBtn != null) salesAnalyticsBtn.setOnAction(e -> { if (onSalesAnalytics != null) onSalesAnalytics.run(); });
        if (extraExpensesBtn != null) extraExpensesBtn.setOnAction(e -> { if (onExtraExpenses != null) onExtraExpenses.run(); });
    }

    public void setWelcome(String name) { welcomeLabel.setText("Welcome, " + name + "!"); }
    public void setRole(String role) { roleLabel.setText("Role: " + role); }
    public void setRoleContent(javafx.scene.Node... nodes) { roleContent.getChildren().setAll(nodes); }
} 