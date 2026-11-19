package com.example.event_ticket_system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VerifiedEmailService {

    private static final Duration VERIFIED_TTL = Duration.ofMinutes(15);
    private static final String VERIFIED_KEY_PREFIX = "otp:verified:";

    private final StringRedisTemplate redisTemplate;

    public void markEmailVerified(String email) {
        redisTemplate.opsForValue().set(key(email), "true", VERIFIED_TTL);
    }

    public boolean isEmailVerified(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(email)));
    }

    public void removeVerifiedEmail(String email) {
        if (email != null && !email.isBlank()) {
            redisTemplate.delete(key(email));
        }
    }

    private String key(String email) {
        return VERIFIED_KEY_PREFIX + email.trim().toLowerCase(Locale.ROOT);
    }
}
