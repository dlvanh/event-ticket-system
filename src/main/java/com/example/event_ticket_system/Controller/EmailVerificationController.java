package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.SendCodeRequest;
import com.example.event_ticket_system.DTO.request.VerifyCodeDTO;
import com.example.event_ticket_system.Service.AccountService;
import com.example.event_ticket_system.Service.EmailService;
import com.example.event_ticket_system.Service.VerificationCodeService;
import com.example.event_ticket_system.Service.VerifiedEmailService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EmailVerificationController {
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationController.class);

    private final VerificationCodeService verificationCodeService;
    private final EmailService emailService;
    private final VerifiedEmailService verifiedEmailService;
    private final AccountService accountService;

    @Autowired
    public EmailVerificationController(VerificationCodeService verificationCodeService,
                                       EmailService emailService,
                                       VerifiedEmailService verifiedEmailService,
                                       AccountService accountService) {
        this.verificationCodeService = verificationCodeService;
        this.emailService = emailService;
        this.verifiedEmailService = verifiedEmailService;
        this.accountService = accountService;
    }

    // API gửi mã xác thực đến email
    @PostMapping("/sendVerificationCode")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendCodeRequest request,
                                                  BindingResult bindingResult) {
        // Kiểm tra lỗi validate đầu vào
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        String email = request.getEmail();

        // Kiểm tra xem email đã tồn tại trong hệ thống chưa
        if (accountService.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email đã được sử dụng");
        }

        // Tạo và lưu mã xác thực cho email
        String code = verificationCodeService.generateAndSaveCode(email);
        emailService.sendVerificationEmail(email, code);

        return ResponseEntity.ok("Mã xác thực đã được gửi tới email. Vui lòng kiểm tra email của bạn.");
    }

    // API xác thực mã
    @PostMapping("/verifyCode")
    public ResponseEntity<Map<String, String>> verifyCode(@RequestBody VerifyCodeDTO verifyCodeDTO) {
        String email = verifyCodeDTO.getEmail();
        String code = verifyCodeDTO.getCode();
        logger.info("Nhận yêu cầu xác thực cho email {} với mã: {}", email, code);

        boolean isValid = verificationCodeService.verifyCode(email, code);
        if (isValid) {
            logger.info("Email {} đã xác thực thành công", email);
            // Đánh dấu email đã xác thực
            verifiedEmailService.markEmailVerified(email);

            // Trả về JSON kiểu { "message": "Email xác thực thành công" }
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email xác thực thành công");
            return ResponseEntity.ok(response);

        } else {
            logger.warn("Mã xác thực không hợp lệ hoặc đã hết hạn cho email {}", email);

            // Trả về JSON kiểu { "error": "Mã xác thực không hợp lệ..." } + status 400
            Map<String, String> response = new HashMap<>();
            response.put("error", "Mã xác thực không hợp lệ hoặc đã hết hạn");
            return ResponseEntity.badRequest().body(response);
        }
    }

}
