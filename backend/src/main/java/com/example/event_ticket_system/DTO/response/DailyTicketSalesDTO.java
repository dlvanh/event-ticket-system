package com.example.event_ticket_system.DTO.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DailyTicketSalesDTO {
    private LocalDate date;
    private Long totalTicketSold;

    // Constructor cho projection từ query
    public DailyTicketSalesDTO(LocalDate date, Long totalTicketSold) {
        this.date = date;
        this.totalTicketSold = totalTicketSold;
    }
}
