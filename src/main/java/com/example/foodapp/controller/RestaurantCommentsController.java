package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import java.util.*;
import java.util.Base64;
import java.io.ByteArrayInputStream;

public class RestaurantCommentsController {
    @FXML private Button backBtn;
    @FXML private Label restaurantNameLabel;
    @FXML private ListView<CommentItem> commentsList;

    private int restaurantId;
    private String restaurantName;
    private String jwtToken;
    private Runnable onBack;

    public void setRestaurant(int id, String name, String jwtToken) {
        this.restaurantId = id;
        this.restaurantName = name;
        this.jwtToken = jwtToken;
        restaurantNameLabel.setText("Comments for " + name);
        fetchComments();
    }
    public void setOnBack(Runnable r) { this.onBack = r; }

    @FXML
    public void initialize() {
        commentsList.setCellFactory(list -> new CommentCell());
        if (backBtn != null) backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    private void fetchComments() {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/ratings/restaurant/" + restaurantId);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                if (jwtToken != null) conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                System.out.println("[DEBUG] Response code: " + code);
                if (code == 200) {
                    java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                    String json = sc.useDelimiter("\\A").next();
                    sc.close();
                    System.out.println("[DEBUG] Raw comments JSON length: " + json.length());
                    System.out.println("[DEBUG] First 500 chars: " + json.substring(0, Math.min(500, json.length())));
                    System.out.println("[DEBUG] Last 200 chars: " + json.substring(Math.max(0, json.length() - 200)));
                    
                    // Check if "comments" key exists
                    if (json.contains("\"comments\"")) {
                        System.out.println("[DEBUG] Found 'comments' key in JSON");
                        List<CommentItem> comments = parseComments(json);
                        Platform.runLater(() -> commentsList.getItems().setAll(comments));
                    } else {
                        System.out.println("[DEBUG] No 'comments' key found in JSON");
                        System.out.println("[DEBUG] Available keys: " + json.replaceAll("\\{[^}]*\"([^\"]+)\"[^}]*\\}", "$1"));
                    }
                } else {
                    System.out.println("[DEBUG] HTTP error: " + code);
                }
            } catch (Exception e) { 
                System.out.println("[DEBUG] Exception in fetchComments: " + e.getMessage());
                e.printStackTrace(); 
            }
        }).start();
    }

    private List<CommentItem> parseComments(String json) {
        List<CommentItem> list = new ArrayList<>();
        System.out.println("[DEBUG] Starting parseComments");
        
        int commIdx = json.indexOf("\"comments\":[");
        System.out.println("[DEBUG] Found comments index: " + commIdx);
        
        if (commIdx != -1) {
            // Find all comment objects by looking for each opening brace
            int objStart = commIdx;
            int commentCount = 0;
            Set<String> seenComments = new HashSet<>(); // To track duplicates
            
            while ((objStart = json.indexOf('{', objStart + 1)) != -1) {
                // Check if this brace is inside the comments array
                int prevBracket = json.lastIndexOf('[', objStart);
                int nextBracket = json.indexOf(']', objStart);
                
                // If this brace is between the comments array brackets, it's a comment object
                if (prevBracket >= commIdx && (nextBracket == -1 || nextBracket > objStart)) {
                    commentCount++;
                    System.out.println("[DEBUG] Found comment object #" + commentCount + " at index: " + objStart);
                    
                    // Find the end by looking for the closing brace after the created_at field
                    int createdAtIdx = json.indexOf("\"created_at\":", objStart);
                    if (createdAtIdx != -1) {
                        System.out.println("[DEBUG] Found created_at for comment #" + commentCount + " at index: " + createdAtIdx);
                        // Find the closing bracket and brace after created_at
                        int bracketEnd = json.indexOf(']', createdAtIdx);
                        if (bracketEnd != -1) {
                            int braceEnd = json.indexOf('}', bracketEnd);
                            if (braceEnd != -1) {
                                String obj = json.substring(objStart, braceEnd + 1);
                                System.out.println("[DEBUG] Comment object #" + commentCount + " length: " + obj.length());
                                
                                try {
                                    // Parse rating
                                    int rIdx = obj.indexOf("\"rating\":");
                                    int rating = 0;
                                    if (rIdx != -1) {
                                        int rStart = rIdx + 9;
                                        int rEnd = obj.indexOf(',', rStart);
                                        if (rEnd == -1) rEnd = obj.indexOf('}', rStart);
                                        String ratingStr = obj.substring(rStart, rEnd).replaceAll("[^0-9]", "").trim();
                                        rating = Integer.parseInt(ratingStr);
                                        System.out.println("[DEBUG] Parsed rating for comment #" + commentCount + ": " + rating);
                                    }
                                    
                                    // Parse comment
                                    int cIdx = obj.indexOf("\"comment\":");
                                    String comment = "";
                                    if (cIdx != -1) {
                                        int cStart = obj.indexOf('"', cIdx + 9) + 1;
                                        int cEnd = obj.indexOf('"', cStart);
                                        comment = obj.substring(cStart, cEnd);
                                        System.out.println("[DEBUG] Parsed comment #" + commentCount + ": " + comment);
                                    }
                                    
                                    // Parse images - image_base64 is an array of strings
                                    List<String> images = new ArrayList<>();
                                    int imgArrIdx = obj.indexOf("\"image_base64\":[");
                                    if (imgArrIdx != -1) {
                                        int imgArrStart = obj.indexOf('[', imgArrIdx);
                                        int imgArrEnd = obj.indexOf(']', imgArrStart);
                                        if (imgArrStart != -1 && imgArrEnd != -1) {
                                            String imgArr = obj.substring(imgArrStart + 1, imgArrEnd);
                                            // Split by comma and clean up quotes
                                            String[] imgParts = imgArr.split(",");
                                            for (String imgPart : imgParts) {
                                                imgPart = imgPart.trim();
                                                if (imgPart.startsWith("\"") && imgPart.endsWith("\"")) {
                                                    images.add(imgPart.substring(1, imgPart.length() - 1));
                                                }
                                            }
                                            System.out.println("[DEBUG] Parsed " + images.size() + " images for comment #" + commentCount);
                                        }
                                    }
                                    
                                    // Create a unique key for this comment to check for duplicates
                                    String commentKey = rating + "|" + comment + "|" + images.size();
                                    if (!seenComments.contains(commentKey)) {
                                        seenComments.add(commentKey);
                                        list.add(new CommentItem(rating, comment, images));
                                        System.out.println("[DEBUG] Successfully added comment #" + commentCount + " (unique)");
                                    } else {
                                        System.out.println("[DEBUG] Skipped duplicate comment #" + commentCount + " (key: " + commentKey + ")");
                                    }
                                } catch (Exception e) {
                                    System.out.println("[DEBUG] Exception parsing comment #" + commentCount + ": " + e.getMessage());
                                    e.printStackTrace();
                                }
                            } else {
                                System.out.println("[DEBUG] No closing brace found after created_at for comment #" + commentCount);
                            }
                        } else {
                            System.out.println("[DEBUG] No closing bracket found for created_at for comment #" + commentCount);
                        }
                    } else {
                        System.out.println("[DEBUG] No created_at field found for comment #" + commentCount);
                    }
                }
            }
            System.out.println("[DEBUG] Total comment objects found: " + commentCount);
        } else {
            System.out.println("[DEBUG] No comments array found in JSON");
        }
        
        System.out.println("[DEBUG] Total comments parsed: " + list.size());
        return list;
    }

    public static class CommentItem {
        public final int rating;
        public final String comment;
        public final List<String> images;
        public CommentItem(int rating, String comment, List<String> images) {
            this.rating = rating; this.comment = comment; this.images = images;
        }
    }

    public static class CommentCell extends ListCell<CommentItem> {
        private final VBox content = new VBox(8);
        private final Label ratingLabel = new Label();
        private final Label commentLabel = new Label();
        private final HBox imagesBox = new HBox(8);
        public CommentCell() {
            ratingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ff9800; -fx-font-weight: bold;");
            commentLabel.setStyle("-fx-font-size: 15px; -fx-padding: 4 0 0 0;");
            content.getChildren().addAll(ratingLabel, commentLabel, imagesBox);
        }
        @Override
        protected void updateItem(CommentItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                ratingLabel.setText("⭐ " + item.rating);
                commentLabel.setText(item.comment);
                imagesBox.getChildren().clear();
                if (item.images != null && !item.images.isEmpty()) {
                    for (String b64 : item.images) {
                        if (b64 != null && !b64.isEmpty()) {
                            try {
                                byte[] imgBytes = Base64.getDecoder().decode(b64);
                                ImageView imgView = new ImageView(new Image(new ByteArrayInputStream(imgBytes)));
                                imgView.setFitHeight(60);
                                imgView.setFitWidth(60);
                                imagesBox.getChildren().add(imgView);
                            } catch (Exception ignore) {}
                        }
                    }
                }
                setGraphic(content);
            }
        }
    }
} 