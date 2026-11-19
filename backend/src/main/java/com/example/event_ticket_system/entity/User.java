package com.example.event_ticket_system.entity;

import com.example.event_ticket_system.enums.Gender;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Size(max = 100)
    @NotNull
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @JsonIgnore
    @Size(max = 255)
    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ColumnDefault("'customer'")
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.customer;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Size(max = 255)
    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.active;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "address")
    private String address;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}