package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class BuyerDashboardController {
    @FXML public Label welcomeLabel;
    @FXML public Label roleLabel;
    @FXML public Button profileBtn;
    @FXML public Button logoutBtn;
    @FXML public Button orderHistoryBtn;
    @FXML public Button favoritesBtn;
    @FXML public Button restaurantListBtn;
    @FXML public VBox roleContent;
    @FXML private Button topUpWalletBtn;
    private String jwtToken;
    public void setJwtToken(String token) { this.jwtToken = token; }

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

    private com.example.foodapp.LoginApp app;
    public void setApp(com.example.foodapp.LoginApp app) { this.app = app; }

    @FXML
    public void initialize() {
        if (profileBtn != null) profileBtn.setOnAction(e -> { if (onProfile != null) onProfile.accept(null); });
        if (logoutBtn != null) logoutBtn.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        if (orderHistoryBtn != null) orderHistoryBtn.setOnAction(e -> { if (onOrderHistory != null) onOrderHistory.run(); });
        if (favoritesBtn != null) favoritesBtn.setOnAction(e -> { if (onFavorites != null) onFavorites.run(); });
        if (restaurantListBtn != null) restaurantListBtn.setOnAction(e -> { if (onRestaurantList != null) onRestaurantList.run(); });
        if (topUpWalletBtn != null) {
            topUpWalletBtn.setOnAction(e -> {
                Stage stage = (Stage) topUpWalletBtn.getScene().getWindow();
                app.showTopUpWalletPage(stage, jwtToken, () -> app.showDashboard(stage, "BUYER"));
            });
        }
    }

    public void setWelcome(String name) { welcomeLabel.setText("Welcome, " + name + "!"); }
    public void setRole(String role) { roleLabel.setText("Role: " + role); }
    public void setRoleContent(javafx.scene.Node... nodes) { roleContent.getChildren().setAll(nodes); }

    public void showTopUpWalletPage(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/TopUpWallet.fxml"));
            Parent root = loader.load();
            TopUpWalletController controller = loader.getController();
            controller.setJwtToken(jwtToken);
            controller.setOnBack(() -> {
                stage.setScene(topUpWalletBtn.getScene());
            });
            stage.setScene(new Scene(root));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
} 