package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Mapper.AccountMapper;
import com.example.event_ticket_system.Service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {
    private final AccountMapper accountMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AccountServiceImpl(AccountMapper accountMapper, BCryptPasswordEncoder passwordEncoder) {
        this.accountMapper = accountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findByEmail(String email) {
        return accountMapper.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return accountMapper.findByEmail(email) != null;
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return accountMapper.findByPhoneNumber(phoneNumber) != null;
    }

    @Override
    public void createAccount(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPasswordHash());
        user.setPasswordHash(encodedPassword);
        accountMapper.insertAccount(user);
    }

    @Override
    public void updateAccount(User user) {
        accountMapper.updatePassword(user);
    }

}
