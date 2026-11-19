package com.example.event_ticket_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String token;
    private Integer userId;
    private String fullName;
    private String email;
    private String role;
}
