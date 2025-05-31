package com.example.foodapp;

import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.Role;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.util.JdbcUtil;
import com.example.foodapp.handler.AuthHandler;
import com.example.foodapp.handler.ProfileHandler;
import com.sun.net.httpserver.HttpServer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class App {
    public static void main(String[] args) throws Exception {
        // FIRST: Test DB connection (as before)
        System.out.println("Testing database connection...");
        testConnection();

        // SECOND: Test UserDao
        System.out.println("\nTesting UserDao insert and query...");
        testUserDao();

        // 1) Start the HTTP server on port 8000 (or whichever port you choose)
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 2) Register contexts (endpoints) with their handlers
        AuthHandler authHandler = new AuthHandler();
        server.createContext("/auth/register", authHandler);
        server.createContext("/auth/login", authHandler);

        // Protected "profile" endpoint
        ProfileHandler profileHandler = new ProfileHandler();
        server.createContext("/auth/profile", profileHandler);

        // 3) Use a thread pool to handle requests
        server.setExecutor(Executors.newFixedThreadPool(8));

        // 4) Start the server
        server.start();
        System.out.println("Server running at http://localhost:" + port);
    }

    /** 
     * Exactly the same testConnection() you ran earlier, creating test_connection table,
     * inserting one row, and printing it back.
     */
    private static void testConnection() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtil.getConnection();
            System.out.println("Connected to MySQL successfully!");

            stmt = conn.createStatement();
            String createTableSQL = ""
                + "CREATE TABLE IF NOT EXISTS test_connection ("
                + "  id INT AUTO_INCREMENT PRIMARY KEY, "
                + "  message VARCHAR(255) NOT NULL"
                + ")";
            stmt.execute(createTableSQL);

            stmt.execute("INSERT INTO test_connection (message) VALUES ('Hello from JDBC!')");

            rs = stmt.executeQuery("SELECT id, message FROM test_connection ORDER BY id DESC LIMIT 1");
            if (rs.next()) {
                int id = rs.getInt("id");
                String msg = rs.getString("message");
                System.out.println("Latest row in test_connection: id=" + id + ", message=\"" + msg + "\"");
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Could not connect or run test query:");
            e.printStackTrace();
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException ignored) {}
            JdbcUtil.closeQuietly(conn);
        }
    }

    /**
     * Tests inserting and fetching a User via UserDao.
     */
    private static void testUserDao() {
        UserDao userDao = new UserDao();

        // 1) Create a new User object
        String randomPhone = "0912" + (int)(Math.random() * 1000000);
        User newUser = new User(
            "Test User",
            randomPhone,
            "testuser@example.com",
            "$2a$10$eBHrjKX..." /* this would be a Bcrypt hash in real life */,
            Role.BUYER
        );

        try {
            // 2) Insert the user
            userDao.createUser(newUser);
            System.out.println("Inserted user with ID = " + newUser.getId());

            // 3) Fetch the same user by phone
            User fetched = userDao.findByPhone(randomPhone);
            if (fetched != null) {
                System.out.println("Fetched user: " + fetched);
            } else {
                System.out.println("No user found with phone = " + randomPhone);
            }
        } catch (SQLException e) {
            System.err.println("ERROR in UserDao test:");
            e.printStackTrace();
        }
    }
}
