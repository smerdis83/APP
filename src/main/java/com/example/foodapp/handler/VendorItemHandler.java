package com.example.foodapp.handler;

import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.dao.MenuDao;
import com.example.foodapp.dao.RestaurantDao;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.model.entity.Menu;
import com.example.foodapp.model.entity.Restaurant;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VendorItemHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final RestaurantDao restaurantDao = new RestaurantDao();
    private final MenuDao menuDao = new MenuDao();
    private final FoodItemDao foodItemDao = new FoodItemDao();

    public VendorItemHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // Require Authorization header for all endpoints
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
            return;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        Claims claims;
        try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }

        try {
            // POST /vendors (list vendors with filters)
            if (method.equalsIgnoreCase("POST") && "/vendors".equals(path)) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> req = json.isEmpty() ? new HashMap<>() : mapper.readValue(json, Map.class);
                String search = req.getOrDefault("search", "").toString().toLowerCase();
                List<String> keywords = req.containsKey("keywords") && req.get("keywords") instanceof List ? (List<String>) req.get("keywords") : Collections.emptyList();
                List<Restaurant> all = restaurantDao.findAll();
                List<Restaurant> filtered = new ArrayList<>();
                for (Restaurant r : all) {
                    boolean matches = true;
                    if (!search.isEmpty() && !(r.getName().toLowerCase().contains(search) || r.getAddress().toLowerCase().contains(search))) {
                        matches = false;
                    }
                    if (!keywords.isEmpty()) {
                        // Check if any menu or item matches keywords
                        try {
                            List<Menu> menus = menuDao.getMenusByRestaurant(r.getId());
                            boolean found = false;
                            for (Menu m : menus) {
                                for (Integer itemId : m.getItemIds()) {
                                    FoodItem item = foodItemDao.getFoodItemById(itemId);
                                    if (item != null && item.getKeywords() != null) {
                                        for (String kw : keywords) {
                                            if (item.getKeywords().contains(kw)) { found = true; break; }
                                        }
                                    }
                                    if (found) break;
                                }
                                if (found) break;
                            }
                            if (!found) matches = false;
                        } catch (Exception ignore) {}
                    }
                    if (matches) filtered.add(r);
                }
                sendJson(exchange, 200, filtered);
                return;
            }
            // GET /vendors/{id} (view menu items for a vendor)
            else if (method.equalsIgnoreCase("GET") && path.matches("/vendors/\\d+")) {
                int vendorId = extractIdFromPath(path, "/vendors/");
                Restaurant vendor = restaurantDao.findById(vendorId);
                if (vendor == null) { sendJson(exchange, 404, Map.of("error", "Vendor not found")); return; }
                List<Menu> menus = menuDao.getMenusByRestaurant(vendorId);
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("vendor", vendor);
                List<String> menuTitles = new ArrayList<>();
                for (Menu m : menus) menuTitles.add(m.getTitle());
                resp.put("menu_titles", menuTitles);
                for (Menu m : menus) {
                    List<FoodItem> items = new ArrayList<>();
                    for (Integer itemId : m.getItemIds()) {
                        try {
                            FoodItem item = foodItemDao.getFoodItemById(itemId);
                            if (item != null) items.add(item);
                        } catch (Exception ignore) {}
                    }
                    resp.put(m.getTitle(), items);
                }
                sendJson(exchange, 200, resp);
                return;
            }
            // POST /items (list items with filters)
            else if (method.equalsIgnoreCase("POST") && "/items".equals(path)) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> req = json.isEmpty() ? new HashMap<>() : mapper.readValue(json, Map.class);
                String search = req.getOrDefault("search", "").toString().toLowerCase();
                int price = req.containsKey("price") ? (int) req.get("price") : -1;
                List<String> keywords = req.containsKey("keywords") && req.get("keywords") instanceof List ? (List<String>) req.get("keywords") : Collections.emptyList();
                List<FoodItem> all = foodItemDao.getAllFoodItems();
                List<FoodItem> filtered = new ArrayList<>();
                for (FoodItem item : all) {
                    boolean matches = true;
                    if (!search.isEmpty() && !(item.getName().toLowerCase().contains(search) || item.getDescription().toLowerCase().contains(search))) {
                        matches = false;
                    }
                    if (price >= 0 && item.getPrice() != price) {
                        matches = false;
                    }
                    if (!keywords.isEmpty() && item.getKeywords() != null) {
                        boolean found = false;
                        for (String kw : keywords) {
                            if (item.getKeywords().contains(kw)) { found = true; break; }
                        }
                        if (!found) matches = false;
                    }
                    if (matches) filtered.add(item);
                }
                sendJson(exchange, 200, filtered);
                return;
            }
            // GET /items/{id} (get item details)
            else if (method.equalsIgnoreCase("GET") && path.matches("/items/\\d+")) {
                int itemId = extractIdFromPath(path, "/items/");
                FoodItem item = foodItemDao.getFoodItemById(itemId);
                if (item == null) { sendJson(exchange, 404, Map.of("error", "Item not found")); return; }
                sendJson(exchange, 200, item);
                return;
            }
            else {
                sendJson(exchange, 404, Map.of("error", "Not Found"));
            }
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        byte[] responseBytes = mapper.writeValueAsBytes(payload);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private int extractIdFromPath(String path, String prefix) {
        String temp = path.substring(path.indexOf(prefix) + prefix.length());
        return Integer.parseInt(temp.replaceAll("[^0-9]", ""));
    }
} 