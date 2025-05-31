package com.example.foodapp.model.dto;

public class AuthResponse {
    private String token;
    private String error;

    public AuthResponse() { }

    public AuthResponse(String token, String error) {
        this.token = token;
        this.error = error;
    }

    // Getters & setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
} 