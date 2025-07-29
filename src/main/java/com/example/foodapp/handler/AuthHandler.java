package com.example.foodapp.handler;

import com.example.foodapp.model.entity.AuthResponse;
import com.example.foodapp.model.entity.LoginRequest;
import com.example.foodapp.model.entity.RegisterRequest;
import com.example.foodapp.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final AuthService authService = new AuthService();
    private final ObjectMapper mapper = new ObjectMapper();

    public AuthHandler() {
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        // Check Content-Type for POST /auth/register and /auth/login
        if ("POST".equalsIgnoreCase(method) && ("/auth/register".equals(path) || "/auth/login".equals(path))) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
                sendJson(exchange, 415, Map.of("error", "Unsupported Media Type: Content-Type must be application/json"));
                return;
            }
        }

        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String requestJson = sb.toString();

            if ("POST".equalsIgnoreCase(method) && "/auth/logout".equals(path)) {
                // Require Authorization header
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    sendJson(exchange, 401, Map.of("error", "Missing or invalid Authorization header"));
                    return;
                }
                String token = authHeader.substring("Bearer ".length()).trim();
                try {
                    com.example.foodapp.security.JwtUtil.parseToken(token);
                } catch (Exception e) {
                    sendJson(exchange, 401, Map.of("error", "Invalid token"));
                    return;
                }
                sendJson(exchange, 200, Map.of("message", "Logged out successfully"));
                return;
            }

            if ("POST".equalsIgnoreCase(method) && "/auth/register".equals(path)) {
                Map<String, Object> registerReq = mapper.readValue(requestJson, Map.class);
                // Validate required fields
                if (!registerReq.containsKey("fullName") || !registerReq.containsKey("phone") ||
                    !registerReq.containsKey("password") || !registerReq.containsKey("role")) {
                    sendJson(exchange, 400, Map.of("error", "Missing required fields: fullName, phone, password, role"));
                    return;
                }
                try {
                    var user = authService.register(
                        (String) registerReq.get("fullName"),
                        (String) registerReq.get("phone"),
                        (String) registerReq.get("email"),
                        (String) registerReq.get("password"),
                        (String) registerReq.get("role")
                    );
                    String token = authService.login((String) registerReq.get("phone"), (String) registerReq.get("password"));
                    user.setPasswordHash(null);
                    sendJson(exchange, 200, Map.of(
                        "message", "User registered successfully",
                        "user_id", String.valueOf(user.getId()),
                        "token", token,
                        "user", user
                    ));
                    return;
                } catch (IllegalArgumentException e) {
                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("phone number already registered")) {
                        sendJson(exchange, 409, Map.of("error", "Phone number already exists"));
                    } else {
                        sendJson(exchange, 400, Map.of("error", e.getMessage()));
                    }
                    return;
                }
            } else if ("POST".equalsIgnoreCase(method) && "/auth/login".equals(path)) {
                Map<String, Object> loginReq = mapper.readValue(requestJson, Map.class);
                if (!loginReq.containsKey("phone") || !loginReq.containsKey("password")) {
                    sendJson(exchange, 400, Map.of("error", "Missing required fields: phone, password"));
                    return;
                }
                try {
                    String phone = (String) loginReq.get("phone");
                    String password = (String) loginReq.get("password");
                    String token = authService.login(phone, password);
                    var user = authService.getUserByPhone(phone);
                    user.setPasswordHash(null);
                    // Ensure walletBalance is set
                    user.setWalletBalance(new com.example.foodapp.dao.UserDao().getWalletBalance(user.getId()));
                    sendJson(exchange, 200, Map.of(
                        "message", "User logged in successfully",
                        "token", token,
                        "user", user
                    ));
                    return;
                } catch (IllegalArgumentException e) {
                    sendJson(exchange, 401, Map.of("error", e.getMessage()));
                    return;
                } catch (Exception e) {
                    sendJson(exchange, 500, Map.of("error", "Server error: " + e.getMessage()));
                    return;
                }
            } else {
                sendJson(exchange, 404, Map.of("error", "Not Found"));
                return;
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
}
