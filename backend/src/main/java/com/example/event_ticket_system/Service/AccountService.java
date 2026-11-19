package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.Entity.User;

public interface AccountService {
    boolean existsByEmail(String email);
    void createAccount(User user);
    void updateAccount(User user);
    boolean existsByPhoneNumber(String phoneNumber);
    User findByEmail(String email);
}
