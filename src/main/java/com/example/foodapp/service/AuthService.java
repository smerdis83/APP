package com.example.foodapp.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.foodapp.dao.UserDao;
import com.example.foodapp.model.entity.Role;
import com.example.foodapp.model.entity.User;
import com.example.foodapp.security.JwtUtil;

import java.sql.SQLException;

public class AuthService {
    private final UserDao userDao = new UserDao();

    /**
     * Registers a new user.
     * @param fullName   User's full name
     * @param phone      Unique phone number
     * @param email      Email (nullable)
     * @param password   Raw password
     * @param roleStr    "BUYER", "SELLER", or "COURIER"
     * @throws IllegalArgumentException if phone already exists or role invalid
     * @throws SQLException             on DB errors
     */
    public User register(String fullName, String phone, String email, String password, String roleStr)
        throws SQLException {

        // 1) Check if phone already exists
        if (userDao.findByPhone(phone) != null) {
            throw new IllegalArgumentException("Phone number already registered.");
        }

        // 2) Parse role
        Role role;
        try {
            role = Role.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr);
        }

        // 3) Hash the password
        String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        // 4) Create User and save
        User user = new User(fullName, phone, email, passwordHash, role);
        userDao.createUser(user);
        return user;
    }

    /**
     * Logs in a user by phone+password. Returns a JWT on success.
     * @param phone    Phone number
     * @param password Raw password
     * @return JWT string
     * @throws IllegalArgumentException if credentials are invalid or user disabled
     * @throws SQLException             on DB errors
     */
    public String login(String phone, String password) throws SQLException {
        // 1) Lookup user by phone
        User user = userDao.findByPhone(phone);
        if (user == null) {
            throw new IllegalArgumentException("Invalid phone or password.");
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled.");
        }

        // 2) Verify password with BCrypt
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
        if (!result.verified) {
            throw new IllegalArgumentException("Invalid phone or password.");
        }

        // 3) Generate JWT with user ID and role
        return JwtUtil.generateToken(user.getId(), user.getRole().name());
    }
} 