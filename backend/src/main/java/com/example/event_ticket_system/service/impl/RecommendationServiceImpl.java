package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.dto.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import com.example.event_ticket_system.service.EventQueryService;
import com.example.event_ticket_system.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

    private final EventRepository eventRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final EventQueryService eventQueryService;

    @Override
    public List<RecommendEventsResponseDto> recommendEventsForUser() {
        Integer userId = SecurityUtils.getCurrentUserId();
        List<String> favouriteGenres = orderTicketRepository.findFavouriteGenresByUserId(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Event> events = favouriteGenres.isEmpty()
                ? List.of()
                : eventRepository.findUpcomingEventsByGenres(favouriteGenres, now);

        if (events.isEmpty()) {
            events = eventRepository.findAllUpcomingEvents(now);
        }

        // Reuse batch-mapped DTO conversion (zero N+1 queries)
        return eventQueryService.mapToRecommendDtos(events);
    }
}
