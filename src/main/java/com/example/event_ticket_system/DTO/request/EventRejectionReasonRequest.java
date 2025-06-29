package com.example.event_ticket_system.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class EventRejectionReasonRequest {
    @NotBlank(message = "Lý do không được để trống")
    @Size(max = 255, message = "Lý do không được vượt quá 255 ký tự")
    private String rejectionReason;
}
