package com.example.foodapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

public class AdminDashboardController {
    @FXML private TabPane adminTabPane;
    @FXML private TableView<AdminUser> usersTable;
    @FXML private TableView<AdminRestaurant> restaurantsTable;
    @FXML private TableView<AdminOrder> ordersTable;
    @FXML private TableView<AdminTransaction> transactionsTable;
    @FXML private TableView<Object> salesTable;
    @FXML private TableView<Object> analyticsTable;
    @FXML private Label userCountLabel;
    @FXML private Label restaurantCountLabel;
    @FXML private Label totalSalesLabel;
    @FXML private Label orderCountLabel;
    @FXML private Button logoutBtn;
    @FXML private Button deleteUserBtn;
    @FXML private Button deleteRestaurantBtn;
    @FXML private Button deleteOrderBtn;
    @FXML private Button enableUserBtn;
    @FXML private Button enableRestaurantBtn;
    @FXML private TableView<AdminFood> foodsTable;
    @FXML private Button deleteFoodBtn;
    private ObservableList<AdminFood> foods = FXCollections.observableArrayList();

    private String jwtToken;
    private Runnable onLogout;

    public void setJwtToken(String token) { this.jwtToken = token; loadAllAdminData(); }
    public void setOnLogout(Runnable callback) { this.onLogout = callback; }

    private void loadAllAdminData() {
        loadUsers();
        loadRestaurants();
        loadOrders();
        loadSales();
        loadTransactions();
        loadAnalytics();
        loadFoods();
    }

