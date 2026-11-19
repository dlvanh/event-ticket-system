package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.enums.UserStatus;
import com.example.event_ticket_system.repository.UserRepository;
import com.example.event_ticket_system.service.AccountService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    @Transactional
    public void createAccount(User user) {
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        if (user.getRole() == null) {
            user.setRole(UserRole.customer);
        }
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.active);
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now());
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateAccount(User user) {
        User existing = userRepository.findByEmail(user.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + user.getEmail()));
        existing.setPasswordHash(user.getPasswordHash());
        userRepository.save(existing);
    }
}
