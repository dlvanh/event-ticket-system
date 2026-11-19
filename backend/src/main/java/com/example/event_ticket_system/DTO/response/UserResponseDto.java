package com.example.event_ticket_system.DTO.response;

import lombok.Data;

@Data
public class UserResponseDto {
    private Integer id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String role;
    private String status;
    private String gender;
    private String address;
    private String bio;
    private String avatarUrl;
    private String createdAt;
    private String updatedAt;
}
