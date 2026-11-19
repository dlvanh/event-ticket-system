package com.example.event_ticket_system.ValidLogin;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

public class LoginPasswordValidator implements ConstraintValidator<ValidLoginPassword, String> {

    // Danh sách các ký tự đặc biệt được cho phép
    private static final Set<Character> allowedSpecialChars = new HashSet<>();

    static {
        // Cập nhật danh sách theo yêu cầu của bạn
        for (char ch : "!@#$%^&*()_+-[]{};':\"\\|,.<>/?".toCharArray()) {
            allowedSpecialChars.add(ch);
        }
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Nếu password null, để các annotation khác (ví dụ @NotBlank) xử lý
        if (password == null) {
            return true;
        }
        for (char ch : password.toCharArray()) {
            // Nếu ký tự không phải là chữ cái hay số, kiểm tra xem có trong danh sách cho phép hay không
            if (!Character.isLetterOrDigit(ch) && !allowedSpecialChars.contains(ch)) {
                return false;
            }
        }
        return true;
    }
}
