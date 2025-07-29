package com.example.foodapp.handler;

import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.dao.RestaurantDao;
import com.example.foodapp.dao.MenuDao;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.model.entity.Restaurant;
import com.example.foodapp.model.entity.Menu;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.example.foodapp.model.entity.Order;
import java.net.URLDecoder;

public class RestaurantHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final RestaurantDao restaurantDao = new RestaurantDao();
    private final FoodItemDao foodItemDao = new FoodItemDao();
    private final MenuDao menuDao = new MenuDao();

    public RestaurantHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath();     // e.g. "/restaurants" or "/restaurants/mine"
        String method = exchange.getRequestMethod();             // GET or POST

        System.out.println("\n>>> RESTAURANT REQUEST >>>");
        System.out.println("Path   : " + path);
        System.out.println("Method : " + method);
        System.out.println("Headers: ");
        exchange.getRequestHeaders().forEach((k, v) -> System.out.println("  " + k + ": " + v));
        System.out.println("<<< END >>>\n");

        // Always respond JSON
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        try {
            if ("GET".equalsIgnoreCase(method) && "/restaurants".equals(path)) {
                // 1) Public: list all restaurants (buyer view)
                List<Restaurant> all = restaurantDao.findAll();
                sendJson(exchange, 200, all);

            } else if ("GET".equalsIgnoreCase(method) && "/restaurants/mine".equals(path)) {
                // 2) Protected: list only the authenticated seller's restaurants
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try {
                    claims = JwtUtil.parseToken(token);
                } catch (ExpiredJwtException e) {
                    sendJson(exchange, 401, new ErrorResponse("Token expired"));
                    return;
                } catch (SignatureException | MalformedJwtException e) {
                    sendJson(exchange, 401, new ErrorResponse("Invalid token"));
                    return;
                } catch (Exception e) {
                    sendJson(exchange, 500, new ErrorResponse("Server error"));
                    return;
                }

                int userId;
                try {
                    userId = Integer.parseInt(claims.getSubject());
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, new ErrorResponse("Invalid user ID in token"));
                    return;
                }

                // Only SELLERs can create/view their own restaurants
                String role = claims.get("role", String.class);
                if (!"SELLER".equals(role)) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller"));
                    return;
                }

                List<Restaurant> mine = restaurantDao.findByOwner(userId);
                sendJson(exchange, 200, mine);

            } else if ("GET".equalsIgnoreCase(method) && path.matches("/restaurants/\\d+$")) {
                // 2.1) Protected: get individual restaurant details
                int restaurantId = extractIdFromPath(path, "/restaurants/");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try {
                    claims = JwtUtil.parseToken(token);
                } catch (ExpiredJwtException e) {
                    sendJson(exchange, 401, new ErrorResponse("Token expired"));
                    return;
                } catch (SignatureException | MalformedJwtException e) {
                    sendJson(exchange, 401, new ErrorResponse("Invalid token"));
                    return;
                } catch (Exception e) {
                    sendJson(exchange, 500, new ErrorResponse("Server error"));
                    return;
                }

                int userId;
                try {
                    userId = Integer.parseInt(claims.getSubject());
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, new ErrorResponse("Invalid user ID in token"));
                    return;
                }

                // Allow SELLER, BUYER, and ADMIN to view restaurant details
                String role = claims.get("role", String.class);
                if (!"SELLER".equals(role) && !"BUYER".equals(role) && !"ADMIN".equals(role)) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller, buyer, or admin"));
                    return;
                }

                Restaurant restaurant = null;
                try {
                    restaurant = restaurantDao.findById(restaurantId);
                } catch (Exception e) {
                    sendJson(exchange, 500, new ErrorResponse("Database error"));
                    return;
                }

                if (restaurant == null) {
                    sendJson(exchange, 404, new ErrorResponse("Restaurant not found"));
                    return;
                }

                // Only check ownership for SELLERs
                if ("SELLER".equals(role) && restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }

                sendJson(exchange, 200, restaurant);

            } else if ("POST".equalsIgnoreCase(method) && "/restaurants".equals(path)) {
                // 3) Protected: seller creates a new restaurant
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try {
                    claims = JwtUtil.parseToken(token);
                } catch (ExpiredJwtException e) {
                    sendJson(exchange, 401, new ErrorResponse("Token expired"));
                    return;
                } catch (SignatureException | MalformedJwtException e) {
                    sendJson(exchange, 401, new ErrorResponse("Invalid token"));
                    return;
                } catch (Exception e) {
                    sendJson(exchange, 500, new ErrorResponse("Server error"));
                    return;
                }

                int userId;
                try {
                    userId = Integer.parseInt(claims.getSubject());
                } catch (NumberFormatException e) {
                    sendJson(exchange, 400, new ErrorResponse("Invalid user ID in token"));
                    return;
                }

                // Only SELLERs can create restaurants
                String role = claims.get("role", String.class);
                if (!"SELLER".equals(role)) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller"));
                    return;
                }

                // Read request body (JSON) into a Restaurant object
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                String json = sb.toString();
                Restaurant createReq = mapper.readValue(json, Restaurant.class);

                // Set the owner_id to the authenticated seller
                createReq.setOwnerId(userId);

                // Defensive: Only save logoBase64 if it's a valid Base64 string
                String logo = createReq.getLogoBase64();
                if (logo != null && (logo.length() < 20 || logo.startsWith("[") || logo.startsWith("{"))) {
                    createReq.setLogoBase64(null);
                }
                System.out.println("[DEBUG] logoBase64 in handler: " + (createReq.getLogoBase64() == null ? "null" : createReq.getLogoBase64().substring(0, Math.min(40, createReq.getLogoBase64().length()))));

                // Insert into DB
                try {
                    restaurantDao.createRestaurant(createReq);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
                    return;
                }

                // Return 201 Created with the new restaurant in body
                sendJson(exchange, 201, createReq);

            } else if (method.equalsIgnoreCase("POST") && path.matches("/restaurants/\\d+/item")) {
                // Add food item to restaurant
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/item");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // TODO: Optionally check if userId owns restaurantId
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                FoodItem item = mapper.readValue(json, FoodItem.class);
                item.setVendorId(restaurantId);
                try { foodItemDao.addFoodItem(item); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 201, item);
                return;
            } else if (method.equalsIgnoreCase("PUT") && path.matches("/restaurants/\\d+/item/\\d+")) {
                // Edit food item
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/item/");
                int itemId = extractIdFromPath(path, "/item/");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // TODO: Optionally check if userId owns restaurantId
                FoodItem item;
                try { item = foodItemDao.getFoodItemById(itemId); } catch (Exception e) { sendJson(exchange, 404, new ErrorResponse("Item not found")); return; }
                if (item == null || item.getVendorId() != restaurantId) { sendJson(exchange, 404, new ErrorResponse("Item not found for this restaurant")); return; }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                FoodItem update = mapper.readValue(json, FoodItem.class);
                item.setName(update.getName());
                item.setDescription(update.getDescription());
                item.setPrice(update.getPrice());
                item.setSupply(update.getSupply());
                item.setKeywords(update.getKeywords());
                item.setImageBase64(update.getImageBase64());
                try { foodItemDao.updateFoodItem(item); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, item);
                return;
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/restaurants/\\d+/item/\\d+")) {
                // Delete food item
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/item/");
                int itemId = extractIdFromPath(path, "/item/");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // TODO: Optionally check if userId owns restaurantId
                FoodItem item;
                try { item = foodItemDao.getFoodItemById(itemId); } catch (Exception e) { sendJson(exchange, 404, new ErrorResponse("Item not found")); return; }
                if (item == null || item.getVendorId() != restaurantId) { sendJson(exchange, 404, new ErrorResponse("Item not found for this restaurant")); return; }
                try { foodItemDao.deleteFoodItem(itemId); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, Map.of("message", "Item deleted successfully"));
                return;
            } else if (method.equalsIgnoreCase("POST") && path.matches("/restaurants/\\d+/menu")) {
                // Create a new menu for a restaurant
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/menu");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // TODO: Optionally check if userId owns restaurantId
                Restaurant restaurant = null;
                try { restaurant = restaurantDao.findById(restaurantId); } catch (Exception e) { /* ignore */ }
                if (restaurant == null || restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Menu menu = mapper.readValue(json, Menu.class);
                menu.setRestaurantId(restaurantId);
                try { menuDao.addMenu(menu); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 201, menu);
                return;
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/restaurants/\\d+/menu/.+/\\d+")) {
                // Remove item from a menu
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/menu/");
                String rest = path.substring(path.indexOf("/menu/") + 6);
                String[] parts = rest.split("/");
                String title = parts[0];
                title = URLDecoder.decode(title, StandardCharsets.UTF_8);
                int itemId = Integer.parseInt(parts[1]);
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // TODO: Optionally check if userId owns restaurantId
                try { menuDao.removeItemFromMenu(restaurantId, title, itemId); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, Map.of("message", "Item removed from menu"));
                return;
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/restaurants/\\d+/menu/.+")) {
                // Delete a menu by title
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/menu/");
                String title = path.substring(path.indexOf("/menu/") + 6);
                title = URLDecoder.decode(title, StandardCharsets.UTF_8);
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // TODO: Optionally check if userId owns restaurantId
                Restaurant restaurant = null;
                try { restaurant = restaurantDao.findById(restaurantId); } catch (Exception e) { /* ignore */ }
                if (restaurant == null || restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }
                try { menuDao.deleteMenu(restaurantId, title); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, Map.of("message", "Menu deleted successfully"));
                return;
            } else if (method.equalsIgnoreCase("PUT") && path.matches("/restaurants/\\d+/menu/.+")) {
                // Add item to a menu
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/menu/");
                String title = path.substring(path.indexOf("/menu/") + 6);
                title = URLDecoder.decode(title, StandardCharsets.UTF_8);
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // TODO: Optionally check if userId owns restaurantId
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> req = mapper.readValue(json, Map.class);
                if (!req.containsKey("item_id")) { sendJson(exchange, 400, new ErrorResponse("Missing item_id")); return; }
                int itemId = (int) req.get("item_id");
                // Validate item exists
                FoodItem foodItem = null;
                try { foodItem = foodItemDao.getFoodItemById(itemId); } catch (Exception e) { /* ignore */ }
                if (foodItem == null) {
                    sendJson(exchange, 404, new ErrorResponse("Item not found"));
                    return;
                }
                // Check if menu exists
                Menu menuCheck = null;
                try { menuCheck = menuDao.getMenu(restaurantId, title); } catch (Exception e) { /* ignore */ }
                if (menuCheck == null) {
                    sendJson(exchange, 404, new ErrorResponse("Menu not found"));
                    return;
                }
                try { menuDao.addItemToMenu(restaurantId, title, itemId); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, Map.of("message", "Item added to menu"));
                return;
            }
            // --- NEW: Public GET /restaurants/{restaurantId}/menus ---
            else if ("GET".equalsIgnoreCase(method) && path.matches("/restaurants/\\d+/menus")) {
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/menus");
                List<Menu> menus;
                try {
                    menus = menuDao.getMenusByRestaurant(restaurantId);
                } catch (Exception e) {
                    sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
                    return;
                }
                sendJson(exchange, 200, menus);
                return;
            }
            // --- NEW: Public GET /restaurants/{restaurantId}/menus/{menuTitle}/items ---
            else if ("GET".equalsIgnoreCase(method) && path.matches("/restaurants/\\d+/menus/.+/items")) {
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/menus/");
                String rest = path.substring(path.indexOf("/menus/") + 7); // after /menus/
                String[] parts = rest.split("/items");
                String menuTitle = parts[0];
                menuTitle = URLDecoder.decode(menuTitle, StandardCharsets.UTF_8);
                Menu menu;
                try {
                    menu = menuDao.getMenu(restaurantId, menuTitle);
                } catch (Exception e) {
                    sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
                    return;
                }
                if (menu == null) {
                    sendJson(exchange, 404, new ErrorResponse("Menu not found"));
                    return;
                }
                List<FoodItem> items = new java.util.ArrayList<>();
                for (Integer itemId : menu.getItemIds()) {
                    try {
                        FoodItem item = foodItemDao.getFoodItemById(itemId);
                        if (item != null) items.add(item);
                    } catch (Exception e) {
                        // skip missing/broken items
                    }
                }
                sendJson(exchange, 200, items);
                return;
            }
            // --- NEW: Seller GET /restaurants/{restaurant_id}/orders ---
            else if ("GET".equalsIgnoreCase(method) && path.matches("/restaurants/\\d+/orders")) {
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/orders");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // Check ownership
                Restaurant restaurant = null;
                try { restaurant = restaurantDao.findById(restaurantId); } catch (Exception e) { /* ignore */ }
                if (restaurant == null || restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }
                List<Order> orders;
                try { orders = new com.example.foodapp.dao.OrderDao().getOrdersByVendor(restaurantId); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, orders);
                return;
            }
            // --- NEW: Seller PATCH /restaurants/orders/{order_id} ---
            else if ("PATCH".equalsIgnoreCase(method) && path.matches("/restaurants/orders/\\d+")) {
                int orderId = extractIdFromPath(path, "/restaurants/orders/");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // Find order and check ownership
                Order order;
                try { order = new com.example.foodapp.dao.OrderDao().getOrderById(orderId); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                if (order == null) { sendJson(exchange, 404, new ErrorResponse("Order not found")); return; }
                Restaurant restaurant = null;
                try { restaurant = restaurantDao.findById(order.getVendorId()); } catch (Exception e) { /* ignore */ }
                if (restaurant == null || restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }
                // Parse status from request body
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> req = mapper.readValue(json, Map.class);
                if (!req.containsKey("status")) { sendJson(exchange, 400, new ErrorResponse("Missing status")); return; }
                String newStatus = req.get("status").toString();
                // Optionally: validate allowed status transitions here
                if (!("accepted".equals(newStatus) || "rejected".equals(newStatus) || "served".equals(newStatus))) {
                    sendJson(exchange, 400, new ErrorResponse("Invalid status: must be one of 'accepted', 'rejected', or 'served'"));
                    return;
                }
                // Only allow accepting if order is waiting vendor (i.e., paid)
                if ("accepted".equals(newStatus) && !"waiting vendor".equals(order.getStatus())) {
                    sendJson(exchange, 409, new ErrorResponse("Order must be paid before it can be accepted"));
                    return;
                }
                // Add order pay price to seller's wallet when accepted
                if ("accepted".equals(newStatus)) {
                    try {
                        com.example.foodapp.dao.UserDao userDao = new com.example.foodapp.dao.UserDao();
                        int currentBalance = userDao.getWalletBalance(userId);
                        int customerPaidAmount = order.getPayPrice();
                        
                        System.out.println("[DEBUG] Seller accepting order: Order ID=" + orderId + ", Seller ID=" + userId);
                        System.out.println("[DEBUG] Customer paid amount: " + customerPaidAmount);
                        System.out.println("[DEBUG] Seller wallet: Before=" + currentBalance + ", After=" + (currentBalance + customerPaidAmount));
                        
                        userDao.updateWalletBalance(userId, currentBalance + customerPaidAmount);
                        
                        System.out.println("[DEBUG] Seller wallet updated successfully!");
                    } catch (Exception e) {
                        System.err.println("[ERROR] Failed to update seller wallet: " + e.getMessage());
                        e.printStackTrace();
                        sendJson(exchange, 500, new ErrorResponse("Failed to update seller wallet: " + e.getMessage()));
                        return;
                    }
                }
                // Only allow serving if order is accepted
                if ("served".equals(newStatus) && !"accepted".equals(order.getStatus())) {
                    sendJson(exchange, 409, new ErrorResponse("Order must be accepted before it can be served"));
                    return;
                }
                order.setStatus(newStatus);
                try {
                    new com.example.foodapp.dao.OrderDao().updateOrderStatus(orderId, newStatus);
                    new com.example.foodapp.dao.OrderDao().insertOrderStatusHistory(orderId, newStatus, "seller");
                } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, Map.of("message", "Order status updated", "order_id", orderId, "new_status", newStatus));
                return;
            }
            // --- NEW: Seller PUT /restaurants/{id} ---
            else if ("PUT".equalsIgnoreCase(method) && path.matches("/restaurants/\\d+")) {
                int restaurantId = extractIdFromPath(path, "/restaurants/");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // Check ownership
                Restaurant restaurant = null;
                try { restaurant = restaurantDao.findById(restaurantId); } catch (Exception e) { /* ignore */ }
                if (restaurant == null) {
                    sendJson(exchange, 404, new ErrorResponse("Restaurant not found"));
                    return;
                }
                if (restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
                    sendJson(exchange, 415, new ErrorResponse("Unsupported Media Type: Content-Type must be application/json"));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> updateReq;
                try { updateReq = mapper.readValue(json, Map.class); } catch (Exception e) { sendJson(exchange, 400, new ErrorResponse("Invalid input: " + e.getMessage())); return; }
                if (updateReq.isEmpty()) {
                    sendJson(exchange, 400, new ErrorResponse("No updatable fields provided"));
                    return;
                }
                // Update fields if present
                if (updateReq.containsKey("name")) restaurant.setName((String) updateReq.get("name"));
                if (updateReq.containsKey("address")) restaurant.setAddress((String) updateReq.get("address"));
                if (updateReq.containsKey("phone")) restaurant.setPhone((String) updateReq.get("phone"));
                if (updateReq.containsKey("logoBase64")) restaurant.setLogoBase64((String) updateReq.get("logoBase64"));
                if (updateReq.containsKey("tax_fee")) restaurant.setTaxFee((Integer) updateReq.get("tax_fee"));
                if (updateReq.containsKey("additional_fee")) restaurant.setAdditionalFee((Integer) updateReq.get("additional_fee"));
                if (updateReq.containsKey("description")) restaurant.setDescription(updateReq.get("description") == null ? "" : ((String) updateReq.get("description")).trim());
                if (updateReq.containsKey("working_hours")) restaurant.setWorkingHours(updateReq.get("working_hours") == null ? "" : ((String) updateReq.get("working_hours")).trim());
                try { restaurantDao.updateRestaurant(restaurant); } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                        sendJson(exchange, 409, new ErrorResponse("Duplicate restaurant info"));
                    } else {
                        sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
                    }
                    return;
                }
                sendJson(exchange, 200, restaurant);
                return;
            }
            // --- YAML-COMPLIANT: Seller PUT /restaurants/{id}/menu/{title} (add item to menu only) ---
            else if ("PUT".equalsIgnoreCase(method) && path.matches("/restaurants/\\d+/menu/.+")) {
                int restaurantId = extractIdFromPath(path, "/restaurants/", "/menu/");
                String title = path.substring(path.indexOf("/menu/") + 6);
                title = URLDecoder.decode(title, StandardCharsets.UTF_8);
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // Check ownership
                Restaurant restaurant = null;
                try { restaurant = restaurantDao.findById(restaurantId); } catch (Exception e) { /* ignore */ }
                if (restaurant == null) {
                    sendJson(exchange, 404, new ErrorResponse("Restaurant not found"));
                    return;
                }
                if (restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }
                Menu menu = null;
                try { menu = menuDao.getMenu(restaurantId, title); } catch (Exception e) { /* ignore */ }
                if (menu == null) {
                    sendJson(exchange, 404, new ErrorResponse("Menu not found"));
                    return;
                }
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
                    sendJson(exchange, 415, new ErrorResponse("Unsupported Media Type: Content-Type must be application/json"));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> req;
                try { req = mapper.readValue(json, Map.class); } catch (Exception e) { sendJson(exchange, 400, new ErrorResponse("Invalid input: " + e.getMessage())); return; }
                if (!req.containsKey("item_id")) { sendJson(exchange, 400, new ErrorResponse("Missing item_id")); return; }
                int itemId = (int) req.get("item_id");
                // Validate item exists
                FoodItem foodItem = null;
                try { foodItem = foodItemDao.getFoodItemById(itemId); } catch (Exception e) { /* ignore */ }
                if (foodItem == null) {
                    sendJson(exchange, 404, new ErrorResponse("Item not found"));
                    return;
                }
                // Check if menu exists (already done above)
                try { menuDao.addItemToMenu(restaurantId, title, itemId); } catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage())); return; }
                sendJson(exchange, 200, Map.of("message", "Item added to menu"));
                return;
            }
            // --- NEW: Seller DELETE /restaurants/{id} ---
            else if ("DELETE".equalsIgnoreCase(method) && path.matches("/restaurants/\\d+")) {
                int restaurantId = extractIdFromPath(path, "/restaurants/");
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                Claims claims;
                try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
                String role = claims.get("role", String.class);
                int userId = Integer.parseInt(claims.getSubject());
                if (!"SELLER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a seller")); return; }
                // Check ownership
                Restaurant restaurant = null;
                try { restaurant = restaurantDao.findById(restaurantId); } catch (Exception e) { /* ignore */ }
                if (restaurant == null) {
                    sendJson(exchange, 404, new ErrorResponse("Restaurant not found"));
                    return;
                }
                if (restaurant.getOwnerId() != userId) {
                    sendJson(exchange, 403, new ErrorResponse("Forbidden: you do not own this restaurant"));
                    return;
                }
                try { restaurantDao.deleteRestaurant(restaurantId); } catch (Exception e) {
                    sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
                    return;
                }
                sendJson(exchange, 200, Map.of("message", "Restaurant deleted successfully"));
                return;
            }
            else {
                // Path/method not matched
                sendJson(exchange, 404, new ErrorResponse("Not Found"));
            }

        } catch (Exception e) {
            // Catch-all for unexpected errors
            e.printStackTrace();
            sendJson(exchange, 500, new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    // Helper to send any Java object as JSON
    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        String json = mapper.writeValueAsString(payload);
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // Simple DTO for error messages
    static class ErrorResponse {
        private final String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }

    // Helper to extract IDs from path
    private int extractIdFromPath(String path, String prefix, String suffix) {
        String temp = path.substring(prefix.length());
        if (suffix != null && !suffix.isEmpty()) {
            temp = temp.substring(0, temp.indexOf(suffix));
        }
        return Integer.parseInt(temp.replaceAll("[^0-9]", ""));
    }
    private int extractIdFromPath(String path, String prefix) {
        String temp = path.substring(path.indexOf(prefix) + prefix.length());
        return Integer.parseInt(temp.replaceAll("[^0-9]", ""));
    }
} 