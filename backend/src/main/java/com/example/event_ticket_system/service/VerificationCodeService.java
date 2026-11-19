package com.example.event_ticket_system.service;

import com.example.event_ticket_system.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private static final String CODE_KEY_PREFIX = "otp:code:";
    private static final String COOLDOWN_KEY_PREFIX = "otp:cooldown:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp:attempts:";

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAndSaveCode(String email) {
        String normalizedEmail = normalize(email);
        String cooldownKey = COOLDOWN_KEY_PREFIX + normalizedEmail;

        Boolean acquiredCooldown = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", RESEND_COOLDOWN);
        if (Boolean.FALSE.equals(acquiredCooldown)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Vui lòng chờ 60 giây trước khi yêu cầu gửi lại mã xác thực."
            );
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + normalizedEmail, code, CODE_TTL);
        redisTemplate.delete(ATTEMPTS_KEY_PREFIX + normalizedEmail);

        log.info("Generated OTP verification code for email {} (TTL {}s)", maskEmail(normalizedEmail), CODE_TTL.toSeconds());
        return code;
    }

    public boolean verifyCode(String email, String providedCode) {
        if (email == null || providedCode == null || providedCode.isBlank()) {
            return false;
        }
        String normalizedEmail = normalize(email);
        String codeKey = CODE_KEY_PREFIX + normalizedEmail;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + normalizedEmail;

        String expectedCode = redisTemplate.opsForValue().get(codeKey);
        if (expectedCode == null) {
            log.warn("No active OTP found for email {}", maskEmail(normalizedEmail));
            return false;
        }

        boolean matched = MessageDigest.isEqual(
                expectedCode.getBytes(StandardCharsets.UTF_8),
                providedCode.trim().getBytes(StandardCharsets.UTF_8)
        );

        if (matched) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            redisTemplate.delete(COOLDOWN_KEY_PREFIX + normalizedEmail);
            log.info("OTP verified successfully for email {}", maskEmail(normalizedEmail));
            return true;
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, CODE_TTL);
        }
        if (attempts != null && attempts >= MAX_VERIFY_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            redisTemplate.delete(attemptsKey);
            log.warn("OTP invalidated after {} failed attempts for email {}", attempts, maskEmail(normalizedEmail));
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã nhập sai mã xác thực quá 5 lần. Vui lòng yêu cầu mã mới."
            );
        }

        log.warn("Invalid OTP attempt ({}/{}) for email {}", attempts, MAX_VERIFY_ATTEMPTS, maskEmail(normalizedEmail));
        return false;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***";
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
