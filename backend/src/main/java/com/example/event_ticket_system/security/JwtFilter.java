package com.example.event_ticket_system.security;

import com.example.event_ticket_system.enums.UserRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/oauth2/") || uri.startsWith("/login/oauth2/")) {
            chain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = authorizationHeader.substring(7);
        try {
            Claims claims = jwtUtil.extractAllClaims(jwt);
            if (!jwtUtil.isTokenValid(claims)) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            Integer userId = Integer.parseInt(claims.getSubject());
            String fullName = claims.get("full_name", String.class);
            String email = claims.get("email", String.class);
            String roleClaim = claims.get("role", String.class);

            if (roleClaim == null) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            String normalizedRole = roleClaim.startsWith("ROLE_") ? roleClaim.substring(5) : roleClaim;
            UserRole userRole = UserRole.valueOf(normalizedRole);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                CustomUserPrincipal principal = new CustomUserPrincipal(userId, email, fullName, userRole);
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            log.debug("Invalid JWT token on {}: {}", uri, e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"status\":401,\"message\":\"" + message + "\"}");
    }
}
