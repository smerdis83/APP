package com.example.foodapp.handler;

import com.example.foodapp.dao.AddressDao;
import com.example.foodapp.model.entity.Address;
import com.example.foodapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AddressHandler implements HttpHandler {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AddressDao addressDao = new AddressDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        try {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendJson(exchange, 401, new ErrorResponse("Missing or invalid Authorization header"));
                return;
            }
            String token = authHeader.substring("Bearer ".length()).trim();
            Claims claims;
            try { claims = JwtUtil.parseToken(token); } catch (ExpiredJwtException e) { sendJson(exchange, 401, new ErrorResponse("Token expired")); return; }
            catch (SignatureException | MalformedJwtException e) { sendJson(exchange, 401, new ErrorResponse("Invalid token")); return; }
            catch (Exception e) { sendJson(exchange, 500, new ErrorResponse("Server error")); return; }
            int userId;
            try { userId = Integer.parseInt(claims.getSubject()); } catch (NumberFormatException e) { sendJson(exchange, 400, new ErrorResponse("Invalid user ID in token")); return; }

            if ("GET".equalsIgnoreCase(method) && "/addresses".equals(path)) {
                List<Address> addresses = addressDao.findByUser(userId);
                sendJson(exchange, 200, addresses);
                return;
            } else if ("POST".equalsIgnoreCase(method) && "/addresses".equals(path)) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) { sb.append(line); }
                }
                String json = sb.toString();
                Address address = mapper.readValue(json, Address.class);
                address.setUserId(userId);
                addressDao.addAddress(address);
                sendJson(exchange, 201, address);
                return;
            } else {
                sendJson(exchange, 404, new ErrorResponse("Not Found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    private void sendJson(HttpExchange exchange, int statusCode, Object payload) throws IOException {
        String json = mapper.writeValueAsString(payload);
        byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    static class ErrorResponse {
        private final String error;
        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }
} 