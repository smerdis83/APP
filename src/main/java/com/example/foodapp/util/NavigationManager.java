package com.example.foodapp.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationManager {
    
    private static NavigationManager instance;
    private Stage primaryStage;
    
    private NavigationManager() {
        // this.primaryStage = FoodAppFX.getPrimaryStage();
    }
    
    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }
    
    public void navigateToBuyerDashboard() {
        try {
            System.out.println("[NavigationManager] Loading BuyerDashboard.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BuyerDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("FoodApp - Buyer Dashboard");
            System.out.println("[NavigationManager] BuyerDashboard scene set successfully");
        } catch (IOException e) {
            System.err.println("[NavigationManager] Failed to load BuyerDashboard.fxml: " + e.getMessage());
            e.printStackTrace();
            // Fallback to login screen
            navigateToLogin();
        }
    }
    
    public void navigateToSellerDashboard() {
        try {
            System.out.println("[NavigationManager] Loading SellerDashboard.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SellerDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("FoodApp - Seller Dashboard");
        } catch (IOException e) {
            System.err.println("[NavigationManager] Failed to load SellerDashboard.fxml: " + e.getMessage());
            e.printStackTrace();
            // Fallback to login screen
            navigateToLogin();
        }
    }
    
    public void navigateToCourierDashboard() {
        try {
            System.out.println("[NavigationManager] Loading CourierDashboard.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CourierDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("FoodApp - Courier Dashboard");
        } catch (IOException e) {
            System.err.println("[NavigationManager] Failed to load CourierDashboard.fxml: " + e.getMessage());
            e.printStackTrace();
            // Fallback to login screen
            navigateToLogin();
        }
    }
    
    public void navigateToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegisterScreen.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("FoodApp - Register");
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback to login screen
            navigateToLogin();
        }
    }
    
    public void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginScreen.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("FoodApp - Login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void logout() {
        // Clear session
        SessionManager.getInstance().clearSession();
        // Navigate back to login
        navigateToLogin();
    }
} 