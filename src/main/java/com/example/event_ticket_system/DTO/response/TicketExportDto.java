package com.example.event_ticket_system.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketExportDto {
    private String ticketType;
    private int totalQuantity;
    private int soldQuantity;
    private int remainingQuantity;
    private double revenue;
}
