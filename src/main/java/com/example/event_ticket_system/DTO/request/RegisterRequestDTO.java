package com.example.event_ticket_system.DTO.request;

import com.example.event_ticket_system.ValidLogin.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public class RegisterRequestDTO {
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Định dạng email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @ValidPassword
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu là bắt buộc")
    private String confirmPassword;

    @NotBlank(message = "Họ và tên là bắt buộc")
    private String fullName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public  String getPassword() {
        return password;
    }

    public void setPassword( String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
