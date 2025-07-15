package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.FoodItemDao;
import com.example.foodapp.model.entity.Order;
import com.example.foodapp.model.entity.OrderItem;
import com.example.foodapp.model.entity.FoodItem;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                // Price calculation logic
                int rawPrice = 0;
                for (OrderItem oi : order.getItems()) {
                    FoodItem fi;
                    try {
                        fi = foodItemDao.getFoodItemById(oi.getItem_id());
                        if (fi == null) { sendJson(exchange, 400, Map.of("error", "Invalid item_id: " + oi.getItem_id())); return; }
                    } catch (Exception e) {
                        sendJson(exchange, 500, Map.of("error", "Failed to fetch item: " + oi.getItem_id()));
                        return;
                    }
                    System.out.println("OrderItem: id=" + oi.getItem_id() + ", quantity=" + oi.getQuantity() + ", price=" + fi.getPrice());
                    rawPrice += fi.getPrice() * oi.getQuantity();
                }
                order.setRawPrice(rawPrice);
                order.setTaxFee(0); // You can add logic here
                order.setAdditionalFee(0); // You can add logic here
                order.setCourierFee(0); // You can add logic here
                order.setPayPrice(rawPrice); // For now, just rawPrice
                order.setStatus("submitted");
                orderDao.addOrder(order);
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
                sendJson(exchange, 200, order);
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