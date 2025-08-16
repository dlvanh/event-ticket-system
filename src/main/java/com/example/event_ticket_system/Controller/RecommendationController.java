package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.DTO.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private EventRepository eventRepository;

    @GetMapping()
    public ResponseEntity<?> getRecommendations(
            HttpServletRequest request
    ) {
        try {
            List<RecommendEventsResponseDto> responseDtos = recommendationService.recommendEventsForUser(request);
            return APIResponse.responseBuilder(
                    responseDtos,
                    "get recommend events successfully",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An error occurred while fetching recommendations: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
