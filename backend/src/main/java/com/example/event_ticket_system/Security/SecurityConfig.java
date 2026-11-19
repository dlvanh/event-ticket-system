package com.example.event_ticket_system.Security;

import com.example.event_ticket_system.Service.Impl.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    private final JwtFilter jwtFilter;

    @Autowired
    public SecurityConfig(JwtFilter jwtFilter, CustomOAuth2UserService customOAuth2UserService) {
        this.jwtFilter = jwtFilter;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Authentication endpoints
                        .requestMatchers(
                                "/oauth2/authorization/google",
                                "/login/oauth2/code/google"
                        ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Role-based endpoints
                        .requestMatchers("/api/payment/**").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/events/recommend").permitAll()
                        .requestMatchers("/api/events/by-organizer").authenticated()
                        .requestMatchers("/api/events").authenticated()
                        .requestMatchers("/api/events/*").permitAll()
                        .requestMatchers("/api/chat").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_admin")
                        .requestMatchers("/api/customer/**").hasAuthority("ROLE_customer")
                        .requestMatchers("/api/organizer/**").hasAuthority("ROLE_organizer")
                        .requestMatchers("/api/review/upload/{reviewId}",
                                "/api/review/update/{reviewId}",
                                "/api/review/delete/{reviewId}").authenticated()
                        .requestMatchers("/api/review/event/{eventId}",
                                "/api/review/user/{userId}").permitAll()

                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            log.info("OAuth2 login successful for user: {}", authentication.getPrincipal());
                            response.sendRedirect("/api/auth/oauth2/google");
                        })
                        .failureHandler((request, response, exception) -> {
                            log.error("OAuth2 login failed: {}", exception.getMessage());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("OAuth2 login failed: " + exception.getMessage());
                        })
                )
                .exceptionHandling(exh -> exh
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:4200"); // Cho phép gọi từ origin này
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true); // Nếu bạn cần gửi cookie hoặc thông tin xác thực khác

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // áp dụng cho mọi URL
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
