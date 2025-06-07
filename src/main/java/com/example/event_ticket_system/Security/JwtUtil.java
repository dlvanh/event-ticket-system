package com.example.event_ticket_system.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    // Tạo token từ fullname và vai trò
    public String generateToken(String fullName, String role, Long id) {
        System.out.println("Generating token for: fullname=" + fullName + ", role=" + role + ", id=" + id);
        System.out.println("Secret Key: " + SECRET_KEY);
        return Jwts.builder()
            .setSubject(String.valueOf(id))
            .claim("user_fullName", fullName)
            .claim("user_role", role)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 tiếng
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    // Giải mã token và lấy id người dùng
    public Long extractUserId(String token) {
        return Long.valueOf(extractAllClaims(token).getSubject());
    }

    // Giải mã token và lấy thông tin fullname
    public String extractFullname(String token) {
        return extractClaim(token, claims -> claims.get("user_fullName", String.class));
    }

    // Giải mã token và lấy thông tin vai trò
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("user_role", String.class));
    }

    // Giải mã toàn bộ claims
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Hàm trích xuất một claim cụ thể
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Kiểm tra token có hợp lệ không
    public boolean validateToken(String token, String fullname) {
        try {
            return fullname.equals(extractFullname(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    // Kiểm tra token hết hạn chưa
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}