    public static class AdminUser {
        private final int id;
        private final String name;
        private final String phone;
        private final String email;
        private final String role;
        private final boolean enabled;
        public AdminUser(int id, String name, String phone, String email, String role, boolean enabled) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.role = role;
            this.enabled = enabled;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isEnabled() { return enabled; }
    }
    // --- Restaurant Model ---
    public static class AdminRestaurant {
        private final int id;
        private final String name;
        private final String address;
        private final String phone;
        private final int ownerId;
        private final boolean enabled;
        public AdminRestaurant(int id, String name, String address, String phone, int ownerId, boolean enabled) {
            this.id = id; this.name = name; this.address = address; this.phone = phone; this.ownerId = ownerId; this.enabled = enabled;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getPhone() { return phone; }
        public int getOwnerId() { return ownerId; }
        public boolean isEnabled() { return enabled; }
    }
    // --- Order Model ---
    public static class AdminOrder {
        private final int id;
        private final String status;
        private final int customerId;
        private final int vendorId;
        private final int payPrice;
        private final String createdAt;
        public AdminOrder(int id, String status, int customerId, int vendorId, int payPrice, String createdAt) {
            this.id = id; this.status = status; this.customerId = customerId; this.vendorId = vendorId; this.payPrice = payPrice; this.createdAt = createdAt;
        }
        public int getId() { return id; }
        public String getStatus() { return status; }
        public int getCustomerId() { return customerId; }
        public int getVendorId() { return vendorId; }
        public int getPayPrice() { return payPrice; }
        public String getCreatedAt() { return createdAt; }
    }
    // --- Transaction Model ---
    public static class AdminTransaction {
        private final int id;
        private final int userId;
        private final int amount;
        private final String type;
        private final String createdAt;
        public AdminTransaction(int id, int userId, int amount, String type, String createdAt) {
            this.id = id; this.userId = userId; this.amount = amount; this.type = type; this.createdAt = createdAt;
        }
        public int getId() { return id; }
        public int getUserId() { return userId; }
        public int getAmount() { return amount; }
        public String getType() { return type; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class AdminFood {
        private final int id;
        private final String name;
        private final String description;
        private final int price;
        private final Integer discountPrice;
        private final int supply;
        private final int vendorId;
        public AdminFood(int id, String name, String description, int price, Integer discountPrice, int supply, int vendorId) {
            this.id = id; this.name = name; this.description = description; this.price = price; this.discountPrice = discountPrice; this.supply = supply; this.vendorId = vendorId;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getPrice() { return price; }
        public Integer getDiscountPrice() { return discountPrice; }
        public int getSupply() { return supply; }
        public int getVendorId() { return vendorId; }
    }

    @FXML
    public void initialize() {
        logoutBtn.setOnAction(e -> { if (onLogout != null) onLogout.run(); });
        deleteUserBtn.setOnAction(e -> handleDeleteUser());
        deleteRestaurantBtn.setOnAction(e -> handleDeleteRestaurant());
        deleteOrderBtn.setOnAction(e -> handleDeleteOrder());
        enableUserBtn.setOnAction(e -> handleEnableUser());
        enableRestaurantBtn.setOnAction(e -> handleEnableRestaurant());
        if (deleteFoodBtn != null) {
            deleteFoodBtn.setOnAction(e -> handleDeleteFood());
        }
        // Enable/disable delete buttons based on selection
        deleteUserBtn.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());
        deleteRestaurantBtn.disableProperty().bind(restaurantsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteOrderBtn.disableProperty().bind(ordersTable.getSelectionModel().selectedItemProperty().isNull());
        // Setup usersTable columns
        TableColumn<AdminUser, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));
        TableColumn<AdminUser, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        TableColumn<AdminUser, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        TableColumn<AdminUser, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getEmail()));
        TableColumn<AdminUser, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getRole()));
        TableColumn<AdminUser, String> enabledCol = new TableColumn<>("Enabled");
        enabledCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().isEnabled() ? "Yes" : "No"
        ));
        usersTable.getColumns().setAll(idCol, nameCol, phoneCol, emailCol, roleCol, enabledCol);
        // Add Enable User button
        Button enableUserBtn = new Button("Enable Selected User");
        enableUserBtn.setOnAction(e -> handleEnableUser());
        enableUserBtn.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());
        // Add to UI (assumes there is an HBox for user actions)
        // You may need to add this button to the FXML or dynamically to the HBox in code.
        // Setup restaurantsTable columns
        TableColumn<AdminRestaurant, String> restIdCol = new TableColumn<>("ID");
        restIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));
        TableColumn<AdminRestaurant, String> restNameCol = new TableColumn<>("Name");
        restNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        TableColumn<AdminRestaurant, String> restAddrCol = new TableColumn<>("Address");
        restAddrCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getAddress()));
        TableColumn<AdminRestaurant, String> restPhoneCol = new TableColumn<>("Phone");
        restPhoneCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));
        TableColumn<AdminRestaurant, String> restOwnerCol = new TableColumn<>("Owner ID");
        restOwnerCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getOwnerId())));
        TableColumn<AdminRestaurant, String> restEnabledCol = new TableColumn<>("Enabled");
        restEnabledCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().isEnabled() ? "Yes" : "No"
        ));
        restaurantsTable.getColumns().setAll(restIdCol, restNameCol, restAddrCol, restPhoneCol, restOwnerCol, restEnabledCol);
        // Add Enable Restaurant button
        Button enableRestaurantBtn = new Button("Enable Selected Restaurant");
        enableRestaurantBtn.setOnAction(e -> handleEnableRestaurant());
        enableRestaurantBtn.disableProperty().bind(restaurantsTable.getSelectionModel().selectedItemProperty().isNull());
        // Add to UI (assumes there is an HBox for restaurant actions)
        // You may need to add this button to the FXML or dynamically to the HBox in code.
        // Setup ordersTable columns
        TableColumn<AdminOrder, String> orderIdCol = new TableColumn<>("ID");
        orderIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));
        TableColumn<AdminOrder, String> orderStatusCol = new TableColumn<>("Status");
        orderStatusCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));
        TableColumn<AdminOrder, String> orderCustCol = new TableColumn<>("Customer ID");
        orderCustCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getCustomerId())));
        TableColumn<AdminOrder, String> orderVendCol = new TableColumn<>("Vendor ID");
        orderVendCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getVendorId())));
        TableColumn<AdminOrder, String> orderPriceCol = new TableColumn<>("Pay Price");
        orderPriceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getPayPrice())));
        TableColumn<AdminOrder, String> orderCreatedCol = new TableColumn<>("Created At");
        orderCreatedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedAt()));
        ordersTable.getColumns().setAll(orderIdCol, orderStatusCol, orderCustCol, orderVendCol, orderPriceCol, orderCreatedCol);
        // Setup transactionsTable columns
        TableColumn<AdminTransaction, String> txIdCol = new TableColumn<>("ID");
        txIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));
        TableColumn<AdminTransaction, String> txUserCol = new TableColumn<>("User ID");
        txUserCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getUserId())));
        TableColumn<AdminTransaction, String> txAmountCol = new TableColumn<>("Amount");
        txAmountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getAmount())));
        TableColumn<AdminTransaction, String> txTypeCol = new TableColumn<>("Type");
        txTypeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getType()));
        TableColumn<AdminTransaction, String> txCreatedCol = new TableColumn<>("Created At");
        txCreatedCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCreatedAt()));
        transactionsTable.getColumns().setAll(txIdCol, txUserCol, txAmountCol, txTypeCol, txCreatedCol);
        // TODO: Initialize tables and load data
        // Do NOT call loadUsers(), loadRestaurants(), etc. here!
        // Dynamic loading for Sales tab
        adminTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null && "Sales".equals(newTab.getText())) {
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/SalesAnalytics.fxml"));
                    javafx.scene.Parent analyticsRoot = loader.load();
                    com.example.foodapp.controller.SalesAnalyticsController controller = loader.getController();
                    controller.setAdminMode(true);
                    controller.setJwtToken(jwtToken);
                    controller.setSellerId(-1); // Indicate admin mode for analytics
                    controller.setOnBack(() -> adminTabPane.getSelectionModel().select(0));
                    ((Tab) newTab).setContent(analyticsRoot);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    ((Tab) newTab).setContent(new Label("Failed to load analytics dashboard."));
                }
            }
        });
        if (foodsTable != null) {
            TableColumn<AdminFood, Integer> foodIdCol = new TableColumn<>("ID");
            foodIdCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
            TableColumn<AdminFood, String> foodNameCol = new TableColumn<>("Name");
            foodNameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
            TableColumn<AdminFood, String> foodDescCol = new TableColumn<>("Description");
            foodDescCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
            TableColumn<AdminFood, Integer> foodPriceCol = new TableColumn<>("Price");
            foodPriceCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getPrice()).asObject());
            TableColumn<AdminFood, Integer> foodDiscountCol = new TableColumn<>("Discount");
            foodDiscountCol.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getDiscountPrice()));
            TableColumn<AdminFood, Integer> foodSupplyCol = new TableColumn<>("Supply");
            foodSupplyCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getSupply()).asObject());
            TableColumn<AdminFood, Integer> foodVendorCol = new TableColumn<>("Vendor ID");
            foodVendorCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getVendorId()).asObject());
            foodsTable.getColumns().setAll(foodIdCol, foodNameCol, foodDescCol, foodPriceCol, foodDiscountCol, foodSupplyCol, foodVendorCol);
        }
    }

    private void loadUsers() {
        javafx.application.Platform.runLater(() -> usersTable.setPlaceholder(new Label("Loading...")));
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/admin/users");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch users: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                java.util.List<AdminUser> userList = new java.util.ArrayList<>();
                int idx = 0;
                while ((idx = json.indexOf("\"id\":", idx)) != -1) {
                    int id = Integer.parseInt(json.substring(idx + 5, json.indexOf(',', idx + 5)).replaceAll("[^0-9]", ""));
                    int nameIdx = json.indexOf("\"full_name\":", idx);
                    String name = nameIdx != -1 ? extractString(json, nameIdx + 12) : "";
                    int phoneIdx = json.indexOf("\"phone\":", idx);
                    String phone = phoneIdx != -1 ? extractString(json, phoneIdx + 8) : "";
                    int emailIdx = json.indexOf("\"email\":", idx);
                    String email = emailIdx != -1 ? extractString(json, emailIdx + 8) : "";
                    int roleIdx = json.indexOf("\"role\":", idx);
                    String role = roleIdx != -1 ? extractString(json, roleIdx + 7) : "";
                    int enabledIdx = json.indexOf("\"enabled\":", idx);
                    boolean enabled = enabledIdx != -1 && json.substring(enabledIdx + 10, json.indexOf(',', enabledIdx + 10)).contains("true");
                    userList.add(new AdminUser(id, name, phone, email, role, enabled));
                    idx = idx + 5;
                }
                javafx.application.Platform.runLater(() -> {
                    usersTable.setItems(FXCollections.observableArrayList(userList));
                    userCountLabel.setText(String.valueOf(userList.size()));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> usersTable.setPlaceholder(new Label("Failed to load users")));
            }
        }).start();
    }

    private String extractString(String json, int startIdx) {
        int quote1 = json.indexOf('"', startIdx);
        int quote2 = json.indexOf('"', quote1 + 1);
        if (quote1 != -1 && quote2 != -1 && quote2 > quote1) {
            return json.substring(quote1 + 1, quote2);
        }
        return "";
    }

    private void loadRestaurants() {
        javafx.application.Platform.runLater(() -> restaurantsTable.setPlaceholder(new Label("Loading...")));
        new Thread(() -> {
            try {
                com.example.foodapp.dao.RestaurantDao restaurantDao = new com.example.foodapp.dao.RestaurantDao();
                java.util.List<com.example.foodapp.model.entity.Restaurant> allRestaurants = restaurantDao.findAllAdmin();
                java.util.List<AdminRestaurant> restList = new java.util.ArrayList<>();
                for (com.example.foodapp.model.entity.Restaurant r : allRestaurants) {
                    restList.add(new AdminRestaurant(r.getId(), r.getName(), r.getAddress(), r.getPhone(), r.getOwnerId(), r.isEnabled()));
                }
                javafx.application.Platform.runLater(() -> {
                    restaurantsTable.setItems(FXCollections.observableArrayList(restList));
                    restaurantCountLabel.setText(String.valueOf(restList.size()));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> restaurantsTable.setPlaceholder(new Label("Failed to load restaurants")));
            }
        }).start();
    }
    private void loadOrders() {
        javafx.application.Platform.runLater(() -> ordersTable.setPlaceholder(new Label("Loading...")));
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/admin/orders");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch orders: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                java.util.List<AdminOrder> orderList = new java.util.ArrayList<>();
                int idx = 0;
                while ((idx = json.indexOf("\"id\":", idx)) != -1) {
                    int id = Integer.parseInt(json.substring(idx + 5, json.indexOf(',', idx + 5)).replaceAll("[^0-9]", ""));
                    int statusIdx = json.indexOf("\"status\":", idx);
                    String status = statusIdx != -1 ? extractString(json, statusIdx + 9) : "";
                    int custIdx = json.indexOf("\"customer_id\":", idx);
                    int customerId = custIdx != -1 ? Integer.parseInt(json.substring(custIdx + 13, json.indexOf(',', custIdx + 13)).replaceAll("[^0-9]", "")) : 0;
                    int vendIdx = json.indexOf("\"vendor_id\":", idx);
                    int vendorId = vendIdx != -1 ? Integer.parseInt(json.substring(vendIdx + 11, json.indexOf(',', vendIdx + 11)).replaceAll("[^0-9]", "")) : 0;
                    int priceIdx = json.indexOf("\"pay_price\":", idx);
                    int payPrice = priceIdx != -1 ? Integer.parseInt(json.substring(priceIdx + 11, json.indexOf(',', priceIdx + 11)).replaceAll("[^0-9]", "")) : 0;
                    int createdIdx = json.indexOf("\"created_at\":", idx);
                    String createdAt = createdIdx != -1 ? extractString(json, createdIdx + 12) : "";
                    orderList.add(new AdminOrder(id, status, customerId, vendorId, payPrice, createdAt));
                    idx = idx + 5;
                }
                javafx.application.Platform.runLater(() -> {
                    ordersTable.setItems(FXCollections.observableArrayList(orderList));
                    orderCountLabel.setText(String.valueOf(orderList.size()));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> ordersTable.setPlaceholder(new Label("Failed to load orders")));
            }
        }).start();
    }
    private void loadSales() {
        // TODO: Fetch sales data from backend and populate salesTable and totalSalesLabel
    }
    private void loadTransactions() {
        javafx.application.Platform.runLater(() -> transactionsTable.setPlaceholder(new Label("Loading...")));
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL("http://localhost:8000/admin/transactions");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("Failed to fetch transactions: " + code);
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream(), "UTF-8");
                String json = sc.useDelimiter("\\A").next();
                sc.close();
                java.util.List<AdminTransaction> txList = new java.util.ArrayList<>();
                int idx = 0;
                while ((idx = json.indexOf("\"id\":", idx)) != -1) {
                    int id = Integer.parseInt(json.substring(idx + 5, json.indexOf(',', idx + 5)).replaceAll("[^0-9]", ""));
                    int userIdx = json.indexOf("\"user_id\":", idx);
                    int userId = userIdx != -1 ? Integer.parseInt(json.substring(userIdx + 9, json.indexOf(',', userIdx + 9)).replaceAll("[^0-9]", "")) : 0;
                    int amountIdx = json.indexOf("\"amount\":", idx);
                    int amount = amountIdx != -1 ? Integer.parseInt(json.substring(amountIdx + 8, json.indexOf(',', amountIdx + 8)).replaceAll("[^0-9]", "")) : 0;
                    int typeIdx = json.indexOf("\"type\":", idx);
                    String type = typeIdx != -1 ? extractString(json, typeIdx + 6) : "";
                    int createdIdx = json.indexOf("\"created_at\":", idx);
                    String createdAt = createdIdx != -1 ? extractString(json, createdIdx + 12) : "";
                    txList.add(new AdminTransaction(id, userId, amount, type, createdAt));
                    idx = idx + 5;
                }
                javafx.application.Platform.runLater(() -> {
                    transactionsTable.setItems(FXCollections.observableArrayList(txList));
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> transactionsTable.setPlaceholder(new Label("Failed to load transactions")));
            }
        }).start();
    }
    private void loadAnalytics() {
        // TODO: Fetch analytics data from backend and populate analyticsTable
    }
    private void loadFoods() {
        foods.clear();
        try {
            com.example.foodapp.dao.FoodItemDao foodDao = new com.example.foodapp.dao.FoodItemDao();
            java.util.List<com.example.foodapp.model.entity.FoodItem> allFoods = foodDao.getAllFoodItems();
            for (com.example.foodapp.model.entity.FoodItem f : allFoods) {
                foods.add(new AdminFood(f.getId(), f.getName(), f.getDescription(), f.getPrice(), f.getDiscountPrice(), f.getSupply(), f.getVendorId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        foodsTable.setItems(foods);
    }
    private void handleDeleteUser() {
        AdminUser selected = (AdminUser) usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Delete user '" + selected.getName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        java.net.URL url = new java.net.URL("http://localhost:8000/admin/users/" + selected.getId());
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            loadUsers();
                        } else {
                            javafx.application.Platform.runLater(() -> {
                                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Failed to delete user.").showAndWait();
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        javafx.application.Platform.runLater(() -> {
                            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
                        });
                    }
                }).start();
            }
        });
    }

    private void handleDeleteRestaurant() {
        AdminRestaurant selected = (AdminRestaurant) restaurantsTable.getSelectionModel().getSelectedItem();
        System.out.println("[DEBUG] handleDeleteRestaurant called. Selected: " + (selected == null ? "null" : selected.getId() + " - " + selected.getName()));
        if (selected == null) return;
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Delete restaurant '" + selected.getName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        java.net.URL url = new java.net.URL("http://localhost:8000/admin/restaurants/" + selected.getId());
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            loadRestaurants();
                        } else {
                            javafx.application.Platform.runLater(() -> {
                                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Failed to delete restaurant.").showAndWait();
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        javafx.application.Platform.runLater(() -> {
                            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
                        });
                    }
                }).start();
            }
        });
    }

    private void handleDeleteOrder() {
        AdminOrder selected = (AdminOrder) ordersTable.getSelectionModel().getSelectedItem();
        System.out.println("[DEBUG] handleDeleteOrder called. Selected: " + (selected == null ? "null" : selected.getId() + " - " + selected.getStatus()));
        if (selected == null) return;
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION, "Delete order ID " + selected.getId() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirm Delete");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                new Thread(() -> {
                    try {
                        java.net.URL url = new java.net.URL("http://localhost:8000/admin/orders/" + selected.getId());
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("DELETE");
                        conn.setRequestProperty("Authorization", "Bearer " + jwtToken);
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            loadOrders();
                        } else {
                            javafx.application.Platform.runLater(() -> {
                                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Failed to delete order.").showAndWait();
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        javafx.application.Platform.runLater(() -> {
                            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
                        });
                    }
                }).start();
            }
        });
    }

    private void handleEnableUser() {
        AdminUser selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            com.example.foodapp.dao.UserDao userDao = new com.example.foodapp.dao.UserDao();
            userDao.enableUser(selected.getId());
            loadUsers();
        } catch (Exception e) {
            e.printStackTrace();
            // Optionally show an error dialog
        }
    }

    private void handleEnableRestaurant() {
        AdminRestaurant selected = restaurantsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            com.example.foodapp.dao.RestaurantDao restaurantDao = new com.example.foodapp.dao.RestaurantDao();
            restaurantDao.enableRestaurant(selected.getId());
            loadRestaurants();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleDeleteFood() {
        AdminFood selected = foodsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            com.example.foodapp.dao.FoodItemDao foodDao = new com.example.foodapp.dao.FoodItemDao();
            foodDao.deleteFoodItem(selected.getId());
            loadFoods();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 