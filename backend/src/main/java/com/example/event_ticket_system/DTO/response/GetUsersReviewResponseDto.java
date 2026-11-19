package com.example.event_ticket_system.DTO.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GetUsersReviewResponseDto {
    private Integer userId;
    private String userFullName;
    private String userProfilePicture;
    private List<UsersReviewDTO> usersReview;

    @Data
    public static class UsersReviewDTO {
        private EventSummaryDTO eventSummary;
        private Integer reviewId;
        private Integer rating;
        private String comment;
        private String reviewDate;
    }

    @Data
    public static class EventSummaryDTO {
        private Integer eventId;
        private String eventName;
        private String category;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String eventLogoUrl;
    }
}
