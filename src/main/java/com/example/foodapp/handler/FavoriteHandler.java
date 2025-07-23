package com.example.foodapp.handler;

import com.example.foodapp.dao.FavoriteDao;
import com.example.foodapp.dao.RestaurantDao;
import com.example.foodapp.model.entity.Restaurant;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FavoriteHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final FavoriteDao favoriteDao = new FavoriteDao();
    private final RestaurantDao restaurantDao = new RestaurantDao();

    public FavoriteHandler() {
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
        String token = authHeader.substring("Bearer ".length()).trim();
        Claims claims;
        try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }
        int userId = Integer.parseInt(claims.getSubject());

        try {
            // GET /favorites
            if (method.equalsIgnoreCase("GET") && "/favorites".equals(path)) {
                List<Integer> favIds = favoriteDao.getFavoritesByUser(userId);
                List<Restaurant> favs = new ArrayList<>();
                for (int rid : favIds) {
                    try {
                        Restaurant r = restaurantDao.findById(rid);
                        if (r != null) favs.add(r);
                    } catch (Exception ignore) {}
                }
                sendJson(exchange, 200, favs);
                return;
            }
            // PUT /favorites/{restaurantId}
            else if (method.equalsIgnoreCase("PUT") && path.matches("/favorites/\\d+")) {
                int restaurantId = extractIdFromPath(path, "/favorites/");
                Restaurant r = restaurantDao.findById(restaurantId);
                if (r == null) { sendJson(exchange, 404, Map.of("error", "Restaurant not found")); return; }
                favoriteDao.addFavorite(userId, restaurantId);
                sendJson(exchange, 200, Map.of("message", "Added to favorites"));
                return;
            }
            // DELETE /favorites/{restaurantId}
            else if (method.equalsIgnoreCase("DELETE") && path.matches("/favorites/\\d+")) {
                int restaurantId = extractIdFromPath(path, "/favorites/");
                Restaurant r = restaurantDao.findById(restaurantId);
                if (r == null) { sendJson(exchange, 404, Map.of("error", "Restaurant not found")); return; }
                favoriteDao.removeFavorite(userId, restaurantId);
                sendJson(exchange, 200, Map.of("message", "Removed from favorites"));
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