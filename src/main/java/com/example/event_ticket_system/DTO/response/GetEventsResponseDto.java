package com.example.event_ticket_system.DTO.response;

import lombok.Data;

@Data
public class GetEventsResponseDto {
    private Integer eventId;
    private String eventName;
    private String status;
    private String address;
    private String approvalStatus;
    private String startTime;
    private String endTime;
    private String updateAt;
    private String organizerName;
}
