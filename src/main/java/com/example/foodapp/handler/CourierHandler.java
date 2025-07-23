package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.model.entity.Order;
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
import java.util.List;

public class CourierHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final OrderDao orderDao = new OrderDao();

    public CourierHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // Only handle /deliveries/available GET
        if ("GET".equalsIgnoreCase(method) && "/deliveries/available".equals(path)) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                return;
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            Claims claims;
            try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
            String role = claims.get("role", String.class);
            if (!"COURIER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a courier")); return; }
            try {
                List<Order> available = orderDao.getAvailableDeliveries();
                sendJson(exchange, 200, available);
            } catch (Exception e) {
                sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
            }
            return;
        }
        // Handle GET /deliveries/history
        if ("GET".equalsIgnoreCase(method) && "/deliveries/history".equals(path)) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                return;
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            Claims claims;
            try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
            String role = claims.get("role", String.class);
            int courierId = Integer.parseInt(claims.getSubject());
            if (!"COURIER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a courier")); return; }
            try {
                List<Order> orders = orderDao.getOrdersByCourier(courierId);
                sendJson(exchange, 200, orders);
            } catch (Exception e) {
                sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
            }
            return;
        }
        // Handle PATCH /deliveries/{order_id}
        if ("PATCH".equalsIgnoreCase(method) && path.matches("/deliveries/\\d+")) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                return;
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            Claims claims;
            try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
            String role = claims.get("role", String.class);
            int courierId = Integer.parseInt(claims.getSubject());
            if (!"COURIER".equals(role)) { sendJson(exchange, 403, new ErrorResponse("Forbidden: must be a courier")); return; }
            // Extract order_id from path
            int orderId;
            try {
                orderId = Integer.parseInt(path.substring("/deliveries/".length()));
            } catch (Exception e) {
                sendJson(exchange, 400, new ErrorResponse("Invalid order_id in path"));
                return;
            }
            // Parse status from request body
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(exchange.getRequestBody(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line; while ((line = reader.readLine()) != null) { sb.append(line); }
            }
            String json = sb.toString();
            String newStatus;
            try {
                java.util.Map<String, Object> req = mapper.readValue(json, java.util.Map.class);
                if (!req.containsKey("status")) { sendJson(exchange, 400, new ErrorResponse("Missing status")); return; }
                newStatus = req.get("status").toString();
            } catch (Exception e) {
                sendJson(exchange, 400, new ErrorResponse("Invalid JSON body"));
                return;
            }
            try {
                if ("accepted".equals(newStatus)) {
                    Order updated = orderDao.assignCourierToOrder(orderId, courierId, newStatus);
                    if (updated == null) {
                        sendJson(exchange, 409, new ErrorResponse("Order is not available for assignment (already assigned, not found, or wrong status)"));
                        return;
                    }
                    orderDao.insertOrderStatusHistory(orderId, newStatus, "courier");
                    sendJson(exchange, 200, java.util.Map.of("message", "Order accepted successfully", "order", updated));
                } else if ("received".equals(newStatus) || "delivered".equals(newStatus)) {
                    Order updated = orderDao.updateCourierOrderStatus(orderId, courierId, newStatus);
                    if (updated == null) {
                        sendJson(exchange, 409, new ErrorResponse("Order status transition not allowed (wrong current status, not assigned, or not found)"));
                        return;
                    }
                    orderDao.insertOrderStatusHistory(orderId, newStatus, "courier");
                    sendJson(exchange, 200, java.util.Map.of("message", "Order status updated to " + newStatus, "order", updated));
                } else {
                    sendJson(exchange, 400, new ErrorResponse("Invalid status: must be one of 'accepted', 'received', or 'delivered'"));
                }
            } catch (Exception e) {
                sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
            }
            return;
        }
        // Not found
        sendJson(exchange, 404, new ErrorResponse("Not Found"));
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        String json = mapper.writeValueAsString(payload);
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    // Simple error response class
    static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
} 