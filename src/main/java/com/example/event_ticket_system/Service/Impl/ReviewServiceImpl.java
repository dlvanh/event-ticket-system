package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.CustomerReviewBody;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Review;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Enums.EventStatus;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.ReviewRepository;
import com.example.event_ticket_system.Repository.UserRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final JwtUtil jwtUtil;
    private final EventRepository eventRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    public void uploadReviewForEvent(CustomerReviewBody customerReviewBody, HttpServletRequest request) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        if (!"ROLE_customer".equals(role)) {
            throw new SecurityException("You do not have permission to write a review for an event.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + customerReviewBody.getUserId()));

        Event event = eventRepository.findById(customerReviewBody.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + customerReviewBody.getEventId()));

        if (!event.getStatus().equals(EventStatus.completed)) {
            throw new IllegalArgumentException("Reviews can only be submitted for completed events.");
        }

        if (customerReviewBody.getEventId() == null || customerReviewBody.getEventId() <= 0) {
            throw new IllegalArgumentException("Event ID must be a positive integer.");
        }

        if (customerReviewBody.getRating() < 1 || customerReviewBody.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        if (customerReviewBody.getComment().length() > 255) {
            throw new IllegalArgumentException("Comment cannot exceed 255 characters.");
        }

        // TODO: Prevent duplicate reviews by the same user for the same event

        Review review = new Review();
        review.setUser(user);
        review.setEvent(event);
        review.setRating(customerReviewBody.getRating());
        review.setComment(customerReviewBody.getComment());
        review.setReviewDate(LocalDateTime.now());
        reviewRepository.save(review);

    }

    @Override
    public void updateReviewForEvent(Integer reviewId, CustomerReviewBody customerReviewBody, HttpServletRequest request) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        if (!"ROLE_customer".equals(role)) {
            throw new SecurityException("You do not have permission to update a review for an event.");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found for this user and event."));

        if (!review.getUser().getId().equals(userId)) {
            throw new SecurityException("You do not have permission to update this review.");
        }

        if (customerReviewBody.getEventId() == null || customerReviewBody.getEventId() <= 0) {
            throw new IllegalArgumentException("Event ID must be a positive integer.");
        }

        if (customerReviewBody.getRating() == review.getRating()) {
            throw new IllegalArgumentException("No changes detected in the rating.");
        }

        if (customerReviewBody.getRating() < 1 || customerReviewBody.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        if (customerReviewBody.getComment().equals(review.getComment())) {
            throw new IllegalArgumentException("No changes detected in the comment.");
        }

        if (customerReviewBody.getComment().length() > 255) {
            throw new IllegalArgumentException("Comment cannot exceed 255 characters.");
        }

        review.setRating(customerReviewBody.getRating());
        review.setComment(customerReviewBody.getComment());
        review.setReviewDate(LocalDateTime.now());
        reviewRepository.save(review);

    }

    @Override
    public void deleteReviewForEvent(Integer reviewId, HttpServletRequest request) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + reviewId));

        boolean isOwner = review.getUser().getId().equals(userId);
        boolean isAdmin = "ROLE_admin".equals(role);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("You do not have permission to delete this review.");
        }

        reviewRepository.deleteById(reviewId);
    }

    @Override
    public Map<String, Object> getReviewsByEventId(Integer eventId, Integer page, Integer size, HttpServletRequest request) {
        return null;
    }

    @Override
    public Map<String, Object> getReviewsByUserId(Integer userId, Integer page, Integer size, HttpServletRequest request) {
        return null;
    }
}
