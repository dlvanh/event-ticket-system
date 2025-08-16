package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.response.RecommendEventsResponseDto;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface RecommendationService {
    List<RecommendEventsResponseDto> recommendEventsForUser(HttpServletRequest request);
}
