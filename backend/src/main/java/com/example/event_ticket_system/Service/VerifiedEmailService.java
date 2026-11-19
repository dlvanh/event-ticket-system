package com.example.event_ticket_system.Service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerifiedEmailService {
    private final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();

    public void markEmailVerified(String email) {
        verifiedEmails.add(email);
    }

    public boolean isEmailVerified(String email) {
        return verifiedEmails.contains(email);
    }

    public void removeVerifiedEmail(String email) {
        verifiedEmails.remove(email);
    }
}
