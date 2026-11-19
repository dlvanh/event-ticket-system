package com.example.event_ticket_system.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationCodeService {
    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeService.class);
    private final Map<String, CodeInfo> codeCache = new ConcurrentHashMap<>();
    private static final int EXPIRATION_MINUTES = 5; // mã có hiệu lực 5 phút
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAndSaveCode(String email) {
        // Tạo mã ngẫu nhiên 6 chữ số
        String code = String.format("%06d", secureRandom.nextInt(1000000));
        LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        codeCache.put(email, new CodeInfo(code, expirationTime));
        logger.info("Generated verification code {} for email {}. Expires at {}", code, email, expirationTime);
        return code;
    }

    public boolean verifyCode(String email, String code) {
        CodeInfo info = codeCache.get(email);
        if (info == null) {
            logger.warn("No verification code found for email {}", email);
            return false;
        }
        // Kiểm tra nếu mã đã hết hạn
        if (LocalDateTime.now().isAfter(info.getExpirationTime())) {
            logger.warn("Verification code for email {} has expired", email);
            codeCache.remove(email);
            return false;
        }
        // Kiểm tra mã có khớp hay không
        if (info.getCode().equals(code)) {
            logger.info("Verification code for email {} is valid", email);
            codeCache.remove(email);
            return true;
        } else {
            logger.warn("Invalid verification code {} for email {}. Expected: {}", code, email, info.getCode());
            return false;
        }
    }

    private static class CodeInfo {
        private final String code;
        private final LocalDateTime expirationTime;

        public CodeInfo(String code, LocalDateTime expirationTime) {
            this.code = code;
            this.expirationTime = expirationTime;
        }

        public String getCode() {
            return code;
        }

        public LocalDateTime getExpirationTime() {
            return expirationTime;
        }
    }
}
