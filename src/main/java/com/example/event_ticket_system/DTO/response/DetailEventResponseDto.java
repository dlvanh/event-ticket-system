package com.example.event_ticket_system.DTO.response;

import lombok.Data;

@Data
public class DetailEventResponseDto {
    private Integer eventId;
    private String eventName;
    private String description;
    private String address;
    private String startTime;
    private String endTime;
    private String category;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String approvalStatus;
    private String logoUrl;
    private String backgroundUrl;
}
