package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.request.LoginRequestDTO;
import com.example.event_ticket_system.dto.request.OrganizerRequest;
import com.example.event_ticket_system.dto.request.RegisterRequestDTO;
import com.example.event_ticket_system.dto.request.ResetPasswordByCodeRequest;
import com.example.event_ticket_system.dto.request.SendCodeRequest;
import com.example.event_ticket_system.dto.request.VerifyCodeDTO;
import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.dto.response.LoginResponseDto;
import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.enums.UserStatus;
import com.example.event_ticket_system.exception.ApiException;
import com.example.event_ticket_system.repository.UserRepository;
import com.example.event_ticket_system.security.JwtUtil;
import com.example.event_ticket_system.service.AccountService;
import com.example.event_ticket_system.service.EmailService;
import com.example.event_ticket_system.service.UserService;
import com.example.event_ticket_system.service.VerificationCodeService;
import com.example.event_ticket_system.service.VerifiedEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final VerifiedEmailService verifiedEmailService;
    private final VerificationCodeService verificationCodeService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerAccount(@Valid @RequestBody RegisterRequestDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu và xác nhận mật khẩu không khớp");
        }
        if (accountService.existsByEmail(dto.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }
        if (!verifiedEmailService.isEmailVerified(dto.getEmail())) {
            throw new IllegalArgumentException("Email chưa được xác thực. Vui lòng xác thực email trước khi đăng ký.");
        }

        User user = new User();
        user.setEmail(dto.getEmail().trim());
        user.setPasswordHash(dto.getPassword());
        user.setFullName(dto.getFullName().trim());
        user.setRole(UserRole.customer);
        user.setStatus(UserStatus.active);
        accountService.createAccount(user);

        verifiedEmailService.removeVerifiedEmail(dto.getEmail());
        return ApiResponse.created(null, "Đăng ký thành công. Vui lòng đăng nhập.");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDTO dto) {
        User user = accountService.findByEmail(dto.getEmail());
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác");
        }
        if (user.getStatus() == null || user.getStatus() != UserStatus.active) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản không hoạt động hoặc đã bị khóa");
        }

        String token = jwtUtil.generateToken(user.getFullName(), user.getEmail(), user.getRole().name(), user.getId());
        LoginResponseDto payload = LoginResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return ApiResponse.ok(payload, "Đăng nhập thành công");
    }

    @RequestMapping(value = "/oauth2/google", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<ApiResponse<LoginResponseDto>> handleGoogleAuth(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Không thể xác thực người dùng");
        }

        String email = oauth2User.getAttribute("email");
        String fullName = oauth2User.getAttribute("name");
        String profilePicture = oauth2User.getAttribute("picture");

        if (email == null || fullName == null) {
            throw new IllegalArgumentException("Không thể lấy thông tin email hoặc tên từ Google");
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
            log.info("Registered new Google OAuth2 user");
        }

        if (user.getStatus() != UserStatus.active) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản không hoạt động.");
        }

        String token = jwtUtil.generateToken(user.getFullName(), user.getEmail(), user.getRole().name(), user.getId());
        LoginResponseDto payload = LoginResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        String message = isNewUser ? "Đăng ký và đăng nhập thành công" : "Đăng nhập thành công";
        return isNewUser ? ApiResponse.created(payload, message) : ApiResponse.ok(payload, message);
    }

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<Void>> sendPasswordResetCode(@Valid @RequestBody SendCodeRequest request) {
        User user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("Email không tồn tại");
        }

        String code = verificationCodeService.generateAndSaveCode(request.getEmail());
        emailService.sendVerificationEmail(request.getEmail(), code);
        return ApiResponse.ok(null, "Mã xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra email.");
    }

    @PostMapping("/reset-password-by-code")
    public ResponseEntity<ApiResponse<Void>> resetPasswordByCode(@Valid @RequestBody ResetPasswordByCodeRequest request) {
        boolean valid = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!valid) {
            throw new IllegalArgumentException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }

        User user = accountService.findByEmail(request.getEmail());
        if (user == null) {
            throw new IllegalArgumentException("Email không tồn tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        accountService.updateAccount(user);
        return ApiResponse.ok(null, "Mật khẩu đã được cập nhật thành công");
    }

    @PostMapping("/sendVerificationCode")
    public ResponseEntity<ApiResponse<Void>> sendRegistrationCode(@Valid @RequestBody SendCodeRequest request) {
        String email = request.getEmail();
        if (accountService.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã được sử dụng. Vui lòng sử dụng email khác.");
        }

        String code = verificationCodeService.generateAndSaveCode(email);
        emailService.sendVerificationEmail(email, code);
        return ApiResponse.ok(null, "Mã xác thực đã được gửi đến email của bạn. Vui lòng kiểm tra email.");
    }

    @PostMapping("/verifyCode")
    public ResponseEntity<ApiResponse<Void>> verifyCode(@Valid @RequestBody VerifyCodeDTO verifyCodeDTO) {
        String email = verifyCodeDTO.getEmail();
        boolean isValid = verificationCodeService.verifyCode(email, verifyCodeDTO.getCode());
        if (!isValid) {
            throw new IllegalArgumentException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }
        verifiedEmailService.markEmailVerified(email);
        return ApiResponse.ok(null, "Email xác thực thành công");
    }

    @PostMapping(value = "/register-organizer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> registerOrganizer(
            @RequestPart("data") @Valid OrganizerRequest organizerRequest,
            @RequestPart("profilePicture") MultipartFile profilePicture) {
        userService.registerOrganizer(profilePicture, organizerRequest);
        return ApiResponse.created(null, "Organizer registration request submitted successfully");
    }
}
