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
            // DEBUG: Print request details
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            System.out.println("\n>>> INCOMING REQUEST >>>");
            System.out.println("Path   : " + path);
            System.out.println("Method : " + method);
            System.out.println("Auth   : " + authHeader);
            System.out.println("Headers: ");
            exchange.getRequestHeaders().forEach((key, values) -> 
                System.out.println("  " + key + ": " + String.join(", ", values))
            );
            System.out.println("<<< END REQUEST <<<\n");

            // Only allow GET /auth/profile
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                System.out.println("DEBUG: Method not allowed: " + method);
                sendJson(exchange, 405, new ErrorResponse("Method Not Allowed"));
                return;
            }

            // 1) Read Authorization header
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("DEBUG: Invalid auth header: " + authHeader);
                sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                return;
            }

            String token = authHeader.substring("Bearer ".length()).trim();
            System.out.println("DEBUG: Extracted token: " + token.substring(0, Math.min(20, token.length())) + "...");

            // 2) Parse and validate JWT
            Claims claims;
            try {
                System.out.println("DEBUG: Attempting to parse token...");
                claims = JwtUtil.parseToken(token);
                System.out.println("DEBUG: Token parsed successfully. Subject: " + claims.getSubject());
            } catch (ExpiredJwtException e) {
                System.out.println("DEBUG: Token expired: " + e.getMessage());
                sendJson(exchange, 401, new ErrorResponse("Token expired"));
                return;
            } catch (SignatureException | MalformedJwtException e) {
                System.out.println("DEBUG: Invalid token: " + e.getMessage());
                sendJson(exchange, 401, new ErrorResponse("Invalid token"));
                return;
            } catch (Exception e) {
                System.out.println("DEBUG: Server error parsing token: " + e);
                e.printStackTrace();
                sendJson(exchange, 500, new ErrorResponse("Server error: " + e.getMessage()));
                return;
            }

            // 3) Extract userId and fetch from DB
            int userId;
            try {
                userId = Integer.parseInt(claims.getSubject());
                System.out.println("DEBUG: Parsed user ID: " + userId);
            } catch (NumberFormatException e) {
                System.out.println("DEBUG: Invalid user ID format: " + claims.getSubject());
                sendJson(exchange, 400, new ErrorResponse("Invalid user ID in token"));
                return;
            }

            User user;
            try {
                System.out.println("DEBUG: Attempting to find user " + userId);
                user = userDao.findById(userId);
                System.out.println("DEBUG: User lookup result: " + (user != null ? "found" : "not found"));
            } catch (Exception e) {
                System.out.println("DEBUG: Database error: " + e);
                e.printStackTrace();
                sendJson(exchange, 500, new ErrorResponse("Database error: " + e.getMessage()));
                return;
            }

            if (user == null) {
                System.out.println("DEBUG: User " + userId + " not found in database");
                sendJson(exchange, 404, new ErrorResponse("User not found"));
                return;
            }

            // 4) Return user profile as JSON (omit passwordHash)
            UserProfile profile = new UserProfile(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
            );

            System.out.println("DEBUG: Sending successful response for user " + userId);
            sendJson(exchange, 200, profile);
            System.out.println("DEBUG: Response sent successfully");

        } catch (Exception e) {
            System.out.println("DEBUG: Unexpected error: " + e);
            e.printStackTrace();
            sendJson(exchange, 500, new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    // Helper to send JSON response with a Java object
    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        try {
            System.out.println("DEBUG: Preparing JSON response. Status: " + statusCode);
            String json = mapper.writeValueAsString(payload);
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
                os.flush();
            }
            System.out.println("DEBUG: JSON response sent successfully");
        } catch (Exception e) {
            System.out.println("DEBUG: Error sending JSON response: " + e);
            e.printStackTrace();
            throw e;
        }
    }

    // DTO for error messages
    static class ErrorResponse {
        private final String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }

    // DTO for user profile (excluding passwordHash)
    static class UserProfile {
        private final int id;
        private final String fullName;
        private final String phone;
        private final String email;
        private final String role;
        private final boolean enabled;
        private final java.time.LocalDateTime createdAt;
        private final java.time.LocalDateTime updatedAt;

        public UserProfile(int id,
                         String fullName,
                         String phone,
                         String email,
                         String role,
                         boolean enabled,
                         java.time.LocalDateTime createdAt,
                         java.time.LocalDateTime updatedAt) {
            this.id = id;
            this.fullName = fullName;
            this.phone = phone;
            this.email = email;
            this.role = role;
            this.enabled = enabled;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Getters (Jackson needs these)
        public int getId() { return id; }
        public String getFullName() { return fullName; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isEnabled() { return enabled; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    }
} 