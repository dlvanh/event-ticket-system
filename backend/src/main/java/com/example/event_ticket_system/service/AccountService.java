package com.example.event_ticket_system.service;

import com.example.event_ticket_system.entity.User;

public interface AccountService {
    boolean existsByEmail(String email);
    void createAccount(User user);
    void updateAccount(User user);
    boolean existsByPhoneNumber(String phoneNumber);
    User findByEmail(String email);
}
