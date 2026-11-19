package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.response.RecommendEventsResponseDto;

import java.util.List;

public interface RecommendationService {
    List<RecommendEventsResponseDto> recommendEventsForUser();
}
