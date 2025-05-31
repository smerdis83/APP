package com.example.foodapp.security;

import com.example.foodapp.config.AppConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class JwtUtil {
    private static final String SECRET = AppConfig.getJwtSecret();
    private static final long EXPIRATION_MS = AppConfig.getJwtExpirationMs();

    /** Generate a JWT containing userId (sub) and role as a claim */
    public static String generateToken(int userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(SignatureAlgorithm.HS256, SECRET)
            .compact();
    }

    /** Parse and validate a JWT using JJWT 0.9.1 API */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                   .setSigningKey(SECRET)
                   .parseClaimsJws(token)
                   .getBody();
    }
} 