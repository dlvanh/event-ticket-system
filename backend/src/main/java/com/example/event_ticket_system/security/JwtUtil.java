package com.example.event_ticket_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT secret key must be at least 64 bytes for HS512");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String fullName, String role, Integer id) {
        return generateToken(fullName, null, role, id);
    }

    public String generateToken(String fullName, String email, String role, Integer id) {
        var builder = Jwts.builder()
                .subject(String.valueOf(id))
                .claim("full_name", fullName)
                .claim("role", role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs));
        if (email != null) {
            builder.claim("email", email);
        }
        return builder.signWith(getSigningKey(), Jwts.SIG.HS512).compact();
    }

    public Integer extractUserId(String token) {
        return Integer.parseInt(extractClaim(token, Claims::getSubject));
    }

    public String extractFullName(String token) {
        return extractClaim(token, claims -> claims.get("full_name", String.class));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.after(new Date());
    }
}