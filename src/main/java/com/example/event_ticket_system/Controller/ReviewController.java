package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.CustomerReviewBody;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/review/upload/{eventId}")
    public ResponseEntity<?> uploadReviewForEvent(@PathVariable Integer eventId,
                                                  @RequestBody @Valid CustomerReviewBody customerReviewBody,
                                                  HttpServletRequest request) {
        try {
            reviewService.uploadReviewForEvent(eventId, customerReviewBody, request);
            return ResponseEntity.ok("Review uploaded successfully");
        }   catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    "You do not have permission to write a review for an event.",
                    HttpStatus.FORBIDDEN
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An error occurred while uploading the review: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PutMapping("/review/update/{reviewId}")
    public ResponseEntity<?> updateReviewForEvent(@PathVariable Integer reviewId,
                                                  @RequestBody @Valid CustomerReviewBody customerReviewBody,
                                                  HttpServletRequest request) {
        try {
            reviewService.updateReviewForEvent(reviewId, customerReviewBody, request);
            return ResponseEntity.ok("Review updated successfully");
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    "Review not found: " + e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    "You do not have permission to update this review.",
                    HttpStatus.FORBIDDEN
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An error occurred while updating the review: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @DeleteMapping("/review/delete/{reviewId}")
    public ResponseEntity<?> deleteReviewForEvent(@PathVariable Integer reviewId, HttpServletRequest request) {
        try {
            reviewService.deleteReviewForEvent(reviewId, request);
            return ResponseEntity.ok("Review deleted successfully");
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    "Review not found: " + e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    "You do not have permission to delete this review.",
                    HttpStatus.FORBIDDEN
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An error occurred while deleting the review: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/review/event/{eventId}")
    public ResponseEntity<?> getReviewsByEventId(@PathVariable Integer eventId, HttpServletRequest request) {
        try {
            Map<String, Object> reviews = reviewService.getReviewsByEventId(eventId, request);
            return ResponseEntity.ok(reviews);
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    "Event not found: " + e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An error occurred while fetching reviews: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/review/user/{userId}")
    public ResponseEntity<?> getReviewsByUserId(@PathVariable Integer userId, HttpServletRequest request) {
        try {
            Map<String, Object> reviews = reviewService.getReviewsByUserId(userId, request);
            return ResponseEntity.ok(reviews);
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    "User not found: " + e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An error occurred while fetching user reviews: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
