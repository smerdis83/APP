package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.RatingDao;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.Rating;
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import com.example.foodapp.model.entity.OrderItem;

public class RatingHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final RatingDao ratingDao = new RatingDao();
    private final OrderDao orderDao = new OrderDao();

    public RatingHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // Require Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
            return;
        }
        Claims claims;
        try { claims = JwtUtil.parseToken(authHeader.substring("Bearer ".length()).trim()); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }
        int userId = Integer.parseInt(claims.getSubject());

        try {
            // POST /ratings
            if (method.equalsIgnoreCase("POST") && path.equals("/ratings")) {
                String postAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (postAuthHeader == null || !postAuthHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                    return;
                }
                Claims postClaims;
                try { postClaims = JwtUtil.parseToken(postAuthHeader.substring("Bearer ".length()).trim()); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }
                int postUserId = Integer.parseInt(postClaims.getSubject());
                Map<String, Object> req = mapper.readValue(exchange.getRequestBody(), Map.class);
                Integer orderId = (Integer) req.get("order_id");
                Integer ratingVal = (Integer) req.get("rating");
                String comment = (String) req.get("comment");
                List<String> imageBase64 = req.get("imageBase64") != null ? (List<String>) req.get("imageBase64") : null;
                if (orderId == null || ratingVal == null || comment == null) {
                    sendJson(exchange, 400, Map.of("error", "Missing required fields"));
                    return;
                }
                // Validate order ownership and status
                OrderDao orderDao = new OrderDao();
                Order order;
                try { order = orderDao.getOrderById(orderId); } catch (Exception e) { sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage())); return; }
                if (order == null) { sendJson(exchange, 404, Map.of("error", "Order not found")); return; }
                if (order.getCustomerId() != postUserId) { sendJson(exchange, 403, Map.of("error", "Forbidden: not your order")); return; }
                if (!"delivered".equalsIgnoreCase(order.getStatus())) { sendJson(exchange, 400, Map.of("error", "Order not delivered yet")); return; }
                // Add order-level rating
                Rating rating = new Rating();
                rating.setOrderId(orderId);
                rating.setUserId(postUserId);
                rating.setRating(ratingVal);
                rating.setComment(comment);
                rating.setImageBase64(imageBase64);
                try {
                    ratingDao.addRating(rating);
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                    return;
                }
                // Add item-level ratings for each item in the order
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        Rating itemRating = new Rating();
                        itemRating.setOrderId(orderId);
                        itemRating.setUserId(postUserId);
                        itemRating.setRating(ratingVal);
                        itemRating.setComment(comment);
                        itemRating.setImageBase64(imageBase64);
                        itemRating.setItemId(item.getItem_id());
                        try { ratingDao.addItemRating(itemRating); } catch (Exception ignore) {}
                    }
                }
                sendJson(exchange, 200, rating);
                return;
            }
            // --- GET /ratings/items/{item_id} ---
            else if (method.equalsIgnoreCase("GET") && path.matches("/ratings/items/\\d+")) {
                int itemId = extractIdFromPath(path, "/ratings/items/");
                String itemAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (itemAuthHeader == null || !itemAuthHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                    return;
                }
                try {
                    List<Rating> ratings = ratingDao.getRatingsByItem(itemId);
                    double avg = ratings.isEmpty() ? 0 : ratings.stream().mapToInt(Rating::getRating).average().orElse(0);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("avg_rating", avg);
                    resp.put("comments", ratings);
                    sendJson(exchange, 200, resp);
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            }
            // --- GET /ratings/{id} ---
            else if (method.equalsIgnoreCase("GET") && path.matches("/ratings/\\d+")) {
                int ratingId = extractIdFromPath(path, "/ratings/");
                String ratingAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (ratingAuthHeader == null || !ratingAuthHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                    return;
                }
                try {
                    Rating rating = ratingDao.getRatingById(ratingId);
                    if (rating == null) {
                        sendJson(exchange, 404, Map.of("error", "Rating not found"));
                        return;
                    }
                    sendJson(exchange, 200, rating);
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            }
            // --- PUT /ratings/{id} ---
            else if (method.equalsIgnoreCase("PUT") && path.matches("/ratings/\\d+")) {
                int ratingId = extractIdFromPath(path, "/ratings/");
                String putAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (putAuthHeader == null || !putAuthHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                    return;
                }
                Claims putClaims;
                try { putClaims = JwtUtil.parseToken(putAuthHeader.substring("Bearer ".length()).trim()); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }
                int putUserId = Integer.parseInt(putClaims.getSubject());
                try {
                    Rating rating = ratingDao.getRatingById(ratingId);
                    if (rating == null) { sendJson(exchange, 404, Map.of("error", "Rating not found")); return; }
                    if (rating.getUserId() != putUserId) { sendJson(exchange, 403, Map.of("error", "Forbidden: not your rating")); return; }
                    Map<String, Object> req = mapper.readValue(exchange.getRequestBody(), Map.class);
                    if (req.containsKey("rating")) rating.setRating((Integer) req.get("rating"));
                    if (req.containsKey("comment")) rating.setComment((String) req.get("comment"));
                    if (req.containsKey("imageBase64")) rating.setImageBase64((List<String>) req.get("imageBase64"));
                    ratingDao.updateRating(rating);
                    sendJson(exchange, 200, rating);
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            }
            // --- DELETE /ratings/{id} ---
            else if (method.equalsIgnoreCase("DELETE") && path.matches("/ratings/\\d+")) {
                int ratingId = extractIdFromPath(path, "/ratings/");
                String deleteAuthHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (deleteAuthHeader == null || !deleteAuthHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                    return;
                }
                Claims deleteClaims;
                try { deleteClaims = JwtUtil.parseToken(deleteAuthHeader.substring("Bearer ".length()).trim()); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }
                int deleteUserId = Integer.parseInt(deleteClaims.getSubject());
                try {
                    Rating rating = ratingDao.getRatingById(ratingId);
                    if (rating == null) { sendJson(exchange, 404, Map.of("error", "Rating not found")); return; }
                    if (rating.getUserId() != deleteUserId) { sendJson(exchange, 403, Map.of("error", "Forbidden: not your rating")); return; }
                    ratingDao.deleteRating(ratingId);
                    sendJson(exchange, 200, Map.of("message", "Rating deleted"));
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            }
            // --- GET /ratings/restaurant/{restaurantId} ---
            else if (method.equalsIgnoreCase("GET") && path.matches("/ratings/restaurant/\\d+")) {
                int restaurantId = Integer.parseInt(path.replaceAll(".*?/ratings/restaurant/(\\d+)", "$1"));
                OrderDao orderDao = new OrderDao();
                RatingDao ratingDao = new RatingDao();
                try {
                    List<com.example.foodapp.model.entity.Order> orders = orderDao.getOrdersByVendor(restaurantId);
                    List<com.example.foodapp.model.entity.Rating> allRatings = new java.util.ArrayList<>();
                    for (com.example.foodapp.model.entity.Order order : orders) {
                        // Only get order-level ratings (item_id is NULL) to avoid duplicates
                        List<com.example.foodapp.model.entity.Rating> ratings = ratingDao.getOrderLevelRatingsByOrder(order.getId());
                        allRatings.addAll(ratings);
                    }
                    double avg = allRatings.isEmpty() ? 0 : allRatings.stream().mapToInt(com.example.foodapp.model.entity.Rating::getRating).average().orElse(0);
                    java.util.Map<String, Object> resp = new java.util.HashMap<>();
                    resp.put("avg_rating", avg);
                    resp.put("comments", allRatings);
                    sendJson(exchange, 200, resp);
                } catch (Exception e) {
                    sendJson(exchange, 500, java.util.Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            }
            // --- GET /ratings/order/{orderId} ---
            else if (method.equalsIgnoreCase("GET") && path.matches("/ratings/order/\\d+")) {
                int orderId = Integer.parseInt(path.replaceAll(".*?/ratings/order/(\\d+)", "$1"));
                try {
                    Rating rating = ratingDao.getRatingByOrderAndUser(orderId, userId);
                    if (rating == null) {
                        sendJson(exchange, 404, Map.of("error", "No rating found"));
                    } else {
                        sendJson(exchange, 200, rating);
                    }
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
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
        String idStr = path.substring(prefix.length());
        return Integer.parseInt(idStr);
    }
} 