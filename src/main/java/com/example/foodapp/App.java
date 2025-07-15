package com.example.foodapp;

import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.Role;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.util.JdbcUtil;
import com.example.foodapp.handler.AuthHandler;
import com.example.foodapp.handler.ProfileHandler;
import com.example.foodapp.handler.RestaurantHandler;
import com.example.foodapp.handler.OrderHandler;
import com.sun.net.httpserver.HttpServer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws Exception {
        // 1) Start the HTTP server on port 8000 (or whichever port you choose)
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 2) Register contexts (endpoints) with their handlers
        AuthHandler authHandler = new AuthHandler();
        server.createContext("/auth/register", authHandler);
        server.createContext("/auth/login", authHandler);
        server.createContext("/auth/logout", authHandler);

        // Protected "profile" endpoint
        ProfileHandler profileHandler = new ProfileHandler();
        server.createContext("/auth/profile", profileHandler);

        // Restaurant endpoints
        RestaurantHandler restaurantHandler = new RestaurantHandler();
        server.createContext("/restaurants", restaurantHandler);

        OrderHandler orderHandler = new OrderHandler();
        server.createContext("/orders", orderHandler);

        // 3) Use a thread pool to handle requests
        server.setExecutor(Executors.newFixedThreadPool(8));

        // 4) Start the server
        server.start();
        System.out.println("Server running at http://localhost:" + port);
    }
}
