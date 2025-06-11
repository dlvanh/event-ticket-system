package com.example.event_ticket_system.Entity;

import com.example.event_ticket_system.Enums.Gender;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Setter
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Setter
    @Getter
    @Size(max = 100)
    @NotNull
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Setter
    @Getter
    @Size(max = 100)
    @NotNull
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Setter
    @Getter
    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Setter
    @Getter
    @Size(max = 255)
    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Setter
    @Getter
    @ColumnDefault("'customer'")
    @Lob
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Setter
    @Getter
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Setter
    @Getter
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    public @Size(max = 255) String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(@Size(max = 255) String profilePicture) {
        this.profilePicture = profilePicture;
    }

    @Size(max = 255)
    @Column(name = "profile_picture")
    private String profilePicture;

    @Getter
    @Setter
    @Column(name= "bio", length = 500)
    private String bio;

    @Setter
    @Getter
    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.active;

    @Setter
    @Getter
    @Column(name="gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Setter
    @Getter
    @Column(name = "address")
    private String address;
}