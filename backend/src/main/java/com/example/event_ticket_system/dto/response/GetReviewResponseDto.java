package com.example.event_ticket_system.dto.response;

import lombok.Data;

@Data
public class GetReviewResponseDto {
    private Integer reviewId;
    private Integer userId;
    private String userFullName;
    private String userProfilePicture;
    private Integer rating;
    private String comment;
    private String reviewDate;
}
