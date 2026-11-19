package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.dto.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecommendEventsResponseDto>>> getRecommendations() {
        List<RecommendEventsResponseDto> responseDtos = recommendationService.recommendEventsForUser();
        return ApiResponse.ok(responseDtos, "Recommended events retrieved successfully");
    }
}
