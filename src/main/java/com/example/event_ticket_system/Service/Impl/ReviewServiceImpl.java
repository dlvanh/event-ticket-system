package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.CustomerReviewBody;
import com.example.event_ticket_system.DTO.response.GetReviewResponseDto;
import com.example.event_ticket_system.DTO.response.GetUsersReviewResponseDto;
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
import java.util.List;
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
    public void uploadReviewForEvent(Integer eventId, CustomerReviewBody customerReviewBody, HttpServletRequest request) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        if (!"ROLE_customer".equals(role)) {
            throw new SecurityException("You do not have permission to write a review for an event.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + customerReviewBody.getUserId()));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + customerReviewBody.getEventId()));

        if (event.getStatus().equals(EventStatus.upcoming)) {
            throw new IllegalArgumentException("Reviews cannot be submitted for upcoming events.");
        }

        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be a positive integer.");
        }

        if (customerReviewBody.getRating() < 1 || customerReviewBody.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        if (customerReviewBody.getComment().length() > 255) {
            throw new IllegalArgumentException("Comment cannot exceed 255 characters.");
        }

        if (reviewRepository.findByEvent(event).stream()
                .anyMatch(review -> review.getUser().getId().equals(userId))) {
            throw new IllegalArgumentException("You have already submitted a review for this event.");
        }
        // TODO: Allow user to upload images with the review
        log.info("User with ID {} is uploading a review for event with ID {}", userId, eventId);
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

        if (customerReviewBody.getRating() < 1 || customerReviewBody.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        String newComment = customerReviewBody.getComment() != null ? customerReviewBody.getComment().trim() : null;
        String existingComment = review.getComment() != null ? review.getComment().trim() : null;

        if (customerReviewBody.getRating() == review.getRating() &&
                (newComment == null && existingComment == null || newComment != null && newComment.equals(existingComment))) {
            throw new IllegalArgumentException("No changes detected in rating or comment.");
        }

        if (newComment != null && newComment.length() > 255) {
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
    public Map<String, Object> getReviewsByEventId(Integer eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        if (!event.getStatus().equals(EventStatus.completed)) {
            throw new IllegalArgumentException("Reviews can only be retrieved for completed events.");
        }
        List<Review> reviews = reviewRepository.findByEvent(event);
        if (reviews.isEmpty()) {
            throw new EntityNotFoundException("No reviews found for this event.");
        }

        List<GetReviewResponseDto> data = reviews.stream()
                .map(review -> {
                    GetReviewResponseDto dto = new GetReviewResponseDto();
                    dto.setReviewId(review.getReviewId());
                    dto.setUserId(review.getUser().getId());
                    dto.setUserFullName(review.getUser().getFullName());
                    dto.setUserProfilePicture(review.getUser().getProfilePicture());
                    dto.setRating(review.getRating());
                    dto.setComment(review.getComment());
                    dto.setReviewDate(review.getReviewDate().toString());
                    return dto;
                })
                .toList();

        return Map.of("reviewDetails", data);
    }

    @Override
    public Map<String, Object> getReviewsByUserId(Integer userId, HttpServletRequest request) {
        String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        Integer currentUserId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));

        if (!"ROLE_admin".equals(role) && !currentUserId.equals(userId)) {
            throw new SecurityException("You do not have permission to view reviews for this user.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<Review> reviews = reviewRepository.findByUser(user);
        if (reviews.isEmpty()) {
            throw new EntityNotFoundException("No reviews found for this user.");
        }

        GetUsersReviewResponseDto response = new GetUsersReviewResponseDto();
        response.setUserId(user.getId());
        response.setUserFullName(user.getFullName());
        response.setUserProfilePicture(user.getProfilePicture());

        List<GetUsersReviewResponseDto.UsersReviewDTO> reviewDtos = reviews.stream().map(review -> {
            GetUsersReviewResponseDto.UsersReviewDTO dto = new GetUsersReviewResponseDto.UsersReviewDTO();
            dto.setReviewId(review.getReviewId());
            dto.setRating(review.getRating());
            dto.setComment(review.getComment());
            dto.setReviewDate(review.getReviewDate().toString());

            // Map event summary
            Event event = review.getEvent();
            GetUsersReviewResponseDto.EventSummaryDTO eventSummary = new GetUsersReviewResponseDto.EventSummaryDTO();
            eventSummary.setEventId(event.getEventId());
            eventSummary.setEventName(event.getEventName());
            eventSummary.setCategory(event.getCategory());
            eventSummary.setStatus(event.getStatus().toString());
            eventSummary.setStartTime(event.getStartTime());
            eventSummary.setEndTime(event.getEndTime());
            eventSummary.setEventLogoUrl(event.getLogoUrl());
            dto.setEventSummary(eventSummary);

            return dto;
        }).toList();

        response.setUsersReview(reviewDtos);

        return Map.of("userReviews", response);
    }
}
