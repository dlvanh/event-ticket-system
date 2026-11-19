package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.request.CustomerReviewBody;

import java.util.Map;

public interface ReviewService {
    void uploadReviewForEvent(Integer eventId, CustomerReviewBody customerReviewBody);
    void updateReviewForEvent(Integer reviewId, CustomerReviewBody customerReviewBody);
    void deleteReviewForEvent(Integer reviewId);
    Map<String, Object> getReviewsByEventId(Integer eventId);
    Map<String, Object> getReviewsByUserId(Integer userId);
}
