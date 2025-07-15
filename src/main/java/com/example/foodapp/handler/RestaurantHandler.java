package com.example.foodapp.handler;

import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.dao.RestaurantDao;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.model.entity.Restaurant;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class RestaurantHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final RestaurantDao restaurantDao = new RestaurantDao();
    private final FoodItemDao foodItemDao = new FoodItemDao();

    public RestaurantHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
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