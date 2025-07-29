package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.Transaction;
import com.example.foodapp.model.entity.Order;
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
import com.example.foodapp.model.entity.User;

public class AdminHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final OrderDao orderDao = new OrderDao();
    private final UserDao userDao = new UserDao();

    public AdminHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
            return;
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        Claims claims;
        try { claims = JwtUtil.parseToken(token); } catch (Exception e) { sendJson(exchange, 401, Map.of("error", "Invalid token")); return; }
        String role = claims.get("role", String.class);
        if (!"ADMIN".equals(role)) { sendJson(exchange, 403, Map.of("error", "Forbidden: must be an admin")); return; }

        try {
            System.out.println("[ADMIN HANDLER] Path: " + path + ", Method: " + method);
            System.out.println("[ADMIN HANDLER] Auth Header: " + authHeader);
            if (method.equalsIgnoreCase("GET") && "/admin/transactions".equals(path)) {
                // Admin: view all transactions
                List<Transaction> txs = orderDao.getAllTransactions();
                sendJson(exchange, 200, txs);
                return;
            } else if (method.equalsIgnoreCase("GET") && "/admin/orders".equals(path)) {
                System.out.println("[ADMIN HANDLER] GET /admin/orders called");
                // Admin: view all orders
                List<Order> orders = orderDao.getAllOrders();
                sendJson(exchange, 200, orders);
                return;
            } else if (method.equalsIgnoreCase("GET") && "/admin/users".equals(path)) {
                System.out.println("[ADMIN HANDLER] GET /admin/users called");
                // Admin: view all users
                List<User> users = userDao.findAll();
                sendJson(exchange, 200, users);
                return;
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/admin/users/\\d+$")) {
                int userId = Integer.parseInt(path.substring("/admin/users/".length()));
                try {
                    User user = userDao.findById(userId);
                    if (user == null) {
                        sendJson(exchange, 404, Map.of("error", "User not found"));
                        return;
                    }
                    userDao.deleteUser(userId);
                    sendJson(exchange, 200, Map.of("message", "User deleted"));
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/admin/restaurants/\\d+$")) {
                int restId = Integer.parseInt(path.substring("/admin/restaurants/".length()));
                try {
                    var restDao = new com.example.foodapp.dao.RestaurantDao();
                    var restaurant = restDao.findById(restId);
                    if (restaurant == null) {
                        sendJson(exchange, 404, Map.of("error", "Restaurant not found"));
                        return;
                    }
                    restDao.deleteRestaurant(restId);
                    sendJson(exchange, 200, Map.of("message", "Restaurant deleted"));
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            } else if (method.equalsIgnoreCase("DELETE") && path.matches("/admin/orders/\\d+$")) {
                int orderId = Integer.parseInt(path.substring("/admin/orders/".length()));
                System.out.println("[ADMIN HANDLER] DELETE /admin/orders/" + orderId);
                try {
                    var order = orderDao.getOrderById(orderId);
                    if (order == null) {
                        System.out.println("[ADMIN HANDLER] Order not found: " + orderId);
                        sendJson(exchange, 404, Map.of("error", "Order not found"));
                        return;
                    }
                    orderDao.deleteOrder(orderId);
                    System.out.println("[ADMIN HANDLER] Order deleted: " + orderId);
                    sendJson(exchange, 200, Map.of("message", "Order deleted"));
                } catch (Exception e) {
                    System.out.println("[ADMIN HANDLER] Exception deleting order: " + e.getMessage());
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
                return;
            } else if (method.equalsIgnoreCase("PATCH") && path.matches("/admin/users/\\d+/status")) {
                // PATCH /admin/users/{id}/status - Update user status (approve/reject)
                int userId = Integer.parseInt(path.replaceAll(".*?/admin/users/(\\d+)/status", "$1"));
                try {
                    User user = userDao.findById(userId);
                    if (user == null) {
                        sendJson(exchange, 404, Map.of("error", "User not found"));
                        return;
                    }
                    
                    // Read request body
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                    }
                    String json = sb.toString();
                    Map<String, Object> req = mapper.readValue(json, Map.class);
                    
                    String status = (String) req.get("status");
                    if ("approved".equals(status)) {
                        user.setEnabled(true);
                        userDao.updateUser(user);
                        sendJson(exchange, 200, Map.of("message", "User approved successfully"));
                    } else if ("rejected".equals(status)) {
                        user.setEnabled(false);
                        userDao.updateUser(user);
                        sendJson(exchange, 200, Map.of("message", "User rejected successfully"));
                    } else {
                        sendJson(exchange, 400, Map.of("error", "Invalid status. Use 'approved' or 'rejected'"));
                    }
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                }
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
} 