package com.example.foodapp.handler;

import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ProfileHandler implements HttpHandler {
    private final ObjectMapper mapper;
    private final UserDao userDao = new UserDao();

    public ProfileHandler() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            if (!"GET".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
                sendJson(exchange, 405, Map.of("error", "Method Not Allowed"));
                return;
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                return;
            }

            String token = authHeader.substring("Bearer ".length()).trim();
            Claims claims;
            try {
                claims = JwtUtil.parseToken(token);
            } catch (Exception e) {
                sendJson(exchange, 401, Map.of("error", "Invalid token"));
                return;
            }

            int userId;
            try {
                userId = Integer.parseInt(claims.getSubject());
            } catch (Exception e) {
                sendJson(exchange, 400, Map.of("error", "Invalid user ID in token"));
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                User user = userDao.findById(userId);
                if (user == null) {
                    sendJson(exchange, 404, Map.of("error", "User not found"));
                    return;
                }
                user.setPasswordHash(null); // Don't expose password
                sendJson(exchange, 200, user);
                return;
            } else if ("PUT".equalsIgnoreCase(method)) {
                String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
                    sendJson(exchange, 415, Map.of("error", "Unsupported Media Type: Content-Type must be application/json"));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                try (var reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(exchange.getRequestBody(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                String requestJson = sb.toString();
                Map<String, Object> updateReq;
                try {
                    updateReq = mapper.readValue(requestJson, Map.class);
                } catch (Exception e) {
                    sendJson(exchange, 400, Map.of("error", "Invalid input: " + e.getMessage()));
                    return;
                }
                if (updateReq.isEmpty()) {
                    sendJson(exchange, 400, Map.of("error", "No updatable fields provided"));
                    return;
                }
                User user = userDao.findById(userId);
                if (user == null) {
                    sendJson(exchange, 404, Map.of("error", "User not found"));
                    return;
                }
                if (updateReq.containsKey("fullName")) user.setFullName((String) updateReq.get("fullName"));
                if (updateReq.containsKey("phone")) user.setPhone((String) updateReq.get("phone"));
                if (updateReq.containsKey("email")) user.setEmail((String) updateReq.get("email"));
                if (updateReq.containsKey("address")) user.setAddress((String) updateReq.get("address"));
                if (updateReq.containsKey("profileImageBase64")) user.setProfileImageBase64((String) updateReq.get("profileImageBase64"));
                if (updateReq.containsKey("bankInfo") && updateReq.get("bankInfo") instanceof Map) {
                    Map<String, Object> bankMap = (Map<String, Object>) updateReq.get("bankInfo");
                    String bankName = bankMap.get("bankName") != null ? bankMap.get("bankName").toString() : null;
                    String accountNumber = bankMap.get("accountNumber") != null ? bankMap.get("accountNumber").toString() : null;
                    user.setBankInfo(new User.BankInfo(bankName, accountNumber));
                }
                try {
                    userDao.updateUser(user);
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate")) {
                        sendJson(exchange, 409, Map.of("error", "Phone number already exists"));
                    } else {
                        sendJson(exchange, 500, Map.of("error", "Database error: " + e.getMessage()));
                    }
                    return;
                }
                sendJson(exchange, 200, Map.of("message", "Profile updated successfully"));
                return;
            }
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "Internal server error: " + e.getMessage()));
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        String json = mapper.writeValueAsString(payload);
        byte[] responseBytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
} 