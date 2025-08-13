package com.example.event_ticket_system.DTO.response;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DailyTicketTypeSalesDTO {
    private LocalDate date;
    private String ticketType;
    private Long totalQuantity;

    public DailyTicketTypeSalesDTO(LocalDate date, String ticketType, Long totalQuantity) {
        this.date = date;
        this.ticketType = ticketType;
        this.totalQuantity = totalQuantity;
    }
}
