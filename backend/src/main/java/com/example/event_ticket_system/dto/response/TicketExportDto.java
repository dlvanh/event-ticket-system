package com.example.event_ticket_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TicketExportDto {
    private String ticketType;
    private int totalQuantity;
    private int soldQuantity;
    private int remainingQuantity;
    private BigDecimal revenue;
}
