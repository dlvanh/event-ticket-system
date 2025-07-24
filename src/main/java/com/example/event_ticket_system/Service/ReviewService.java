package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.CustomerReviewBody;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface ReviewService {
    void uploadReviewForEvent(CustomerReviewBody customerReviewBody, HttpServletRequest request);
    void updateReviewForEvent(Integer reviewId, CustomerReviewBody customerReviewBody, HttpServletRequest request);
    void deleteReviewForEvent(Integer reviewId, HttpServletRequest request);
    Map<String, Object> getReviewsByEventId(Integer eventId, Integer page, Integer size, HttpServletRequest request);
    Map<String, Object> getReviewsByUserId(Integer userId, Integer page, Integer size, HttpServletRequest request);
}
