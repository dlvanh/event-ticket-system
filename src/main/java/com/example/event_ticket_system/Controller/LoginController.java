package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.LoginRequestDTO;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Enums.UserStatus;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final AccountService accountService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public LoginController(AccountService accountService, BCryptPasswordEncoder passwordEncoder, JwtUtil JwtUtil) {
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = JwtUtil;
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
                user.setRole(com.example.event_ticket_system.Enums.UserRole.customer);
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
}
