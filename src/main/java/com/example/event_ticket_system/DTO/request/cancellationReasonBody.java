package com.example.event_ticket_system.DTO.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class cancellationReasonBody {
    private String cancellationReason;
}
