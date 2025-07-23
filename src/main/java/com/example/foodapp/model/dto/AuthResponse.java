package com.example.foodapp.model.dto;

import com.example.foodapp.model.entity.User;

public class AuthResponse {
    private String token;
    private String error;
    private User user;
    private String message;

    public AuthResponse() { }

    public AuthResponse(String token, String error) {
        this.token = token;
        this.error = error;
    }

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    // Getters & setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
} 