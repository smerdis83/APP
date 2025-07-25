package com.example.foodapp;

import com.example.foodapp.controller.LoginController;
import com.example.foodapp.controller.RegisterController;
import com.example.foodapp.controller.OrderHistoryController;
import com.example.foodapp.controller.OrderDetailsController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.BiConsumer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.foodapp.model.OrderSummary;
import java.util.List;
import java.util.ArrayList;

public class LoginApp extends Application {
    private Stage primaryStage;
    private String jwtToken = null;
    private String userRole = null;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        showLoginScreen();
    }

    private void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginScreen.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setOnLoginSuccess((phone, password) -> handleLogin(phone, password, controller));
            controller.setOnRegister(this::showRegisterScreen);
            Scene scene = new Scene(root);
            primaryStage.setTitle("Login App");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showRegisterScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RegisterScreen.fxml"));
            Parent root = loader.load();
            RegisterController controller = loader.getController();
            controller.setOnRegister(data -> handleRegister(data, controller));
            controller.setOnBack(this::showLoginScreen);
            Scene scene = new Scene(root, 650, 500);
            primaryStage.setTitle("Register");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handles login logic (calls backend, then shows dashboard)
    private void handleLogin(String phone, String password, LoginController controller) {
        controller.clearError();
        new Thread(() -> {
            try {
                String json = String.format("{\"phone\":\"%s\",\"password\":\"%s\"}", phone, password);
                java.net.URL url = new java.net.URL("http://localhost:8000/auth/login");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200) {
                    String role = extractRoleFromJson(resp);
                    String token = extractTokenFromJson(resp);
                    this.jwtToken = token;
                    this.userRole = role;
                    javafx.application.Platform.runLater(() -> showDashboard(primaryStage, role));
                } else {
                    String msg = "Login failed (" + code + "):\n" + resp;
                    javafx.application.Platform.runLater(() -> controller.showError(msg));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> controller.showError("Error: " + ex.getMessage()));
            }
        }).start();
    }

    private void handleRegister(RegisterController.RegisterData data, RegisterController controller) {
        controller.clearError();
        new Thread(() -> {
            try {
                String json = String.format("{\"fullName\":\"%s\",\"phone\":\"%s\",\"password\":\"%s\",\"role\":\"%s\"}",
                        data.name, data.phone, data.password, data.role);
                java.net.URL url = new java.net.URL("http://localhost:8000/auth/register");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200) {
                    javafx.application.Platform.runLater(() -> showLoginScreenWithMessage("Registration successful! Please log in."));
                } else {
                    String msg = "Registration failed (" + code + "):\n" + resp;
                    javafx.application.Platform.runLater(() -> controller.showError(msg));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> controller.showError("Error: " + ex.getMessage()));
            }
        }).start();
    }

    private void showLoginScreenWithMessage(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginScreen.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setOnLoginSuccess((phone, password) -> handleLogin(phone, password, controller));
            controller.setOnRegister(this::showRegisterScreen);
            controller.showError(message);
            Scene scene = new Scene(root, 600, 400);
            primaryStage.setTitle("Login App");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Shows a role-based dashboard scene
    public void showDashboard(Stage stage, String role) {
        try {
            if ("SELLER".equalsIgnoreCase(role)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SellerDashboard.fxml"));
                Parent root = loader.load();
                com.example.foodapp.controller.SellerDashboardController controller = loader.getController();
                controller.setWelcome("User");
                controller.setRole(role);
                controller.setOnProfile(v -> fetchAndShowProfilePage(stage, role));
                controller.setOnLogout(() -> handleLogout());
                controller.setOnAddRestaurant(() -> showAddRestaurantScreen(stage));
                controller.setOnMyRestaurants(() -> showMyRestaurantsScreen(stage));
                controller.setOnRestaurantList(() -> showRestaurantListScreen(stage));
                controller.setOnOrderManagement(() -> showRestaurantOrdersScreen(stage));
                controller.setOnSimpleOrderViewer(() -> showSimpleRestaurantOrdersScreen(stage));
                Scene scene = new Scene(root, 1000, 700);
                stage.setTitle("Seller Dashboard");
                stage.setScene(scene);
                stage.centerOnScreen();
            } else if ("COURIER".equalsIgnoreCase(role)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/CourierDashboard.fxml"));
                Parent root = loader.load();
                com.example.foodapp.controller.CourierDashboardController controller = loader.getController();
                controller.setWelcome("User");
                controller.setRole(role);
                controller.setOnProfile(() -> fetchAndShowProfilePage(stage, role));
                controller.setOnLogout(() -> handleLogout());
                controller.setOnDeliveryManagement(() -> showDeliveryManagementScreen(stage));
                Scene scene = new Scene(root, 1000, 700);
                stage.setTitle("Courier Dashboard");
                stage.setScene(scene);
                stage.centerOnScreen();
            } else { // BUYER or default
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/BuyerDashboard.fxml"));
                Parent root = loader.load();
                com.example.foodapp.controller.BuyerDashboardController controller = loader.getController();
                controller.setApp(this);
                controller.setWelcome("User");
                controller.setRole(role);
                controller.setJwtToken(jwtToken);
                controller.setOnProfile(v -> fetchAndShowProfilePage(stage, role));
                controller.setOnLogout(() -> handleLogout());
                controller.setOnOrderHistory(() -> showOrderHistoryScreen(stage));
                controller.setOnFavorites(() -> showFavoritesScreen(stage));
                controller.setOnRestaurantList(() -> showRestaurantListScreen(stage));
                Scene scene = new Scene(root, 1000, 700);
                stage.setTitle("Buyer Dashboard");
                stage.setScene(scene);
                stage.centerOnScreen();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showOrderHistoryScreen(Stage stage) {
        showOrderHistoryScreen(stage, false);
    }
    public void showOrderHistoryScreen(Stage stage, boolean activeOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OrderHistoryScreen.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.OrderHistoryController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setActiveOnly(activeOnly);
            controller.setOnBack(() -> showDashboard(stage, userRole));
            fetchOrders(controller, activeOnly);
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle(activeOnly ? "Active Orders" : "Order History");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void fetchOrders(com.example.foodapp.controller.OrderHistoryController controller) {
        fetchOrders(controller, false);
    }
    public void fetchOrders(com.example.foodapp.controller.OrderHistoryController controller, boolean activeOnly) {
        controller.clearMessage();
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/orders/history");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200) {
                    List<com.example.foodapp.controller.OrderHistoryController.OrderSummary> orderSummaries = new ArrayList<>();
                    int idx = 0;
                    while ((idx = resp.indexOf("{", idx)) != -1) {
                        int idIdx = resp.indexOf("\"id\":", idx);
                        int statusIdx = resp.indexOf("\"status\":", idx);
                        int priceIdx = resp.indexOf("\"pay_price\":", idx);
                        int createdIdx = resp.indexOf("\"created_at\":", idx);
                        int updatedIdx = resp.indexOf("\"updated_at\":", idx);
                        int restIdx = resp.indexOf("\"vendor_name\":", idx);
                        int courierIdx = resp.indexOf("\"courier_id\":", idx);
                        if (idIdx == -1 || statusIdx == -1 || priceIdx == -1) break;
                        int idStart = idIdx + 5;
                        int idEnd = resp.indexOf(',', idStart);
                        String id = resp.substring(idStart, idEnd).replaceAll("[^0-9]", "").trim();
                        int statusStart = resp.indexOf('"', statusIdx + 9) + 1;
                        int statusEnd = resp.indexOf('"', statusStart);
                        String status = resp.substring(statusStart, statusEnd);
                        int priceStart = priceIdx + 11;
                        int priceEnd = resp.indexOf(',', priceStart);
                        if (priceEnd == -1) priceEnd = resp.indexOf('}', priceStart);
                        String price = resp.substring(priceStart, priceEnd).replaceAll("[^0-9]", "").trim();
                        String created = createdIdx != -1 ? extractStringField(resp, createdIdx + 13) : "";
                        String updated = updatedIdx != -1 ? extractStringField(resp, updatedIdx + 13) : "";
                        String restaurant = restIdx != -1 ? extractStringField(resp, restIdx + 14) : "";
                        // Try to extract from 'restaurant' or 'vendor' if 'vendor_name' is missing
                        if ((restaurant == null || restaurant.isEmpty())) {
                            int restObjIdx = resp.indexOf("\"restaurant\":", idx);
                            if (restObjIdx != -1) {
                                int nameIdx = resp.indexOf("\"name\":", restObjIdx);
                                if (nameIdx != -1) {
                                    int nameStart = resp.indexOf('"', nameIdx + 7) + 1;
                                    int nameEnd = resp.indexOf('"', nameStart);
                                    if (nameStart > 0 && nameEnd > nameStart) {
                                        restaurant = resp.substring(nameStart, nameEnd);
                                    }
                                }
                            }
                            int vendorObjIdx = resp.indexOf("\"vendor\":", idx);
                            if ((restaurant == null || restaurant.isEmpty()) && vendorObjIdx != -1) {
                                int nameIdx = resp.indexOf("\"name\":", vendorObjIdx);
                                if (nameIdx != -1) {
                                    int nameStart = resp.indexOf('"', nameIdx + 7) + 1;
                                    int nameEnd = resp.indexOf('"', nameStart);
                                    if (nameStart > 0 && nameEnd > nameStart) {
                                        restaurant = resp.substring(nameStart, nameEnd);
                                    }
                                }
                            }
                        }
                        if (restaurant == null || restaurant.isEmpty()) restaurant = "Unknown Restaurant";
                        // Extract courier_id
                        String courierId = "";
                        if (courierIdx != -1) {
                            int courierStart = courierIdx + 13;
                            int courierEnd = resp.indexOf(',', courierStart);
                            if (courierEnd == -1) courierEnd = resp.indexOf('}', courierStart);
                            courierId = resp.substring(courierStart, courierEnd).replaceAll("[^0-9]", "").trim();
                        }
                        // Filter for active orders if needed
                        if (!activeOnly || (status != null && !status.equalsIgnoreCase("completed") && !status.equalsIgnoreCase("delivered") && !status.equalsIgnoreCase("unpaid and cancelled") && !status.equalsIgnoreCase("cancelled"))) {
                            orderSummaries.add(new com.example.foodapp.controller.OrderHistoryController.OrderSummary(id, restaurant, status, price, created, updated, courierId));
                        }
                        idx = resp.indexOf('}', priceEnd) + 1;
                    }
                    javafx.application.Platform.runLater(() -> controller.setOrders(orderSummaries));
                } else {
                    String msg = "Failed to fetch orders (" + code + "):\n" + resp;
                    javafx.application.Platform.runLater(() -> controller.showMessage(msg));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> controller.showMessage("Error: " + ex.getMessage()));
            }
        }).start();
    }

    private String extractStringField(String resp, int startIdx) {
        int start = resp.indexOf('"', startIdx) + 1;
        int end = resp.indexOf('"', start);
        if (start > 0 && end > start) return resp.substring(start, end);
        return "";
    }

    private void showOrderDetailsScreen(Stage stage, OrderSummary order) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OrderDetailsScreen.fxml"));
            Parent root = loader.load();
            OrderDetailsController controller = loader.getController();
            controller.setOnBack(() -> showOrderHistoryScreen(stage));
            fetchOrderDetails(controller, order.id);
            Scene scene = new Scene(root, 1000, 700);
            stage.setTitle("Order Details");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchOrderDetails(OrderDetailsController controller, String idNum) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/orders/" + idNum);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200) {
                    // Parse fields from JSON (id, status, pay_price, created_at, updated_at, items)
                    java.util.function.Function<String, String> getField = (String key) -> {
                        String marker = "\"" + key + "\":";
                        int idx = resp.indexOf(marker);
                        if (idx != -1) {
                            int start = idx + marker.length();
                            char first = resp.charAt(start);
                            if (first == '"') {
                                int end = resp.indexOf('"', start + 1);
                                if (end > start + 1) return resp.substring(start + 1, end);
                            } else {
                                int end = resp.indexOf(',', start);
                                if (end == -1) end = resp.indexOf('}', start);
                                if (end > start) return resp.substring(start, end).replaceAll("[\n\r]", "").trim();
                            }
                        }
                        return "Empty";
                    };
                    java.util.List<String> items = new java.util.ArrayList<>();
                    // Parse items: [{"item_id":12,"quantity":6}, ...]
                    int arrIdx = resp.indexOf("\"items\":");
                    if (arrIdx != -1) {
                        int arrStart = resp.indexOf('[', arrIdx);
                        int arrEnd = resp.indexOf(']', arrStart);
                        if (arrStart != -1 && arrEnd != -1) {
                            String arr = resp.substring(arrStart + 1, arrEnd);
                            int idx = 0;
                            java.util.List<Integer> itemIds = new java.util.ArrayList<>();
                            java.util.List<Integer> quantities = new java.util.ArrayList<>();
                            while ((idx = arr.indexOf("{", idx)) != -1) {
                                int idIdx = arr.indexOf("\"item_id\":", idx);
                                int qtyIdx = arr.indexOf("\"quantity\":", idx);
                                if (idIdx == -1 || qtyIdx == -1) break;
                                int idStart = idIdx + 10;
                                int idEnd = arr.indexOf(',', idStart);
                                if (idEnd == -1) idEnd = arr.indexOf('}', idStart);
                                String idStr = arr.substring(idStart, idEnd).replaceAll("[^0-9]", "").trim();
                                int itemId = Integer.parseInt(idStr);
                                int qtyStart = qtyIdx + 10;
                                int qtyEnd = arr.indexOf(',', qtyStart);
                                if (qtyEnd == -1) qtyEnd = arr.indexOf('}', qtyStart);
                                String qtyStr = arr.substring(qtyStart, qtyEnd).replaceAll("[^0-9]", "").trim();
                                int quantity = Integer.parseInt(qtyStr);
                                itemIds.add(itemId);
                                quantities.add(quantity);
                                idx = arr.indexOf('}', qtyEnd) + 1;
                            }
                            // For each item_id, fetch name from /items/{id}
                            java.util.List<String> itemNames = new java.util.ArrayList<>();
                            for (int i = 0; i < itemIds.size(); i++) {
                                int itemId = itemIds.get(i);
                                int quantity = quantities.get(i);
                                String name = "Item #" + itemId;
                                try {
                                    java.net.URL itemUrl = new java.net.URL("http://localhost:8000/items/" + itemId);
                                    java.net.HttpURLConnection itemConn = (java.net.HttpURLConnection) itemUrl.openConnection();
                                    itemConn.setRequestMethod("GET");
                                    itemConn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                                    int itemCode = itemConn.getResponseCode();
                                    String itemResp;
                                    try (java.util.Scanner scanner = new java.util.Scanner(
                                            itemCode >= 200 && itemCode < 300 ? itemConn.getInputStream() : itemConn.getErrorStream(),
                                            java.nio.charset.StandardCharsets.UTF_8)) {
                                        scanner.useDelimiter("\\A");
                                        itemResp = scanner.hasNext() ? scanner.next() : "";
                                    }
                                    int nameIdx = itemResp.indexOf("\"name\":");
                                    if (nameIdx != -1) {
                                        int nameStart = itemResp.indexOf('"', nameIdx + 7) + 1;
                                        int nameEnd = itemResp.indexOf('"', nameStart);
                                        if (nameStart > 0 && nameEnd > nameStart) {
                                            name = itemResp.substring(nameStart, nameEnd);
                                        }
                                    }
                                } catch (Exception e) {
                                    // fallback to Item #id
                                }
                                itemNames.add(name + " x" + quantity);
                            }
                            items.addAll(itemNames);
                        }
                    }
                    // Parse created_at and updated_at as arrays directly from JSON
                    String createdAt = null;
                    String updatedAt = null;
                    java.util.regex.Pattern dateArrPattern = java.util.regex.Pattern.compile("\"created_at\":\\s*\\[(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\]");
                    java.util.regex.Matcher matcher = dateArrPattern.matcher(resp);
                    if (matcher.find()) {
                        createdAt = String.format("%s-%02d-%02dT%02d:%02d:%02d",
                                matcher.group(1), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                                Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)), Integer.parseInt(matcher.group(6)));
                    }
                    dateArrPattern = java.util.regex.Pattern.compile("\"updated_at\":\\s*\\[(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\]");
                    matcher = dateArrPattern.matcher(resp);
                    if (matcher.find()) {
                        updatedAt = String.format("%s-%02d-%02dT%02d:%02d:%02d",
                                matcher.group(1), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)),
                                Integer.parseInt(matcher.group(4)), Integer.parseInt(matcher.group(5)), Integer.parseInt(matcher.group(6)));
                    }
                    if (createdAt == null) createdAt = getField.apply("created_at");
                    if (updatedAt == null) updatedAt = getField.apply("updated_at");
                    final String finalCreatedAt = createdAt;
                    final String finalUpdatedAt = updatedAt;
                    javafx.application.Platform.runLater(() -> controller.setOrderDetails(
                        getField.apply("id"),
                        getField.apply("status"),
                        getField.apply("pay_price"),
                        finalCreatedAt,
                        finalUpdatedAt,
                        items
                    ));
                } else {
                    // Show error in all fields
                    javafx.application.Platform.runLater(() -> controller.setOrderDetails(
                        "Error", "Error", "Error", "Error", "Error", java.util.Collections.singletonList("Failed to fetch order details")));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> controller.setOrderDetails(
                    "Error", "Error", "Error", "Error", "Error", java.util.Collections.singletonList("Error: " + ex.getMessage())));
            }
        }).start();
    }

    private void handleLogout() {
        this.jwtToken = null;
        this.userRole = null;
        showLoginScreen();
    }

    // Fetches the user profile and shows it in a dedicated Profile page
    private void fetchAndShowProfilePage(Stage stage, String role) {
        if (jwtToken == null) {
            showAlert("Not logged in", "No JWT token found.");
            return;
        }
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/auth/profile");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200) {
                    javafx.application.Platform.runLater(() -> showProfilePage(stage, role, resp));
                } else {
                    String msg = "Failed to fetch profile (" + code + "):\n" + resp;
                    javafx.application.Platform.runLater(() -> showAlert("Profile Error", msg));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> showAlert("Error", ex.getMessage()));
            }
        }).start();
    }

    // Shows the Profile page with labeled fields and a back button
    private void showProfilePage(Stage stage, String role, String json) {
        // Very basic JSON parsing (for demo; use a real JSON library for production)
        java.util.function.Function<String, String> getField = (String key) -> {
            String marker = "\"" + key + "\":";
            int idx = json.indexOf(marker);
            if (idx != -1) {
                int start = idx + marker.length();
                char first = json.charAt(start);
                if (first == '"') {
                    int end = json.indexOf('"', start + 1);
                    if (end > start + 1) {
                        String val = json.substring(start + 1, end);
                        return val.isEmpty() ? "Empty" : val;
                    }
                } else if (first == 'n') { // null
                    return "Empty";
                } else { // number or boolean
                    int end = json.indexOf(',', start);
                    if (end == -1) end = json.indexOf('}', start);
                    if (end > start) {
                        String val = json.substring(start, end).replaceAll("[\n\r]", "").trim();
                        return val.isEmpty() ? "Empty" : val;
                    }
                }
            }
            return "Empty";
        };
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProfileScreen.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.ProfileController controller = loader.getController();
            controller.setProfile(
                getField.apply("fullName"),
                getField.apply("phone"),
                getField.apply("email"),
                getField.apply("role"),
                getField.apply("address"),
                getField.apply("walletBalance"),
                getField.apply("enabled"),
                getField.apply("createdAt"),
                getField.apply("updatedAt"),
                getField.apply("profileImageBase64")
            );
            controller.setOnBack(() -> showDashboard(stage, role));
            controller.setOnSave(data -> saveProfile(stage, role, data));
            Scene scene = new Scene(root, 1000, 700);
            stage.setTitle("Profile");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProfile(Stage stage, String role, com.example.foodapp.controller.ProfileController.ProfileData data) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/auth/profile");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                conn.setDoOutput(true);
                String json = String.format("{\"full_name\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"address\":\"%s\",\"profileImageBase64\":\"%s\"}",
                    data.name.replace("\"", "'"),
                    data.phone.replace("\"", "'"),
                    data.email.replace("\"", "'"),
                    data.address.replace("\"", "'"),
                    data.profileImageBase64 == null ? "" : data.profileImageBase64.replace("\"", "'"));
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                String resp;
                try (java.util.Scanner scanner = new java.util.Scanner(
                        code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                        java.nio.charset.StandardCharsets.UTF_8)) {
                    scanner.useDelimiter("\\A");
                    resp = scanner.hasNext() ? scanner.next() : "";
                }
                if (code == 200) {
                    javafx.application.Platform.runLater(() -> fetchAndShowProfilePage(stage, role));
                } else {
                    javafx.application.Platform.runLater(() -> showAlert("Profile Update Failed", "Failed to update profile (" + code + "):\n" + resp));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> showAlert("Error", ex.getMessage()));
            }
        }).start();
    }

    // Utility: show an alert dialog
    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Extracts the user's role from the backend JSON response (very basic, not a full JSON parser)
    private String extractRoleFromJson(String json) {
        String marker = "\"role\":\"";
        int idx = json.indexOf(marker);
        if (idx != -1) {
            int start = idx + marker.length();
            int end = json.indexOf('"', start);
            if (end > start) {
                return json.substring(start, end);
            }
        }
        return "UNKNOWN";
    }

    // Extracts the JWT token from the backend JSON response
    private String extractTokenFromJson(String json) {
        String marker = "\"token\":\"";
        int idx = json.indexOf(marker);
        if (idx != -1) {
            int start = idx + marker.length();
            int end = json.indexOf('"', start);
            if (end > start) {
                return json.substring(start, end);
            }
        }
        return null;
    }

    private void showFavoritesScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FavoritesScreen.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.FavoritesController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setOnBack(() -> {
                stage.setFullScreen(false);
                showDashboard(stage, userRole);
            });
            controller.setOnRestaurantClick(item -> showRestaurantPage(stage, item.id, item.name, item.logoBase64, () -> showFavoritesScreen(stage)));
            Scene scene = new Scene(root, 500, 600);
            stage.setTitle("Favorites");
            stage.setScene(scene);
            stage.setFullScreen(true);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showRestaurantPage(Stage stage, int restaurantId, String restaurantName, String logoBase64, Runnable onBack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RestaurantPage.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.RestaurantPageController controller = loader.getController();
            controller.setApp(this);
            controller.setRestaurant(restaurantId, restaurantName, logoBase64, this.jwtToken);
            controller.setOnBack(onBack);
            Scene scene = new Scene(root, 1200, 800);
            stage.setTitle(restaurantName);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRestaurantListScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RestaurantList.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.RestaurantListController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setOnBack(() -> showDashboard(stage, userRole));
            controller.setOnRestaurantClick(item -> showRestaurantPage(stage, item.id, item.name, item.logoBase64, () -> showRestaurantListScreen(stage)));
            Scene scene = new Scene(root, 1000, 800);
            stage.setTitle("All Restaurants");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAddRestaurantScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddRestaurant.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.AddRestaurantController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setOnBack(() -> showDashboard(stage, userRole));
            Scene scene = new Scene(root, 500, 400);
            stage.setTitle("Add Restaurant");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMyRestaurantsScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MyRestaurants.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.MyRestaurantsController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setOnBack(() -> showDashboard(stage, userRole));
            controller.setOnManageMenus(item -> showMenuListScreen(stage, item.id, item.name));
            controller.setOnEditRestaurant(item -> showEditRestaurantScreen(stage, item));
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("My Restaurants");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMenuListScreen(Stage stage, int restaurantId, String restaurantName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuList.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.MenuListController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setRestaurant(restaurantId, restaurantName);
            controller.setOnBack(() -> showMyRestaurantsScreen(stage));
            controller.setOnAddMenu(() -> showAddMenuScreen(stage, restaurantId, restaurantName));
            controller.setOnMenuDetail(menuItem -> showMenuDetailScreen(stage, restaurantId, restaurantName, menuItem.title));
            controller.loadMenus();
            Scene scene = new Scene(root, 500, 400);
            stage.setTitle("Menus for " + restaurantName);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAddMenuScreen(Stage stage, int restaurantId, String restaurantName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AddMenu.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.AddMenuController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setDefaultRestaurantId(restaurantId);
            controller.setOnBack(() -> showMenuListScreen(stage, restaurantId, restaurantName));
            Scene scene = new Scene(root, 500, 350);
            stage.setTitle("Add Menu to " + restaurantName);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showMenuDetailScreen(Stage stage, int restaurantId, String restaurantName, String menuTitle) {
        try {
            String encodedMenuTitle = java.net.URLEncoder.encode(menuTitle, "UTF-8");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MenuDetail.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.MenuDetailController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setRestaurantAndMenu(restaurantId, menuTitle); // pass original for display, encode for requests
            controller.setOnBack(() -> showMenuListScreen(stage, restaurantId, restaurantName));
            controller.loadFoods();
            Scene scene = new Scene(root, 600, 600);
            stage.setTitle(menuTitle + " - " + restaurantName);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showSelectAddressScreen(javafx.stage.Stage stage, java.util.function.Consumer<String> onSelect, Runnable onBack) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/SelectAddress.fxml"));
            javafx.scene.Parent root = loader.load();
            com.example.foodapp.controller.SelectAddressController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setOnSelect(onSelect);
            controller.setOnBack(onBack);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 500, 500);
            stage.setTitle("Select Address");
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showPaymentPage(javafx.stage.Stage stage, int restaurantId, String restaurantName, String logoBase64, String address, java.util.List<com.example.foodapp.controller.RestaurantPageController.BasketItem> basketItems, String jwtToken, Runnable onBack) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/Payment.fxml"));
            javafx.scene.Parent root = loader.load();
            com.example.foodapp.controller.PaymentController controller = loader.getController();
            controller.setApp(this);
            controller.setJwtToken(jwtToken);
            controller.setOrderDetails(restaurantName, restaurantId, address,
                basketItems.stream().map(b -> new com.example.foodapp.controller.PaymentController.Item(b.food.id, b.food.name, b.quantity, b.food.price)).toList(),
                basketItems.stream().mapToInt(b -> b.food.price * b.quantity).sum()
            );
            controller.setOnBack(() -> onBack.run());
            controller.setOnSuccess(() -> {
                javafx.application.Platform.runLater(() -> {
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Payment successful! Order placed.").showAndWait();
                    onBack.run();
                });
            });
            controller.setOnTopUp(() -> showTopUpWalletPage(stage, jwtToken, () -> showPaymentPage(stage, restaurantId, restaurantName, logoBase64, address, basketItems, jwtToken, onBack)));
            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showTopUpWalletPage(javafx.stage.Stage stage, String jwtToken, Runnable onBack) {
        showTopUpWalletPage(stage, jwtToken, onBack, null);
    }
    public void showTopUpWalletPage(javafx.stage.Stage stage, String jwtToken, Runnable onBack, Integer prefillAmount) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/TopUpWallet.fxml"));
            javafx.scene.Parent root = loader.load();
            com.example.foodapp.controller.TopUpWalletController controller = loader.getController();
            controller.setJwtToken(jwtToken);
            controller.setOnBack(onBack);
            if (prefillAmount != null) controller.setPrefillAmount(prefillAmount);
            stage.setScene(new javafx.scene.Scene(root));
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showRestaurantOrdersScreen(javafx.stage.Stage stage) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/RestaurantOrders.fxml"));
            javafx.scene.Parent root = loader.load();
            com.example.foodapp.controller.RestaurantOrdersController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setOnBack(() -> showDashboard(stage, userRole));
            stage.setScene(new javafx.scene.Scene(root, 1200, 700));
            stage.setTitle("Order Management");
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void showSimpleRestaurantOrdersScreen(javafx.stage.Stage stage) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/RestaurantOrders.fxml"));
            javafx.scene.Parent root = loader.load();
            com.example.foodapp.controller.RestaurantOrdersController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setOnBack(() -> showDashboard(stage, userRole));
            stage.setScene(new javafx.scene.Scene(root, 800, 600));
            stage.setTitle("Restaurant Order Management");
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    // TODO: Wire this to a button in the seller dashboard for quick testing

    public void showDeliveryManagementScreen(javafx.stage.Stage stage) {
        // TODO: Implement delivery management screen
        // For now, just show an alert
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Delivery Management");
        alert.setHeaderText("Feature Not Yet Implemented");
        alert.setContentText("Delivery management functionality will be implemented in a future update.");
        alert.showAndWait();
    }

    private void showEditRestaurantScreen(Stage stage, com.example.foodapp.controller.MyRestaurantsController.RestaurantItem restaurantItem) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/EditRestaurant.fxml"));
            Parent root = loader.load();
            com.example.foodapp.controller.EditRestaurantController controller = loader.getController();
            controller.setJwtToken(this.jwtToken);
            controller.setRestaurantItem(restaurantItem);
            controller.setOnBack(() -> showMyRestaurantsScreen(stage));
            Scene scene = new Scene(root, 600, 700);
            stage.setTitle("Edit Restaurant - " + restaurantItem.name);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
} 