package com.example.foodapp.util;

import com.example.foodapp.model.entity.User;

public class SessionManager {
    
    private static SessionManager instance;
    private User currentUser;
    private String token;
    
    private SessionManager() {
        // Private constructor for singleton
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getToken() {
        return token;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null && token != null && !token.isEmpty();
    }
    
    public void clearSession() {
        this.currentUser = null;
        this.token = null;
    }
    
    public String getAuthorizationHeader() {
        if (token != null) {
            return "Bearer " + token;
        }
        return null;
    }
    
    public int getCurrentUserId() {
        if (currentUser != null) {
            return currentUser.getId();
        }
        return -1; // Return -1 if no user is logged in
    }
} 