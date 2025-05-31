package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.ResetPasswordByCodeRequest;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Service.AccountService;
import com.example.event_ticket_system.Service.VerificationCodeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;
    private final VerificationCodeService verificationCodeService;

    @Autowired
    public PasswordResetController(AccountService accountService,
                                   BCryptPasswordEncoder passwordEncoder,
                                   JavaMailSender mailSender,
                                   VerificationCodeService verificationCodeService) {
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.verificationCodeService = verificationCodeService;
    }

    // Endpoint gửi mã xác thực đến email của người dùng (giữ nguyên)
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody com.example.event_ticket_system.DTO.SendCodeRequest request) {
        User user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email không tồn tại");
        }

        // Tạo mã xác thực và lưu trữ
        String code = verificationCodeService.generateAndSaveCode(request.getEmail());

        // Gửi email chứa mã xác thực
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(request.getEmail());
        mailMessage.setSubject("Mã xác thực đặt lại mật khẩu");
        mailMessage.setText("Mã xác thực của bạn là: " + code + "\nMã này có hiệu lực trong 5 phút.");
        mailSender.send(mailMessage);

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
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountService.updateAccount(user);

        return ResponseEntity.ok("Đặt lại mật khẩu thành công!");
    }
}
