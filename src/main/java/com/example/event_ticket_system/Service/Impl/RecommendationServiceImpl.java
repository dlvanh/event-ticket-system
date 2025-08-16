package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Ticket;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.OrderTicketRepository;
import com.example.event_ticket_system.Repository.TicketRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.EventService;
import com.example.event_ticket_system.Service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private OrderTicketRepository orderTicketRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public List<RecommendEventsResponseDto> recommendEventsForUser(HttpServletRequest request) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));

        // Lấy thể loại user yêu thích
        List<String> favouriteGenres = orderTicketRepository.findFavouriteGenresByUserId(userId);

        // Gợi ý sự kiện sắp diễn ra thuộc thể loại đó
        List<Event> events = eventRepository.findUpcomingEventsByGenres(favouriteGenres, LocalDateTime.now());

        if (events == null || events.isEmpty()) {
            events = eventRepository.findAllUpcomingEvents(LocalDateTime.now());
        }

        // Map Event → DTO
        return events.stream()
                .map(event -> {
                    RecommendEventsResponseDto dto = new RecommendEventsResponseDto();
                    dto.setEventId(event.getEventId());
                    dto.setEventName(event.getEventName());
                    dto.setAddress((event.getAddressName() != null ? event.getAddressName() + ", " : "") +
                            (event.getAddressDetail() != null ? event.getAddressDetail() + ", " : "") +
                            event.getWard().getName() + ", " +
                            event.getWard().getDistrict().getName() + ", " +
                            event.getWard().getDistrict().getProvince().getName());
                    dto.setStartTime(event.getStartTime());
                    dto.setEndTime(event.getEndTime());
                    dto.setCategory(event.getCategory());
                    dto.setLogoUrl(event.getLogoUrl());
                    dto.setBackgroundUrl(event.getBackgroundUrl());

                    // Lấy tổng số vé đã bán cho sự kiện này
                    long totalTicketSold = orderTicketRepository.sumQuantityByEvent(event);
                    dto.setTotalTicketSold(totalTicketSold);

                    // Lấy giá vé thấp nhất
                    List<Ticket> tickets = ticketRepository.findByEvent(event);
                    String minPrice = tickets.stream()
                            .map(Ticket::getPrice)
                            .min(Double::compareTo)
                            .map(String::valueOf)
                            .orElse("0.0");
                    dto.setMinPrice(minPrice);

                    return dto;
                })
                .toList();
    }
}
