package com.example.event_ticket_system.security;

import com.example.event_ticket_system.service.impl.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtFilter jwtFilter;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost}")
    private String allowedOrigins;

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**", "/api/auth/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/auth/oauth2/**").permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            log.info("OAuth2 login successful");
                            response.sendRedirect("/api/auth/oauth2/google");
                        })
                        .failureHandler((request, response, exception) -> {
                            log.error("OAuth2 login failed: {}", exception.getMessage());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"status\":401,\"message\":\"OAuth2 login failed\"}");
                        })
                );
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth & documentation endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Public PayOS webhook (verified via HMAC signature)
                        .requestMatchers(HttpMethod.POST, "/api/payment/payos_transfer_handler").permitAll()
                        // Public read-only catalog, location, review, and chat endpoints
                        .requestMatchers(HttpMethod.GET, "/api/events/recommend").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/{eventId:[0-9]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/review/event/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/provinces/**", "/api/districts/**", "/api/wards/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/chat").permitAll()
                        // Admin-only endpoints
                        .requestMatchers(HttpMethod.DELETE, "/api/users/delete", "/api/users/disable").hasAuthority("ROLE_admin")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_admin")
                        .requestMatchers("/api/users/admin/**").hasAuthority("ROLE_admin")
                        .requestMatchers(HttpMethod.GET, "/api/events/pending").hasAuthority("ROLE_admin")
                        .requestMatchers(HttpMethod.GET, "/api/events").hasAuthority("ROLE_admin")
                        .requestMatchers(HttpMethod.GET, "/api/orders/list").hasAuthority("ROLE_admin")
                        .requestMatchers(HttpMethod.POST, "/api/orders/confirm-webhook", "/api/orders/confirm-order/**").hasAuthority("ROLE_admin")
                        .requestMatchers(HttpMethod.GET, "/api/orders/{orderId:[0-9]+}").hasAuthority("ROLE_admin")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{orderCode:[0-9]+}").hasAuthority("ROLE_admin")
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_admin")
                        // Organizer-only endpoints
                        .requestMatchers(HttpMethod.GET, "/api/events/by-organizer").hasAuthority("ROLE_organizer")
                        .requestMatchers(HttpMethod.POST, "/api/events").hasAuthority("ROLE_organizer")
                        .requestMatchers(HttpMethod.PUT, "/api/events/{eventId:[0-9]+}").hasAuthority("ROLE_organizer")
                        .requestMatchers("/api/organizer/**").hasAuthority("ROLE_organizer")
                        // Customer-only endpoints
                        .requestMatchers("/api/customer/**").hasAuthority("ROLE_customer")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exh -> exh
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"status\":401,\"message\":\"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"status\":403,\"message\":\"Forbidden\"}");
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
