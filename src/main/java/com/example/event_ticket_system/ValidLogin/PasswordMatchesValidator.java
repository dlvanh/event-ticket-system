package com.example.event_ticket_system.ValidLogin;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String firstFieldName;
    private String secondFieldName;
    private String message;

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        this.firstFieldName = constraintAnnotation.first();
        this.secondFieldName = constraintAnnotation.second();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Lấy giá trị của 2 trường được chỉ định trong DTO thông qua reflection
        Object firstObj = new BeanWrapperImpl(value).getPropertyValue(firstFieldName);
        Object secondObj = new BeanWrapperImpl(value).getPropertyValue(secondFieldName);

        boolean valid = true;
        // Tắt thông báo mặc định
        context.disableDefaultConstraintViolation();

        // Kiểm tra null cho từng trường (nếu chưa được validate bởi các annotation riêng)
        if (firstObj == null) {
            context.buildConstraintViolationWithTemplate(firstFieldName + " không được để trống")
                    .addPropertyNode(firstFieldName)
                    .addConstraintViolation();
            valid = false;
        }
        if (secondObj == null) {
            context.buildConstraintViolationWithTemplate(secondFieldName + " không được để trống")
                    .addPropertyNode(secondFieldName)
                    .addConstraintViolation();
            valid = false;
        }
        if (!valid) {
            return false;
        }

        // Kiểm tra xem giá trị của 2 trường có bằng nhau không
        if (!firstObj.equals(secondObj)) {
            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode(secondFieldName)
                    .addConstraintViolation();
            valid = false;
        }
        return valid;
    }
}
