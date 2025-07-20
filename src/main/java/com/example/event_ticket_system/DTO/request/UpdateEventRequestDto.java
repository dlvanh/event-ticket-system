package com.example.event_ticket_system.DTO.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateEventRequestDto {
    private String eventName;
    private String description;
    private String addressName;
    private String addressDetail;
    private Integer wardId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String category;
    private List<TicketTypeDto> ticketTypes;

    @Data
    public static class TicketTypeDto {
        private String ticketType;
        private Integer quantityTotal;
        private Double price;
    }
}
