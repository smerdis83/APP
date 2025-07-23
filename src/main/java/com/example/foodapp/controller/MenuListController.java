package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import java.util.function.Consumer;

public class MenuListController {
    @FXML private Label restaurantLabel;
    @FXML private ListView<MenuItem> menuList;
    @FXML private Button addMenuBtn;
    @FXML private Button backBtn;
    @FXML private Label messageLabel;

    private String jwtToken;
    private int restaurantId;
    private String restaurantName;
    private Runnable onBack;
    private Runnable onAddMenu;
    private Consumer<MenuItem> onMenuDetail;
    private ObservableList<MenuItem> menus = FXCollections.observableArrayList();

    public void setJwtToken(String token) { this.jwtToken = token; }
    public void setRestaurant(int id, String name) { this.restaurantId = id; this.restaurantName = name; }
    public void setOnBack(Runnable r) { this.onBack = r; }
    public void setOnAddMenu(Runnable r) { this.onAddMenu = r; }
    public void setOnMenuDetail(Consumer<MenuItem> c) { this.onMenuDetail = c; }

    @FXML
    public void initialize() {
        menuList.setItems(menus);
        menuList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.title);
            }
        });
        menuList.setOnMouseClicked(ev -> {
            if (ev.getClickCount() == 2) {
                MenuItem item = menuList.getSelectionModel().getSelectedItem();
                if (item != null && onMenuDetail != null) onMenuDetail.accept(item);
            }
        });
        addMenuBtn.setOnAction(e -> { if (onAddMenu != null) onAddMenu.run(); });
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });
    }

    public void loadMenus() {
        restaurantLabel.setText("Menus for: " + restaurantName);
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/restaurants/" + restaurantId + "/menus");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch menus: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                List<MenuItem> items = parseMenus(json);
                Platform.runLater(() -> menus.setAll(items));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    menus.clear();
                    messageLabel.setText("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private List<MenuItem> parseMenus(String json) {
        List<MenuItem> list = new ArrayList<>();
        int idx = 0;
        while ((idx = json.indexOf("\"title\":", idx)) != -1) {
            int start = json.indexOf('"', idx + 8) + 1;
            int end = json.indexOf('"', start);
            String title = json.substring(start, end);
            list.add(new MenuItem(title));
            idx = end;
        }
        return list;
    }

    public static class MenuItem {
        public final String title;
        public MenuItem(String title) { this.title = title; }
        @Override public String toString() { return title; }
    }
} 