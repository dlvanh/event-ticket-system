package com.example.event_ticket_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmWebhookRequestBody {
    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;
}
