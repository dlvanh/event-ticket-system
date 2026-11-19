package com.example.event_ticket_system.dto.response;

import java.time.LocalDate;

public interface DailyTicketSalesProjection {
    LocalDate getDate();
    Long getTotalTicketSold();
}
