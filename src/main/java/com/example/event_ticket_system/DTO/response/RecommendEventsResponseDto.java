package com.example.event_ticket_system.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecommendEventsResponseDto {
    private Integer eventId;
    private String eventName;
    private String address;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String category;
    private String logoUrl;
    private String minPrice;
    private String backgroundUrl;
    private Long totalTicketSold;
}
