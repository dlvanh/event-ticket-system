package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.ResetPasswordByCodeRequest;
import com.example.event_ticket_system.DTO.request.SendCodeRequest;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Service.AccountService;
import com.example.event_ticket_system.Service.EmailService;
import com.example.event_ticket_system.Service.VerificationCodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PasswordResetController {

    private final AccountService accountService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;

    @Autowired
    public PasswordResetController(AccountService accountService,
                                   BCryptPasswordEncoder passwordEncoder,
                                   EmailService emailService,
                                   VerificationCodeService verificationCodeService) {
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.verificationCodeService = verificationCodeService;
    }

    // Endpoint gửi mã xác thực đến email của người dùng (giữ nguyên)
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendCodeRequest request) {
        User user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email không tồn tại");
        }

        // Tạo mã xác thực và lưu trữ
        String code = verificationCodeService.generateAndSaveCode(request.getEmail());
        emailService.sendVerificationEmail(request.getEmail(), code);

        return ResponseEntity.ok("Mã xác thực đã được gửi đến email của bạn.");
    }

    // Endpoint đặt lại mật khẩu bằng mã xác thực, sử dụng ResetPasswordByCodeRequest với validate tự động
    @PostMapping("/reset-password-by-code")
    public ResponseEntity<?> resetPasswordByCode(@Valid @RequestBody ResetPasswordByCodeRequest request,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        // Kiểm tra mã xác thực
        boolean valid = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mã xác thực không hợp lệ hoặc đã hết hạn.");
        }

        User user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không tìm thấy tài khoản với email đã cung cấp.");
        }

        // Cập nhật mật khẩu mới (mã hóa trước khi lưu)
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountService.updateAccount(user);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công!");
    }
}
