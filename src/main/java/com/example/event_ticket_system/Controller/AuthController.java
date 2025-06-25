package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.*;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Enums.UserStatus;
import com.example.event_ticket_system.Repository.UserRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountService accountService;

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
                return APIResponse.responseBuilder(
                        errors,
                        "Lỗi validate đầu vào",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Kiểm tra confirmPassword
            if (!registerRequestDTO.getPassword().equals(registerRequestDTO.getConfirmPassword())) {
                return APIResponse.responseBuilder(
                                null,
                                "Mật khẩu và xác nhận mật khẩu không khớp",
                                HttpStatus.BAD_REQUEST
                        );
            }
            // Kiểm tra xem email đã được sử dụng hay chưa
            if (accountService.existsByEmail(registerRequestDTO.getEmail())) {
                return APIResponse.responseBuilder(
                        null,
                        "Email đã được sử dụng",
                        HttpStatus.CONFLICT
                );
            }

            // Kiểm tra xem email đã được xác thực chưa
            if (!verifiedEmailService.isEmailVerified(registerRequestDTO.getEmail())) {
                return APIResponse.responseBuilder(
                        null,
                        "Email chưa được xác thực. Vui lòng xác thực email trước khi đăng ký.",
                        HttpStatus.BAD_REQUEST
                );
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

            return APIResponse.responseBuilder(
                    null,
                    "Đăng ký thành công. Vui lòng đăng nhập.",
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "Lỗi hệ thống: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
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
                return APIResponse.responseBuilder(
                        errors,
                        "Lỗi validate đầu vào",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Tìm user
            User user = accountService.findByEmail(loginRequestDTO.getEmail());
            if (user == null) {
                return APIResponse.responseBuilder(
                        null,
                        "Tài khoản không tồn tại",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Kiểm tra mật khẩu
            if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPasswordHash())) {
                return APIResponse.responseBuilder(
                        null,
                        "Mật khẩu không chính xác",
                        HttpStatus.UNAUTHORIZED
                );
            }

            // Kiểm tra trạng thái tài khoản
            if (user.getStatus() == null || user.getStatus() != UserStatus.active) {
                return APIResponse.responseBuilder(
                        null,
                        "Tài khoản không hoạt động hoặc đã bị khóa",
                        HttpStatus.FORBIDDEN
                );
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
            return APIResponse.responseBuilder(
                    null,
                    "Lỗi hệ thống: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @RequestMapping(value = "/oauth2/google", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> handleGoogleAuth(@AuthenticationPrincipal OAuth2User oauth2User) {
        try {
            if (oauth2User == null) {
                return APIResponse.responseBuilder(
                        null,
                        "Không thể xác thực người dùng",
                        HttpStatus.UNAUTHORIZED
                );
            }

            String email = oauth2User.getAttribute("email");
            String fullName = oauth2User.getAttribute("name");
            String profilePicture = oauth2User.getAttribute("picture");

            log.info("Google auth request for email: {}", email);

            if (email == null || fullName == null) {
                return APIResponse.responseBuilder(
                        null,
                        "Không thể lấy thông tin email hoặc tên từ Google",
                        HttpStatus.BAD_REQUEST
                );
            }

            User user = accountService.findByEmail(email);
            boolean isNewUser = false;
            if (user == null) {
                isNewUser = true;
                user = new User();
                user.setEmail(email);
                user.setFullName(fullName);
                user.setRole(UserRole.customer);
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setStatus(UserStatus.active);
                user.setProfilePicture(profilePicture);
                user.setCreatedAt(Instant.now());
                userRepository.save(user);
                log.info("User registered successfully via Google: {}", email);
            }

            if (user.getStatus() != UserStatus.active) {
                return APIResponse.responseBuilder(
                        null,
                        "Tài khoản không hoạt động.",
                        HttpStatus.FORBIDDEN
                );
            }

            String token = jwtUtil.generateToken(user.getFullName(), user.getRole().toString(), user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", isNewUser ? "Đăng ký và đăng nhập thành công" : "Đăng nhập thành công");
            log.info("User {} via Google: {}", isNewUser ? "registered and logged in" : "logged in", email);

            return APIResponse.responseBuilder(
                    response,
                    response.get("message").toString(),
                    isNewUser ? HttpStatus.CREATED : HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error during Google auth: {}", e.getMessage());
            return APIResponse.responseBuilder(
                    null,
                    "Lỗi hệ thống: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    // Endpoint gửi mã xác thực đến email của người dùng (giữ nguyên)
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody SendCodeRequest request) {
        User user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            return APIResponse.responseBuilder(
                    null,
                    "Email không tồn tại",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Tạo mã xác thực và lưu trữ
        String code = verificationCodeService.generateAndSaveCode(request.getEmail());
        emailService.sendVerificationEmail(request.getEmail(), code);

        return APIResponse.responseBuilder(
                        null,
                        "Mã xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra email.",
                        HttpStatus.OK
                );
    }

    @PostMapping("/reset-password-by-code")
    public ResponseEntity<?> resetPasswordByCode(@Valid @RequestBody ResetPasswordByCodeRequest request,
                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            return APIResponse.responseBuilder(
                    errors,
                    "Lỗi validate đầu vào",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Kiểm tra mã xác thực
        boolean valid = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!valid) {
            return APIResponse.responseBuilder(
                    null,
                    "Mã xác thực không hợp lệ hoặc đã hết hạn",
                    HttpStatus.BAD_REQUEST
            );
        }

        User user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            return APIResponse.responseBuilder(
                    null,
                    "Email không tồn tại",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Cập nhật mật khẩu mới (mã hóa trước khi lưu)
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountService.updateAccount(user);

        return APIResponse.responseBuilder(
                null,
                "Mật khẩu đã được cập nhật thành công",
                HttpStatus.OK
        );
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
            return APIResponse.responseBuilder(
                    errors,
                    "Lỗi validate đầu vào",
                    HttpStatus.BAD_REQUEST
            );
        }

        String email = request.getEmail();

        // Kiểm tra xem email đã tồn tại trong hệ thống chưa
        if (accountService.existsByEmail(request.getEmail())) {
            return APIResponse.responseBuilder(
                    null,
                    "Email đã được sử dụng. Vui lòng sử dụng email khác.",
                    HttpStatus.CONFLICT
            );
        }

        // Tạo và lưu mã xác thực cho email
        String code = verificationCodeService.generateAndSaveCode(email);
        emailService.sendVerificationEmail(email, code);

        return APIResponse.responseBuilder(
                null,
                "Mã xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra email.",
                HttpStatus.OK
        );
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

    @PostMapping(value = "/register-organizer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> registerOrganizer(@ModelAttribute OrganizerRequest organizerRequest) {
        try {
            userService.registerOrganizer(organizerRequest.getProfilePicture(), organizerRequest);
            return APIResponse.responseBuilder(
                    null,
                    "Organizer registration request submitted successfully",
                    HttpStatus.CREATED
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Unexpected error during organizer registration", e);
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while registering organizer",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
