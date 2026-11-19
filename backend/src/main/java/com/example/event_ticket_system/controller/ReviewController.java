package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.request.CustomerReviewBody;
import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/review/upload/{eventId}")
    @PreAuthorize("hasAuthority('ROLE_customer')")
    public ResponseEntity<ApiResponse<Void>> uploadReviewForEvent(
            @PathVariable Integer eventId,
            @RequestBody @Valid CustomerReviewBody customerReviewBody) {
        reviewService.uploadReviewForEvent(eventId, customerReviewBody);
        return ApiResponse.ok(null, "Review created successfully");
    }

    @PutMapping("/review/update/{reviewId}")
    @PreAuthorize("hasAuthority('ROLE_customer')")
    public ResponseEntity<ApiResponse<Void>> updateReviewForEvent(
            @PathVariable Integer reviewId,
            @RequestBody @Valid CustomerReviewBody customerReviewBody) {
        reviewService.updateReviewForEvent(reviewId, customerReviewBody);
        return ApiResponse.ok(null, "Review updated successfully");
    }

    @DeleteMapping("/review/delete/{reviewId}")
    @PreAuthorize("hasAnyAuthority('ROLE_customer', 'ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> deleteReviewForEvent(@PathVariable Integer reviewId) {
        reviewService.deleteReviewForEvent(reviewId);
        return ApiResponse.ok(null, "Review deleted successfully");
    }

    @GetMapping("/review/event/{eventId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewsByEventId(@PathVariable Integer eventId) {
        return ApiResponse.ok(reviewService.getReviewsByEventId(eventId), "Reviews fetched successfully");
    }

    @GetMapping("/review/user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_admin') or #userId == authentication.principal.userId")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewsByUserId(@PathVariable Integer userId) {
        return ApiResponse.ok(reviewService.getReviewsByUserId(userId), "Reviews fetched successfully");
    }
}
