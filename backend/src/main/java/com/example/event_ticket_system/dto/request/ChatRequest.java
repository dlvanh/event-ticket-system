package com.example.event_ticket_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatRequest {
    @NotBlank(message = "Message must not be blank")
    private String message;
}
