package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.dto.request.CustomerReviewBody;
import com.example.event_ticket_system.dto.response.GetReviewResponseDto;
import com.example.event_ticket_system.dto.response.GetUsersReviewResponseDto;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Review;
import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.enums.EventStatus;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.repository.ReviewRepository;
import com.example.event_ticket_system.repository.UserRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import com.example.event_ticket_system.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final EventRepository eventRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void uploadReviewForEvent(Integer eventId, CustomerReviewBody body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.customer)) {
            throw new SecurityException("You do not have permission to write a review for an event.");
        }
        if (eventId == null || eventId <= 0) {
            throw new IllegalArgumentException("Event ID must be a positive integer.");
        }
        if (body.getRating() < 1 || body.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        if (body.getComment() != null && body.getComment().length() > 255) {
            throw new IllegalArgumentException("Comment cannot exceed 255 characters.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        if (event.getStatus() == EventStatus.cancelled
                || (event.getEndTime() != null && event.getEndTime().isAfter(LocalDateTime.now()))) {
            throw new IllegalArgumentException("Reviews can only be submitted after the event has ended.");
        }
        if (!orderTicketRepository.existsPaidTicketByUserAndEvent(userId, eventId)) {
            throw new SecurityException("Only customers who purchased a paid ticket can review this event.");
        }
        if (reviewRepository.existsByEventEventIdAndUserId(eventId, userId)) {
            throw new IllegalArgumentException("You have already submitted a review for this event.");
        }

        log.info("User {} submitting review for event {}", userId, eventId);
        Review review = new Review();
        review.setUser(user);
        review.setEvent(event);
        review.setRating(body.getRating());
        review.setComment(body.getComment());
        review.setReviewDate(LocalDateTime.now());
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void updateReviewForEvent(Integer reviewId, CustomerReviewBody body) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.customer)) {
            throw new SecurityException("You do not have permission to update a review for an event.");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(userId)) {
            throw new SecurityException("You do not have permission to update this review.");
        }
        if (body.getRating() < 1 || body.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        String newComment = body.getComment() != null ? body.getComment().trim() : null;
        String existingComment = review.getComment() != null ? review.getComment().trim() : null;
        if (body.getRating() == review.getRating() && Objects.equals(newComment, existingComment)) {
            throw new IllegalArgumentException("No changes detected in rating or comment.");
        }
        if (newComment != null && newComment.length() > 255) {
            throw new IllegalArgumentException("Comment cannot exceed 255 characters.");
        }

        review.setRating(body.getRating());
        review.setComment(newComment);
        review.setReviewDate(LocalDateTime.now());
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void deleteReviewForEvent(Integer reviewId) {
        Integer userId = SecurityUtils.getCurrentUserId();
        UserRole role = SecurityUtils.getCurrentUserRole();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + reviewId));

        if (!review.getUser().getId().equals(userId) && role != UserRole.admin) {
            throw new SecurityException("You do not have permission to delete this review.");
        }
        reviewRepository.delete(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getReviewsByEventId(Integer eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        if (event.getStatus() == EventStatus.cancelled
                || (event.getEndTime() != null && event.getEndTime().isAfter(LocalDateTime.now()))) {
            throw new IllegalArgumentException("Reviews can only be retrieved after the event has ended.");
        }

        List<Review> reviews = reviewRepository.findByEventWithUser(event);
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
    @Transactional(readOnly = true)
    public Map<String, Object> getReviewsByUserId(Integer userId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.admin) && !currentUserId.equals(userId)) {
            throw new SecurityException("You do not have permission to view reviews for this user.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<Review> reviews = reviewRepository.findByUserWithEvent(user);

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

            Event event = review.getEvent();
            GetUsersReviewResponseDto.EventSummaryDTO eventSummary = new GetUsersReviewResponseDto.EventSummaryDTO();
            eventSummary.setEventId(event.getEventId());
            eventSummary.setEventName(event.getEventName());
            eventSummary.setCategory(event.getCategory());
            eventSummary.setStatus(event.getStatus().name());
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
