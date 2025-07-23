package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

public class DashboardController {
    @FXML public Label welcomeLabel;
    @FXML public Label roleLabel;
    @FXML public Button profileBtn;
    @FXML public Button logoutBtn;
    @FXML public Button orderHistoryBtn;
    @FXML public Button favoritesBtn;
    @FXML public Button restaurantListBtn;
    @FXML public Button addRestaurantBtn;
    @FXML public Button myRestaurantsBtn;
    @FXML public VBox roleContent;

    private Consumer<Void> onProfile;
    public void setOnProfile(Consumer<Void> callback) { this.onProfile = callback; }

    private Runnable onLogout;
    public void setOnLogout(Runnable callback) { this.onLogout = callback; }

    private Runnable onOrderHistory;
    public void setOnOrderHistory(Runnable callback) { this.onOrderHistory = callback; }

    private Runnable onFavorites;
    public void setOnFavorites(Runnable callback) { this.onFavorites = callback; }

    private Runnable onRestaurantList;
    public void setOnRestaurantList(Runnable callback) { this.onRestaurantList = callback; }

    private Runnable onAddRestaurant;
    private Runnable onMyRestaurants;
    public void setOnAddRestaurant(Runnable callback) { this.onAddRestaurant = callback; }
    public void setOnMyRestaurants(Runnable callback) { this.onMyRestaurants = callback; }

    @FXML
    public void initialize() {
        if (profileBtn != null) profileBtn.setOnAction(e -> { if (onProfile != null) onProfile.accept(null); });
        if (logoutBtn != null) logoutBtn.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        if (addRestaurantBtn != null) addRestaurantBtn.setOnAction(e -> { if (onAddRestaurant != null) onAddRestaurant.run(); });
        if (myRestaurantsBtn != null) myRestaurantsBtn.setOnAction(e -> { if (onMyRestaurants != null) onMyRestaurants.run(); });
        if (restaurantListBtn != null) restaurantListBtn.setOnAction(e -> { if (onRestaurantList != null) onRestaurantList.run(); });
        if (orderHistoryBtn != null) orderHistoryBtn.setOnAction(e -> { if (onOrderHistory != null) onOrderHistory.run(); });
        if (favoritesBtn != null) favoritesBtn.setOnAction(e -> { if (onFavorites != null) onFavorites.run(); });
    }

    public void setWelcome(String name) {
        welcomeLabel.setText("Welcome, " + name + "!");
    }
    public void setRole(String role) {
        roleLabel.setText("Role: " + role);
    }
    public void setRoleContent(javafx.scene.Node... nodes) {
        roleContent.getChildren().setAll(nodes);
    }
} 