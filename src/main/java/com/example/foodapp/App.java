package com.example.foodapp;

import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.Role;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.util.JdbcUtil;
import com.example.foodapp.handler.AuthHandler;
import com.example.foodapp.handler.ProfileHandler;
import com.example.foodapp.handler.RestaurantHandler;
import com.example.foodapp.handler.OrderHandler;
import com.example.foodapp.handler.CourierHandler;
import com.example.foodapp.handler.TransactionHandler;
import com.example.foodapp.handler.AdminHandler;
import com.example.foodapp.handler.VendorItemHandler;
import com.example.foodapp.handler.FavoriteHandler;
import com.example.foodapp.handler.RatingHandler;
import com.example.foodapp.handler.AddressHandler;
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

        CourierHandler courierHandler = new CourierHandler();
        server.createContext("/deliveries", courierHandler);

        TransactionHandler transactionHandler = new TransactionHandler();
        server.createContext("/wallet/top-up", transactionHandler);
        server.createContext("/wallet/balance", transactionHandler);
        server.createContext("/transactions", transactionHandler);
        server.createContext("/payment/online", transactionHandler);

        AdminHandler adminHandler = new AdminHandler();
        server.createContext("/admin/transactions", adminHandler);
        server.createContext("/admin/orders", adminHandler);
        server.createContext("/admin/users", adminHandler);

        VendorItemHandler vendorItemHandler = new VendorItemHandler();
        server.createContext("/vendors", vendorItemHandler);
        server.createContext("/vendors/{id}", vendorItemHandler);
        server.createContext("/items", vendorItemHandler);
        server.createContext("/items/{id}", vendorItemHandler);

        FavoriteHandler favoriteHandler = new FavoriteHandler();
        server.createContext("/favorites", favoriteHandler);
        server.createContext("/favorites/", favoriteHandler);

        RatingHandler ratingHandler = new RatingHandler();
        server.createContext("/ratings", ratingHandler);

        AddressHandler addressHandler = new AddressHandler();
        server.createContext("/addresses", addressHandler);

        // 3) Use a thread pool to handle requests
        server.setExecutor(Executors.newFixedThreadPool(8));

        // 4) Start the server
        server.start();
        System.out.println("Server running at http://localhost:" + port);
    }
}
