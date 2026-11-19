package com.example.event_ticket_system.ValidLogin;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = LoginPasswordValidator.class)
@Target({ FIELD })
@Retention(RUNTIME)
public @interface ValidLoginPassword {
    String message() default "Password chứa ký tự không hợp lệ";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
