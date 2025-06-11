package com.example.event_ticket_system.DTO.request;
import com.example.event_ticket_system.ValidLogin.PasswordMatches;
import com.example.event_ticket_system.ValidLogin.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@PasswordMatches(first = "newPassword", second = "confirmNewPassword", message = "Mật khẩu không khớp")
public class ResetPasswordByCodeRequest {
    @NotBlank(message = "Email không được bỏ trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mã xác thực không được bỏ trống")
    private String code;

    @NotBlank(message = "Mật khẩu mới không được bỏ trống")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Xác nhận mật khẩu không được bỏ trống")
    private String confirmNewPassword;

    // getters & setters
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }
}
