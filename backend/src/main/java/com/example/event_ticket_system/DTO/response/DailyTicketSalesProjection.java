package com.example.event_ticket_system.DTO.response;

import java.time.LocalDate;

public interface DailyTicketSalesProjection {
    LocalDate getDate();
    Long getTotalTicketSold();
}
