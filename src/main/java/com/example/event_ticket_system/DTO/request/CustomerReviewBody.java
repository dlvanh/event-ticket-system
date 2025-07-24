package com.example.event_ticket_system.DTO.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerReviewBody {
    private Integer userId;
    private Integer eventId;
    private int rating;
    private String comment;
    private LocalDateTime reviewDate;
}
