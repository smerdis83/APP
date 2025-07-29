package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.Base64;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public class BuyerDashboardController {
    @FXML public Label roleLabel;
    @FXML public Button profileBtn;
    @FXML public Button logoutBtn;
    @FXML public Button orderHistoryBtn;
    @FXML public Button favoritesBtn;
    @FXML public Button restaurantListBtn;
    @FXML public Button searchBtn;
    @FXML public VBox roleContent;
    @FXML private Button topUpWalletBtn;
    @FXML public Button activeOrdersBtn;
    @FXML private VBox sideMenu;
    @FXML private Button burgerBtn;
    @FXML private HBox specialOffersBox;
    @FXML private HBox bestRestaurantsBox;
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

    private Runnable onSearch;
    public void setOnSearch(Runnable callback) { this.onSearch = callback; }

    private Runnable onActiveOrders;
    public void setOnActiveOrders(Runnable callback) { this.onActiveOrders = callback; }

    private com.example.foodapp.LoginApp app;
    public void setApp(com.example.foodapp.LoginApp app) { this.app = app; }

    @FXML
    public void initialize() {
        if (burgerBtn != null && sideMenu != null) {
            burgerBtn.setOnAction(e -> sideMenu.setVisible(!sideMenu.isVisible()));
            sideMenu.setVisible(true);
        }
        if (profileBtn != null) profileBtn.setOnAction(e -> { if (onProfile != null) onProfile.accept(null); });
        if (logoutBtn != null) logoutBtn.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        if (orderHistoryBtn != null) orderHistoryBtn.setOnAction(e -> { if (onOrderHistory != null) onOrderHistory.run(); });
        if (favoritesBtn != null) favoritesBtn.setOnAction(e -> { if (onFavorites != null) onFavorites.run(); });
        if (restaurantListBtn != null) restaurantListBtn.setOnAction(e -> { if (onRestaurantList != null) onRestaurantList.run(); });
        if (searchBtn != null) searchBtn.setOnAction(e -> { if (onSearch != null) onSearch.run(); });
        if (topUpWalletBtn != null) {
            topUpWalletBtn.setOnAction(e -> {
                Stage stage = (Stage) topUpWalletBtn.getScene().getWindow();
                app.showTopUpWalletPage(stage, jwtToken, () -> app.showDashboard(stage, "BUYER"));
            });
        }
        if (orderHistoryBtn != null) orderHistoryBtn.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) orderHistoryBtn.getScene().getWindow();
            app.showOrderHistoryScreen(stage);
        });
        if (activeOrdersBtn != null) activeOrdersBtn.setOnAction(e -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) activeOrdersBtn.getScene().getWindow();
            if (onActiveOrders != null) onActiveOrders.run();
            else if (app != null) app.showOrderHistoryScreen(stage, true); // true = active only
        });
        loadSpecialOffers();
        loadBestRestaurants();
    }

    private void loadSpecialOffers() {
        specialOffersBox.getChildren().clear();
        new Thread(() -> {
            try {
                com.example.foodapp.dao.FoodItemDao foodDao = new com.example.foodapp.dao.FoodItemDao();
                java.util.List<com.example.foodapp.model.entity.FoodItem> offers = foodDao.getDiscountedFoodItems();
                javafx.application.Platform.runLater(() -> {
                    Stage currentStage = (Stage) specialOffersBox.getScene().getWindow();
                    for (com.example.foodapp.model.entity.FoodItem food : offers) {
                        VBox offerBox = new VBox(8);
                        offerBox.setAlignment(Pos.CENTER);
                        ImageView imgView = new ImageView();
                        imgView.setFitWidth(160);
                        imgView.setFitHeight(160);
                        imgView.setPreserveRatio(true);
                        if (food.getImageBase64() != null && !food.getImageBase64().isEmpty()) {
                            try {
                                byte[] imgBytes = Base64.getDecoder().decode(food.getImageBase64());
                                imgView.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                            } catch (Exception e) { imgView.setImage(null); }
                        }
                        Tooltip tip = new Tooltip(food.getName() + "\nDiscount: " + food.getDiscountPrice() + " Toman");
                        Tooltip.install(imgView, tip);
                        imgView.setOnMouseClicked(e -> {
                            if (app != null) {
                                new Thread(() -> {
                                    try {
                                        com.example.foodapp.dao.RestaurantDao restDao = new com.example.foodapp.dao.RestaurantDao();
                                        com.example.foodapp.model.entity.Restaurant rest = restDao.findById(food.getVendorId());
                                        if (rest != null) {
                                            javafx.application.Platform.runLater(() -> {
                                                app.showRestaurantPage(
                                                    currentStage,
                                                    rest.getId(),
                                                    rest.getName(),
                                                    rest.getLogoBase64(),
                                                    () -> app.showDashboard(currentStage, "BUYER")
                                                );
                                            });
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }).start();
                            }
                        });
                        Label oldPrice = new Label(food.getPrice() + " Toman");
                        oldPrice.setStyle("-fx-text-fill: #888; -fx-strikethrough: true; -fx-font-size: 14px;");
                        Label newPrice = new Label(food.getDiscountPrice() + " Toman");
                        newPrice.setStyle("-fx-text-fill: #C2185B; -fx-font-weight: bold; -fx-font-size: 18px;");
                        offerBox.getChildren().addAll(imgView, oldPrice, newPrice);
                        specialOffersBox.getChildren().add(offerBox);
                    }
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void loadBestRestaurants() {
        bestRestaurantsBox.getChildren().clear();
        new Thread(() -> {
            try {
                com.example.foodapp.dao.RestaurantDao restDao = new com.example.foodapp.dao.RestaurantDao();
                java.util.List<com.example.foodapp.model.entity.Restaurant> all = restDao.findAll();
                java.util.List<RestaurantWithRating> rated = new java.util.ArrayList<>();
                for (com.example.foodapp.model.entity.Restaurant r : all) {
                    double avg = fetchRestaurantAvgRating(r.getId());
                    rated.add(new RestaurantWithRating(r, avg));
                }
                rated.sort((a, b) -> Double.compare(b.avgRating, a.avgRating));
                javafx.application.Platform.runLater(() -> {
                    Stage currentStage = (Stage) bestRestaurantsBox.getScene().getWindow();
                    for (RestaurantWithRating rr : rated) {
                        VBox card = new VBox(6);
                        card.setAlignment(Pos.CENTER);
                        ImageView logo = new ImageView();
                        logo.setFitWidth(90);
                        logo.setFitHeight(90);
                        logo.setPreserveRatio(true);
                        if (rr.restaurant.getLogoBase64() != null && !rr.restaurant.getLogoBase64().isEmpty()) {
                            try {
                                byte[] imgBytes = Base64.getDecoder().decode(rr.restaurant.getLogoBase64());
                                logo.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                            } catch (Exception e) { logo.setImage(null); }
                        }
                        Label name = new Label(rr.restaurant.getName());
                        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
                        Label rating = new Label(String.format("★ %.2f", rr.avgRating));
                        rating.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 16px; -fx-font-weight: bold;");
                        Tooltip.install(logo, new Tooltip(rr.restaurant.getName() + "\nRating: " + String.format("%.2f", rr.avgRating)));
                        logo.setOnMouseClicked(e -> {
                            if (app != null) {
                                app.showRestaurantPage(currentStage, rr.restaurant.getId(), rr.restaurant.getName(), rr.restaurant.getLogoBase64(), () -> app.showDashboard(currentStage, "BUYER"));
                            }
                        });
                        card.getChildren().addAll(logo, name, rating);
                        bestRestaurantsBox.getChildren().add(card);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private double fetchRestaurantAvgRating(int restaurantId) {
        try {
            java.net.URL url = new java.net.URL("http://localhost:8000/ratings/restaurant/" + restaurantId);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
            int code = conn.getResponseCode();
            if (code == 200) {
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                int avgIdx = json.indexOf("\"avg_rating\":");
                if (avgIdx != -1) {
                    int start = avgIdx + 13;
                    int end = json.indexOf(',', start);
                    if (end == -1) end = json.indexOf('}', start);
                    return Double.parseDouble(json.substring(start, end).replaceAll("[^0-9\\.]", ""));
                }
            }
        } catch (Exception ignore) {}
        return 0.0;
    }

    private static class RestaurantWithRating {
        final com.example.foodapp.model.entity.Restaurant restaurant;
        final double avgRating;
        RestaurantWithRating(com.example.foodapp.model.entity.Restaurant r, double avg) {
            this.restaurant = r;
            this.avgRating = avg;
        }
    }

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