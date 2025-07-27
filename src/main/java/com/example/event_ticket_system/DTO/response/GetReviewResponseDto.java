package com.example.event_ticket_system.DTO.response;

import lombok.Data;

@Data
public class GetReviewResponseDto {
    private String userFullName;
    private String userProfilePicture;
    private Integer rating;
    private String comment;
    private String reviewDate;
}
