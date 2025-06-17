package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.RegisterRequestDTO;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Service.AccountService;
import com.example.event_ticket_system.Service.VerifiedEmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final AccountService accountService;
    private final VerifiedEmailService verifiedEmailService;

    @Autowired
    public RegistrationController(AccountService accountService, VerifiedEmailService verifiedEmailService) {
        this.accountService = accountService;
        this.verifiedEmailService = verifiedEmailService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerAccount(@Valid @RequestBody RegisterRequestDTO registerRequestDTO, BindingResult bindingResult) {
        try {
            // Kiểm tra lỗi validate từ đầu vào
            if (bindingResult.hasErrors()) {
                List<String> errors = bindingResult.getFieldErrors()
                        .stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            // Kiểm tra confirmPassword
            if (!registerRequestDTO.getPassword().equals(registerRequestDTO.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mật khẩu xác nhận không khớp");
            }
            // Kiểm tra xem email đã được sử dụng hay chưa
            if (accountService.existsByEmail(registerRequestDTO.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Email đã được sử dụng");
            }

            // Kiểm tra xem email đã được xác thực chưa
            if (!verifiedEmailService.isEmailVerified(registerRequestDTO.getEmail())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Email chưa được xác thực. Vui lòng xác thực email trước khi đăng ký.");
            }

            // Tạo tài khoản mới (mật khẩu sẽ được mã hóa trong service)
            User user = new User();
            user.setEmail(registerRequestDTO.getEmail());
            user.setPasswordHash(registerRequestDTO.getPassword());
            user.setFullName(registerRequestDTO.getFullName());
            user.setRole(com.example.event_ticket_system.Enums.UserRole.customer);
            accountService.createAccount(user);

            // Sau khi đăng ký thành công, xóa email khỏi danh sách đã xác thực
            verifiedEmailService.removeVerifiedEmail(registerRequestDTO.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body("Tạo tài khoản thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã xảy ra lỗi trong quá trình đăng ký: " + e.getMessage());
        }
    }
}
