package com.example.event_ticket_system.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vn.payos.PayOS;

@Configuration
public class    WebConfig implements WebMvcConfigurer {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${account.dir}")
    private String accountDir;

    @Value("${PAYOS_CLIENT_ID}")
    private String clientId;

    @Value("${PAYOS_API_KEY}")
    private String apiKey;

    @Value("${PAYOS_CHECKSUM_KEY}")
    private String checksumKey;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200",
                "http://10.0.2.2:8081")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/upload/**")
                .addResourceLocations("file:" + uploadDir);
        registry.addResourceHandler("/images/account/**")
                .addResourceLocations("file:" + accountDir);
    }

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
