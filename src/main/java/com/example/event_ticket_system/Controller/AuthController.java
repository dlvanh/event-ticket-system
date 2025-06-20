package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.*;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Enums.UserStatus;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.AccountService;
import com.example.event_ticket_system.Service.EmailService;
import com.example.event_ticket_system.Service.VerificationCodeService;
import com.example.event_ticket_system.Service.VerifiedEmailService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private VerifiedEmailService verifiedEmailService;

    @Autowired
    private VerificationCodeService verificationCodeService;

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
            user.setRole(UserRole.customer);
            accountService.createAccount(user);

            // Sau khi đăng ký thành công, xóa email khỏi danh sách đã xác thực
            verifiedEmailService.removeVerifiedEmail(registerRequestDTO.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body("Tạo tài khoản thành công");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Đã xảy ra lỗi trong quá trình đăng ký: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO,
                                    BindingResult bindingResult) {
        try {
            // Kiểm tra lỗi validate
            if (bindingResult.hasErrors()) {
                List<String> errors = bindingResult.getFieldErrors()
                        .stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .collect(Collectors.toList());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
            }

            // Tìm user
            User user = accountService.findByEmail(loginRequestDTO.getEmail());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email không tồn tại.");
            }

            // Kiểm tra mật khẩu
            if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mật khẩu không đúng.");
            }

            // Kiểm tra trạng thái tài khoản
            if (user.getStatus() == null || user.getStatus() != UserStatus.active) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản không hoạt động.");
            }

            // Tạo token
            String token = jwtUtil.generateToken(
                    user.getFullName(),
                    user.getRole().toString(),
                    user.getId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Đăng nhập thành công");

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/login/oauth2/google")
    public ResponseEntity<?> loginWithGoogle(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            if (oauth2User == null) {
                return APIResponse.responseBuilder(
                        null,
                        "Không thể xác thực người dùng",
                        HttpStatus.UNAUTHORIZED);
            }

            String email = oauth2User.getAttribute("email");
            String fullName = oauth2User.getAttribute("name");

            // Tìm hoặc tạo user
            User user = accountService.findByEmail(email);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setRole(UserRole.customer);
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // Mật khẩu ngẫu nhiên cho OAuth2
                user.setStatus(UserStatus.active);
                accountService.createAccount(user);
            }

            // Kiểm tra trạng thái
            if (user.getStatus() != UserStatus.active) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Tài khoản không hoạt động.");
            }

            // Tạo token
            String token = jwtUtil.generateToken(
                    user.getFullName(),
                    user.getRole().toString(),
                    user.getId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Đăng nhập thành công");

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
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
        log.info("Nhận yêu cầu xác thực cho email {} với mã: {}", email, code);

        boolean isValid = verificationCodeService.verifyCode(email, code);
        if (isValid) {
            log.info("Email {} đã xác thực thành công", email);
            // Đánh dấu email đã xác thực
            verifiedEmailService.markEmailVerified(email);

            // Trả về JSON kiểu { "message": "Email xác thực thành công" }
            Map<String, String> response = new HashMap<>();
            response.put("message", "Email xác thực thành công");
            return ResponseEntity.status(HttpStatus.OK).body(response);

        } else {
            log.warn("Mã xác thực không hợp lệ hoặc đã hết hạn cho email {}", email);

            // Trả về JSON kiểu { "error": "Mã xác thực không hợp lệ..." } + status 400
            Map<String, String> response = new HashMap<>();
            response.put("error", "Mã xác thực không hợp lệ hoặc đã hết hạn");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
