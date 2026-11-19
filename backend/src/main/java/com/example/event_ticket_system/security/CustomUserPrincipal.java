package com.example.event_ticket_system.security;

import com.example.event_ticket_system.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserPrincipal implements UserDetails {
    private final Integer userId;
    private final String email;
    private final String fullName;
    private final UserRole role;
    private final List<GrantedAuthority> authorities;

    public CustomUserPrincipal(Integer userId, String email, String fullName, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email != null && !email.isBlank() ? email : String.valueOf(userId);
    }

    public String getRoleAuthority() {
        return "ROLE_" + role.name();
    }
}
