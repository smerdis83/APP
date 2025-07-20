package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.model.entity.Transaction;
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

public class OrderHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final OrderDao orderDao = new OrderDao();
    private final FoodItemDao foodItemDao = new FoodItemDao();

    public OrderHandler() {
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
        String role = claims.get("role", String.class);
        int userId = Integer.parseInt(claims.getSubject());

        try {
            if (method.equalsIgnoreCase("POST") && "/orders".equals(path)) {
                // Submit order (buyer only)
                if (!"BUYER".equals(role)) { sendJson(exchange, 403, Map.of("error", "Forbidden: must be a buyer")); return; }
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Order order = mapper.readValue(json, Order.class);
                order.setCustomerId(userId);
                // Price calculation logic & inventory check
                int rawPrice = 0;
                java.util.Map<Integer, Integer> toReduce = new java.util.HashMap<>(); // item_id -> quantity
                for (OrderItem oi : order.getItems()) {
                    FoodItem fi;
                    try {
                        fi = foodItemDao.getFoodItemById(oi.getItem_id());
                        if (fi == null) { sendJson(exchange, 400, Map.of("error", "Invalid item_id: " + oi.getItem_id())); return; }
                    } catch (Exception e) {
                        sendJson(exchange, 500, Map.of("error", "Failed to fetch item: " + oi.getItem_id()));
                        return;
                    }
                    if (oi.getQuantity() > fi.getSupply()) {
                        sendJson(exchange, 409, Map.of("error", "Insufficient stock for item_id: " + oi.getItem_id()));
                        return;
                    }
                    toReduce.put(oi.getItem_id(), oi.getQuantity());
                    rawPrice += fi.getPrice() * oi.getQuantity();
                }
                // All items have enough stock, reduce supply
                for (Map.Entry<Integer, Integer> entry : toReduce.entrySet()) {
                    try {
                        FoodItem fi = foodItemDao.getFoodItemById(entry.getKey());
                        fi.setSupply(fi.getSupply() - entry.getValue());
                        foodItemDao.updateFoodItem(fi);
                    } catch (Exception e) {
                        sendJson(exchange, 500, Map.of("error", "Failed to update stock for item_id: " + entry.getKey()));
                        return;
                    }
                }
                order.setRawPrice(rawPrice);
                order.setTaxFee(0); // You can add logic here
                order.setAdditionalFee(0); // You can add logic here
                order.setCourierFee(0); // You can add logic here
                order.setPayPrice(rawPrice); // For now, just rawPrice
                order.setStatus("submitted");
                orderDao.addOrder(order);
                orderDao.insertOrderStatusHistory(order.getId(), "submitted", "buyer");
                sendJson(exchange, 200, order);
                return;
            } else if (method.equalsIgnoreCase("GET") && path.matches("/orders/\\d+")) {
                // Get order details (buyer only, must own order)
                if (!"BUYER".equals(role)) { sendJson(exchange, 403, Map.of("error", "Forbidden: must be a buyer")); return; }
                int orderId = extractIdFromPath(path, "/orders/");
                Order order = orderDao.getOrderById(orderId);
                if (order == null || order.getCustomerId() != userId) {
                    sendJson(exchange, 404, Map.of("error", "Order not found"));
                    return;
                }
                // If a courier is assigned, include their info in the response (as 'courier_info')
                Map<String, Object> response = mapper.convertValue(order, Map.class);
                if (order.getCourierId() != null) {
                    try {
                        User courier = new UserDao().findById(order.getCourierId());
                        if (courier != null) {
                            response.put("courier_info", Map.of(
                                "id", courier.getId(),
                                "full_name", courier.getFullName(),
                                "phone", courier.getPhone()
                            ));
                        }
                    } catch (Exception e) { /* ignore, just omit courier_info if error */ }
                }
                // Add status history timeline
                try {
                    List<com.example.foodapp.model.entity.OrderStatusHistory> history = orderDao.getOrderStatusHistory(order.getId());
                    response.put("status_history", history);
                } catch (Exception e) { /* ignore, just omit if error */ }
                sendJson(exchange, 200, response);
                return;
            } else if (method.equalsIgnoreCase("GET") && "/orders/history".equals(path)) {
                // Get order history (buyer only)
                if (!"BUYER".equals(role)) { sendJson(exchange, 403, Map.of("error", "Forbidden: must be a buyer")); return; }
                List<Order> orders = orderDao.getOrdersByCustomer(userId);
                sendJson(exchange, 200, orders);
                return;
            } else {
                sendJson(exchange, 404, Map.of("error", "Not Found"));
                return;
            }
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
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

    private int extractIdFromPath(String path, String prefix) {
        String temp = path.substring(path.indexOf(prefix) + prefix.length());
        return Integer.parseInt(temp.replaceAll("[^0-9]", ""));
    }
} 