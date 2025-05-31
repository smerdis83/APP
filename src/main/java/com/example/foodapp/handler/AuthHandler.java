package com.example.foodapp.handler;

import com.example.foodapp.model.dto.AuthResponse;
import com.example.foodapp.model.dto.LoginRequest;
import com.example.foodapp.model.dto.RegisterRequest;
import com.example.foodapp.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class AuthHandler implements HttpHandler {
    private final AuthService authService = new AuthService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path   = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        AuthResponse authResponse;
        int statusCode;

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

            if ("POST".equalsIgnoreCase(method) && "/auth/register".equals(path)) {
                RegisterRequest registerReq = mapper.readValue(requestJson, RegisterRequest.class);
                authService.register(
                        registerReq.getFullName(),
                        registerReq.getPhone(),
                        registerReq.getEmail(),
                        registerReq.getPassword(),
                        registerReq.getRole()
                );
                statusCode = 201; // Created
                authResponse = new AuthResponse(null, null);

            } else if ("POST".equalsIgnoreCase(method) && "/auth/login".equals(path)) {
                LoginRequest loginReq = mapper.readValue(requestJson, LoginRequest.class);
                String token = authService.login(loginReq.getPhone(), loginReq.getPassword());
                statusCode = 200; // OK
                authResponse = new AuthResponse(token, null);

            } else {
                statusCode = 404; // Not Found
                authResponse = new AuthResponse(null, "Not Found");
            }

        } catch (IllegalArgumentException e) {
            statusCode = 400; // Bad Request
            authResponse = new AuthResponse(null, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            statusCode = 500; // Internal Server Error
            authResponse = new AuthResponse(null, "Server error: " + e.getMessage());
        }

        byte[] responseBytes = mapper.writeValueAsBytes(authResponse);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
