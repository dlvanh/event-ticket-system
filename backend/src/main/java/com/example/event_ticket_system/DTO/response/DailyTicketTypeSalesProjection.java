package com.example.event_ticket_system.DTO.response;

import java.time.LocalDate;

public interface DailyTicketTypeSalesProjection {
    LocalDate getDate();
    String getTicketType();
    Long getTotalQuantity();
}
