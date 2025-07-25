package com.example.foodapp.handler;

import com.example.foodapp.dao.OrderDao;
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

public class TransactionHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final OrderDao orderDao = new OrderDao();

    public TransactionHandler() {
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
        int userId = Integer.parseInt(claims.getSubject());

        try {
            if (method.equalsIgnoreCase("POST") && ("/wallet/top-up".equals(path) || "/wallet/topup".equals(path))) {
                // Wallet top-up
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> req = mapper.readValue(json, Map.class);
                if (!req.containsKey("amount")) {
                    sendJson(exchange, 400, Map.of("error", "Missing amount"));
                    return;
                }
                int amount = (int) req.get("amount");
                String paymentMethod = req.getOrDefault("method", "topup").toString();
                if (amount <= 0) {
                    sendJson(exchange, 400, Map.of("error", "Amount must be positive"));
                    return;
                }
                Transaction tx = new Transaction();
                tx.setOrderId(null);
                tx.setUserId(userId);
                tx.setMethod(paymentMethod);
                tx.setType("top-up");
                tx.setAmount(amount);
                tx.setStatus("success");
                orderDao.insertTransaction(tx);
                // Update wallet balance
                com.example.foodapp.dao.UserDao userDao = new com.example.foodapp.dao.UserDao();
                int currentBalance = userDao.getWalletBalance(userId);
                userDao.updateWalletBalance(userId, currentBalance + amount);
                sendJson(exchange, 200, Map.of("message", "Wallet top-up successful", "transaction", tx));
                return;
            } else if (method.equalsIgnoreCase("GET") && "/transactions".equals(path)) {
                // Get user's transaction history
                List<Transaction> txs = orderDao.getTransactionsByUser(userId);
                sendJson(exchange, 200, txs);
                return;
            } else if (method.equalsIgnoreCase("GET") && "/wallet/balance".equals(path)) {
                // Get user's wallet balance
                int balance = 0;
                try {
                    balance = new com.example.foodapp.dao.UserDao().getWalletBalance(userId);
                } catch (Exception e) { sendJson(exchange, 500, Map.of("error", "Failed to fetch wallet balance")); return; }
                sendJson(exchange, 200, Map.of("wallet_balance", balance));
                return;
            } else if (method.equalsIgnoreCase("POST") && "/payment/online".equals(path)) {
                // Online or wallet payment for an order
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Map<String, Object> req = mapper.readValue(json, Map.class);
                if (!req.containsKey("order_id") || !req.containsKey("method")) {
                    sendJson(exchange, 400, Map.of("error", "Missing order_id or method"));
                    return;
                }
                int orderId = (int) req.get("order_id");
                String paymentMethod = req.get("method").toString();
                paymentMethod = paymentMethod.trim().toLowerCase();
                System.out.println("[DEBUG] Payment method received: '" + paymentMethod + "'");
                // Fetch order and price
                com.example.foodapp.dao.OrderDao orderDao2 = new com.example.foodapp.dao.OrderDao();
                com.example.foodapp.model.entity.Order order;
                try {
                    order = orderDao2.getOrderById(orderId);
                } catch (Exception e) { sendJson(exchange, 404, Map.of("error", "Order not found")); return; }
                if (order == null || order.getCustomerId() != userId) {
                    sendJson(exchange, 404, Map.of("error", "Order not found or not yours"));
                    return;
                }
                int amount = order.getPayPrice();
                if ("wallet".equals(paymentMethod)) {
                    com.example.foodapp.dao.UserDao userDao = new com.example.foodapp.dao.UserDao();
                    int currentBalance = userDao.getWalletBalance(userId);
                    if (currentBalance < amount) {
                        sendJson(exchange, 409, Map.of("error", "Insufficient wallet balance"));
                        return;
                    }
                    userDao.updateWalletBalance(userId, currentBalance - amount);
                } else if (!"online".equals(paymentMethod)) {
                    sendJson(exchange, 400, Map.of("error", "Invalid payment method: '" + paymentMethod + "'"));
                    return;
                }
                // Create payment transaction
                Transaction tx = new Transaction();
                tx.setOrderId(orderId);
                tx.setUserId(userId);
                tx.setMethod(paymentMethod);
                tx.setType("payment");
                tx.setAmount(amount);
                tx.setStatus("success");
                orderDao.insertTransaction(tx);
                // Always update order status to 'waiting vendor' after payment
                try {
                    orderDao2.updateOrderStatus(orderId, "waiting vendor");
                    orderDao2.insertOrderStatusHistory(orderId, "waiting vendor", "system");
                    System.out.println("[DEBUG] Order status updated to 'waiting vendor' for order_id=" + orderId);
                } catch (Exception e) {
                    System.err.println("[ERROR] Failed to update order status to 'waiting vendor' for order_id=" + orderId + ": " + e.getMessage());
                    e.printStackTrace();
                }
                sendJson(exchange, 200, tx);
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