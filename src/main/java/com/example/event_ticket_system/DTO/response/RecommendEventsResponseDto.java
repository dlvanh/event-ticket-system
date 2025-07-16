package com.example.event_ticket_system.DTO.response;

import lombok.Data;

@Data
public class RecommendEventsResponseDto {
    private Integer eventId;
    private String eventName;
    private String address;
    private String startTime;
    private String endTime;
    private String category;
    private String logoUrl;
    private String minPrice;
    private String backgroundUrl;
    private Long totalTicketSold;
}
