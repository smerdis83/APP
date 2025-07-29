package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;
import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

public class FavoritesController {
    @FXML private ListView<RestaurantItem> favoriteList;
    @FXML private ListView<RestaurantItem> otherList;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private Runnable onBack;
    private String jwtToken;
    private Set<Integer> favoriteIds = new HashSet<>();
    private Consumer<RestaurantItem> onRestaurantClick;
    private ObservableList<RestaurantItem> favoriteRestaurants = FXCollections.observableArrayList();
    private ObservableList<RestaurantItem> otherRestaurants = FXCollections.observableArrayList();
    private Runnable onBackFromRestaurant;

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }
    public void setOnRestaurantClick(Consumer<RestaurantItem> callback) { this.onRestaurantClick = callback; }
    public void setOnBackFromRestaurant(Runnable r) { this.onBackFromRestaurant = r; }

    @FXML
    public void initialize() {
        loadRestaurantsAndFavorites();
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        // Set up single-click to open restaurant page
        favoriteList.setCellFactory(list -> new RestaurantCell(favoriteIds, this::toggleFavorite, item -> {
            if (onRestaurantClick != null) onRestaurantClick.accept(item);
        }));
        favoriteList.setOnMouseClicked(ev -> {
            RestaurantItem selected = favoriteList.getSelectionModel().getSelectedItem();
            if (selected != null && ev.getClickCount() == 1 && onRestaurantClick != null) {
                onRestaurantClick.accept(selected);
            }
        });
        otherList.setCellFactory(list -> new RestaurantCell(favoriteIds, this::toggleFavorite, onRestaurantClick));
    }

    private void loadRestaurantsAndFavorites() {
        messageLabel.setText("");
        new Thread(() -> {
            try {
                // Fetch all restaurants
                URL url = new URL("http://localhost:8000/restaurants");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch restaurants: " + code);
                Scanner sc = new Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<RestaurantItem> restaurants = parseRestaurants(json);
                // Fetch favorites
                url = new URL("http://localhost:8000/favorites");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch favorites: " + code);
                sc = new Scanner(conn.getInputStream(), "UTF-8");
                String favJson = sc.useDelimiter("\\A").next();
                sc.close();
                favoriteIds = parseFavoriteIds(favJson);
                Platform.runLater(() -> {
                    favoriteRestaurants.clear();
                    otherRestaurants.clear();
                    for (RestaurantItem r : restaurants) {
                        if (favoriteIds.contains(r.id)) favoriteRestaurants.add(r);
                        else otherRestaurants.add(r);
                    }
                    favoriteList.setItems(favoriteRestaurants);
                    otherList.setItems(otherRestaurants);
                    favoriteList.setCellFactory(list -> new RestaurantCell(favoriteIds, this::toggleFavorite));
                    otherList.setCellFactory(list -> new RestaurantCell(favoriteIds, this::toggleFavorite));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    private List<RestaurantItem> parseRestaurants(String json) {
        List<RestaurantItem> list = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"id\":", idx)) != -1) {
            int id = Integer.parseInt(json.substring(idx + 5, json.indexOf(',', idx + 5)).replaceAll("[^0-9]", ""));
            int nameIdx = json.indexOf("\"name\":", idx);
            int nameStart = json.indexOf('"', nameIdx + 7) + 1;
            int nameEnd = json.indexOf('"', nameStart);
            String name = json.substring(nameStart, nameEnd);
            int logoIdx = json.indexOf("\"logo_base64\":", idx);
            String logo = null;
            if (logoIdx != -1) {
                int logoStart = json.indexOf('"', logoIdx + 14) + 1;
                int logoEnd = json.indexOf('"', logoStart);
                logo = json.substring(logoStart, logoEnd);
            }
            list.add(new RestaurantItem(id, name, logo));
            idx = nameEnd;
        }
        return list;
    }

    private Set<Integer> parseFavoriteIds(String json) {
        Set<Integer> ids = new HashSet<>();
        int idx = 0;
        while ((idx = json.indexOf("\"id\":", idx)) != -1) {
            int id = Integer.parseInt(json.substring(idx + 5, json.indexOf(',', idx + 5)).replaceAll("[^0-9]", ""));
            ids.add(id);
            idx += 5;
        }
        return ids;
    }

    private void toggleFavorite(RestaurantItem item) {
        boolean isFav = favoriteIds.contains(item.id);
        new Thread(() -> {
            try {
                URL url = new URL("http://localhost:8000/favorites/" + item.id);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(isFav ? "DELETE" : "PUT");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                if (!isFav) { conn.setDoOutput(true); conn.getOutputStream().close(); }
                int code = conn.getResponseCode();
                if (code == 200) {
                    Platform.runLater(() -> {
                        if (isFav) {
                            favoriteIds.remove(item.id);
                            favoriteRestaurants.remove(item);
                            otherRestaurants.add(item);
                            messageLabel.setText("Removed from favorites.");
                        } else {
                            favoriteIds.add(item.id);
                            otherRestaurants.remove(item);
                            favoriteRestaurants.add(item);
                            messageLabel.setText("Added to favorites.");
                        }
                        favoriteList.refresh();
                        otherList.refresh();
                    });
                } else {
                    Platform.runLater(() -> messageLabel.setText("Failed: " + code));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> messageLabel.setText("Error: " + ex.getMessage()));
            }
        }).start();
    }

    // Data class for restaurant
    public static class RestaurantItem {
        public final int id;
        public final String name;
        public final String logoBase64;
        public RestaurantItem(int id, String name, String logoBase64) {
            this.id = id; this.name = name; this.logoBase64 = logoBase64;
        }
    }

    // Custom cell for restaurant list
    public static class RestaurantCell extends ListCell<RestaurantItem> {
        private final HBox content = new HBox(10);
        private final ImageView imageView = new ImageView();
        private final Label nameLabel = new Label();
        private final Button favBtn = new Button();
        private final Set<Integer> favoriteIds;
        private final Consumer<RestaurantItem> onToggle;
        private Consumer<RestaurantItem> onRestaurantClick;
        public RestaurantCell(Set<Integer> favoriteIds, Consumer<RestaurantItem> onToggle) {
            this(favoriteIds, onToggle, null);
        }
        public RestaurantCell(Set<Integer> favoriteIds, Consumer<RestaurantItem> onToggle, Consumer<RestaurantItem> onRestaurantClick) {
            this.favoriteIds = favoriteIds;
            this.onToggle = onToggle;
            this.onRestaurantClick = onRestaurantClick;
            imageView.setFitHeight(50); imageView.setFitWidth(50);
            content.getChildren().addAll(imageView, nameLabel, favBtn);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
        }
        @Override
        protected void updateItem(RestaurantItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setOnMouseClicked(null);
            } else {
                nameLabel.setText(item.name);
                if (item.logoBase64 != null && !item.logoBase64.isEmpty()) {
                    try {
                        byte[] imgBytes = Base64.getDecoder().decode(item.logoBase64);
                        imageView.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                    } catch (Exception e) { imageView.setImage(null); }
                } else {
                    imageView.setImage(null);
                }
                favBtn.setText(favoriteIds.contains(item.id) ? "Remove from Favorites" : "Add to Favorites");
                favBtn.setOnAction(e -> onToggle.accept(item));
                setGraphic(content);
                if (onRestaurantClick != null) {
                    setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 2) onRestaurantClick.accept(item);
                    });
                }
            }
        }
    }
} 