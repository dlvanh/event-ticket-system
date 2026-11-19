package com.example.event_ticket_system.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class OrganizerRequest {

    @NotBlank(message = "Tên không được để trống")
    @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Định dạng email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @Size(max = 500, message = "Tiểu sử không được vượt quá 500 ký tự")
    @NotBlank(message = "Tiểu sử không được để trống")
    private String bio;

    private MultipartFile profilePicture;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 12, message = "Mật khẩu phải từ 12 trở lên")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;
}
