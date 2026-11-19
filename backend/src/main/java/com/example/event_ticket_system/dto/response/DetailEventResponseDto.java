package com.example.event_ticket_system.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

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
    private Integer organizerId;
    private String organizerName;
    private String organizerBio;
    private String organizerAvatarUrl;
    private String organizerEmail;
    private String organizerPhoneNumber;
    private Map<Integer, String> ticketTypes;
    private Map<String, BigDecimal> ticketPrices;
    private Map<String, Integer> ticketsTotal;
    private Map<String, Integer> ticketsSold;
    private String ticketsSaleStartTime;
    private String ticketsSaleEndTime;
    private String rejectReason;
}
