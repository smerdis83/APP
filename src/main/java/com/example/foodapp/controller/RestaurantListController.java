package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import java.util.*;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.util.function.Consumer;
import javafx.scene.layout.HBox;

public class RestaurantListController {
    @FXML private ListView<RestaurantItem> restaurantList;
    @FXML private Button backBtn;

    private String jwtToken;
    private Runnable onBack;
    private Consumer<RestaurantItem> onRestaurantClick;
    private ObservableList<RestaurantItem> restaurants = FXCollections.observableArrayList();

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setOnBack(Runnable r) { this.onBack = r; }
    public void setOnRestaurantClick(Consumer<RestaurantItem> c) { this.onRestaurantClick = c; }

    @FXML
    public void initialize() {
        restaurantList.setItems(restaurants);
        restaurantList.setCellFactory(list -> new RestaurantCell());
        restaurantList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                RestaurantItem item = restaurantList.getSelectionModel().getSelectedItem();
                if (item != null && onRestaurantClick != null) onRestaurantClick.accept(item);
            }
        });
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
        fetchRestaurants();
    }

    private void fetchRestaurants() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch restaurants: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<RestaurantItem> items = parseRestaurants(json);
                Platform.runLater(() -> restaurants.setAll(items));
            } catch (Exception ex) {
                Platform.runLater(() -> restaurants.clear());
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
            int descIdx = json.indexOf("\"description\":", idx);
            String description = "";
            if (descIdx != -1) {
                int descStart = json.indexOf('"', descIdx + 14) + 1;
                int descEnd = json.indexOf('"', descStart);
                description = json.substring(descStart, descEnd);
                // Patch: filter out base64 or nonsense
                if (description == null || description.trim().isEmpty() || description.length() > 200 || description.contains("base64")) {
                    description = "";
                }
            }
            int whIdx = json.indexOf("\"working_hours\":", idx);
            String workingHours = "";
            if (whIdx != -1) {
                int whStart = json.indexOf('"', whIdx + 16) + 1;
                int whEnd = json.indexOf('"', whStart);
                workingHours = json.substring(whStart, whEnd);
                // Patch: filter out base64 or nonsense
                if (workingHours == null || workingHours.trim().isEmpty() || workingHours.length() > 100 || workingHours.contains("base64")) {
                    workingHours = "";
                }
            }
            RestaurantItem item = new RestaurantItem(id, name, logo, description, workingHours);
            list.add(item);
            fetchAndSetRatingAndComments(item);
            idx = nameEnd;
        }
        return list;
    }

    private void fetchAndSetRatingAndComments(RestaurantItem item) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/ratings/restaurant/" + item.id);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code == 200) {
                    java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                    String json = sc.useDelimiter("\\A").next();
                    sc.close();
                    double avg = 0;
                    List<String> comments = new ArrayList<>();
                    int avgIdx = json.indexOf("\"avg_rating\":");
                    if (avgIdx != -1) {
                        int start = avgIdx + 13;
                        int end = json.indexOf(',', start);
                        if (end == -1) end = json.indexOf('}', start);
                        avg = Double.parseDouble(json.substring(start, end).replaceAll("[^0-9\\.]", ""));
                    }
                    int commIdx = json.indexOf("\"comments\":[");
                    if (commIdx != -1) {
                        int arrStart = json.indexOf('[', commIdx);
                        int arrEnd = json.indexOf(']', arrStart);
                        if (arrStart != -1 && arrEnd != -1) {
                            String arr = json.substring(arrStart + 1, arrEnd);
                            int cIdx = 0;
                            while ((cIdx = arr.indexOf("\"comment\":", cIdx)) != -1) {
                                int cStart = arr.indexOf('"', cIdx + 9) + 1;
                                int cEnd = arr.indexOf('"', cStart);
                                String comment = arr.substring(cStart, cEnd);
                                comments.add(comment);
                                cIdx = cEnd;
                            }
                        }
                    }
                    double finalAvg = avg;
                    List<String> finalComments = comments;
                    Platform.runLater(() -> {
                        item.avgRating = finalAvg;
                        item.comments = finalComments;
                        restaurantList.refresh();
                    });
                }
            } catch (Exception ignore) {}
        }).start();
    }

    public static class RestaurantItem {
        public final int id;
        public final String name;
        public final String logoBase64;
        public final String description;
        public final String workingHours;
        public double avgRating = -1;
        public List<String> comments = new ArrayList<>();
        public RestaurantItem(int id, String name, String logoBase64, String description, String workingHours) {
            this.id = id; this.name = name; this.logoBase64 = logoBase64;
            this.description = description; this.workingHours = workingHours;
        }
    }

    public class RestaurantCell extends ListCell<RestaurantItem> {
        private final HBox content = new HBox(16);
        private final ImageView imageView = new ImageView();
        private final Label nameLabel = new Label();
        private final Label ratingLabel = new Label();
        private final Button commentsBtn = new Button("Comments");
        private final Label descLabel = new Label();
        private final Label whLabel = new Label();
        private RestaurantItem lastItem = null;
        public RestaurantCell() {
            imageView.setFitHeight(80);
            imageView.setFitWidth(80);
            imageView.setPreserveRatio(true);
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            ratingLabel.setMaxWidth(Double.MAX_VALUE);
            commentsBtn.setMaxWidth(Double.MAX_VALUE);
            content.setMaxWidth(Double.MAX_VALUE);
            content.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 8 0 8 0;");
            content.getChildren().addAll(imageView, nameLabel, descLabel, whLabel, ratingLabel, commentsBtn);
            HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(ratingLabel, javafx.scene.layout.Priority.NEVER);
            HBox.setHgrow(commentsBtn, javafx.scene.layout.Priority.NEVER);
            commentsBtn.setOnAction(e -> {
                if (lastItem != null) {
                    System.out.println("[DEBUG] Comments button clicked for restaurant: " + lastItem.name);
                    showCommentsPage(lastItem);
                }
            });
        }
        private void showCommentsPage(RestaurantItem item) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/RestaurantComments.fxml"));
                javafx.scene.Parent root = loader.load();
                com.example.foodapp.controller.RestaurantCommentsController controller = loader.getController();
                controller.setRestaurant(item.id, item.name, jwtToken);
                controller.setOnBack(() -> {
                    // Use the current stage from the backBtn in the comments page
                    javafx.stage.Stage stage = (javafx.stage.Stage) root.getScene().getWindow();
                    // Restore the restaurant list scene
                    stage.setScene(restaurantList.getScene());
                    stage.setTitle("Restaurant List");
                });
                // Use the current stage from the commentsBtn
                javafx.stage.Stage stage = (javafx.stage.Stage) commentsBtn.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root, 700, 600));
                stage.setTitle("Comments for " + item.name);
                stage.centerOnScreen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        @Override
        protected void updateItem(RestaurantItem item, boolean empty) {
            super.updateItem(item, empty);
            lastItem = item;
            if (empty || item == null) {
                setGraphic(null);
            } else {
                nameLabel.setText(item.name);
                descLabel.setText(item.description != null && !item.description.trim().isEmpty() ? item.description : "");
                String wh = item.workingHours != null ? item.workingHours.trim() : "";
                whLabel.setText(!wh.isEmpty() ? "Hours: " + wh : "");
                if (item.logoBase64 != null && item.logoBase64.length() > 20 && !item.logoBase64.startsWith("[")) {
                    try {
                        byte[] imgBytes = Base64.getDecoder().decode(item.logoBase64);
                        imageView.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                    } catch (Exception e) { imageView.setImage(null); }
                } else {
                    imageView.setImage(null);
                }
                if (item.avgRating >= 0) {
                    ratingLabel.setText("⭐ " + String.format("%.2f", item.avgRating));
                } else {
                    ratingLabel.setText("Loading...");
                }
                setGraphic(content);
            }
        }
    }
} 