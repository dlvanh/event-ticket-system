package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.LoginRequestDTO;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.AccountService;
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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest,
                                    BindingResult bindingResult) {
        // Kiểm tra lỗi validate từ DTO
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors()
                    .stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }

        // Tìm Account theo Email
        User user = accountService.findByEmail(loginRequest.getEmail());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email không tồn tại");
        }

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Mật khẩu không đúng");
        }

        //TODO: Kiểm tra trạng thái tài khoản

        // Tạo JWT token
        Long userId = Long.valueOf(user.getId());
        System.out.println(userId);
        String userRole = String.valueOf(user.getRole());
        String token = jwtUtil.generateToken(
            user.getFullName(),
            userRole,
            userId
        );

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("message", "Đăng nhập thành công");

        //Đăng nhập thành công
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
