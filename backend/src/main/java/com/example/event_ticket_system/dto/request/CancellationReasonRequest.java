package com.example.event_ticket_system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancellationReasonRequest {
    private String cancellationReason;
}
