package com.example.event_ticket_system.ValidLogin;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // Định nghĩa danh sách các ký tự đặc biệt được cho phép
    private static final Set<Character> allowedSpecialChars = new HashSet<>();
    static {
        // Lưu ý: Danh sách dưới đây là các ký tự đặc biệt cho phép. Bạn có thể điều chỉnh nếu cần.
        for (char ch : "!@#$%^&*()_+-[]{};':\"\\|,.<>/?".toCharArray()) {
            allowedSpecialChars.add(ch);
        }
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Nếu password null thì báo lỗi ngay
        if (password == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password không được để trống")
                    .addConstraintViolation();
            return false;
        }

        List<String> errors = new ArrayList<>();

        // Kiểm tra theo thứ tự mong muốn:
        if (password.length() < 10) {
            errors.add("10 ký tự");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            errors.add("1 ký tự đặc biệt");
        }
        if (!password.matches(".*[A-Z].*")) {
            errors.add("1 chữ hoa");
        }
        if (!password.matches(".*\\d.*")) {
            errors.add("1 số");
        }
        // Kiểm tra ký tự đặc biệt hợp lệ: nếu phát hiện ký tự không hợp lệ thì báo lỗi
        for (char c : password.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                if (!allowedSpecialChars.contains(c)) {
                    errors.add("Ký tự không hợp lệ: " + c);
                }
            }
        }

        // Nếu không có lỗi, trả về true
        if (errors.isEmpty()) {
            return true;
        } else {
            // Nếu có lỗi, kết hợp tất cả thông báo lỗi thành một chuỗi
            String combinedMessage = String.join(", ", errors);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Phải có ít nhất " + combinedMessage)
                    .addConstraintViolation();
            return false;
        }
    }
}
