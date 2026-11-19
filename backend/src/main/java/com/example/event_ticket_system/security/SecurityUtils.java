package com.example.event_ticket_system.security;

import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static CustomUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal;
    }

    public static Integer getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static UserRole getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    public static boolean hasRole(UserRole role) {
        return getCurrentUser().getRole() == role;
    }
}
