package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Ticket;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Entity.Ward;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.TicketRepository;
import com.example.event_ticket_system.Repository.UserRepository;
import com.example.event_ticket_system.Repository.WardRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.EventService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final WardRepository wardRepository;

    private final TicketRepository ticketRepository;

    @Autowired
    private final JwtUtil jwtUtil;

    @Override
    public Integer createEvent(EventRequestDto eventRequestDto, HttpServletRequest request) {
//        Integer organizerId = jwtUtil.extractUserId(jwtUtil.extractRole(request.getHeader("Authorization").substring(7)));
//        User currentUser = userRepository.findById(organizerId)
//                .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + organizerId));
//        if (!currentUser.getRole().equals("organizer")) {
//            throw new SecurityException("You do not have permission to create events.");
//        }
        Ward ward = wardRepository.findById(eventRequestDto.getWardId())
                .orElseThrow(() -> new EntityNotFoundException("Ward not found with id: " + eventRequestDto.getWardId()));

        Event event = new Event();
        event.setEventName(eventRequestDto.getEventName());
        event.setDescription(eventRequestDto.getDescription());
        event.setWard(ward);
        event.setAddressDetail(eventRequestDto.getAddressDetail());
        event.setAddressName(eventRequestDto.getAddressName());
        event.setCategory(eventRequestDto.getCategory());
        event.setStartTime(eventRequestDto.getStartTime());
        event.setEndTime(eventRequestDto.getEndTime());
//        event.setOrganizer(currentUser);
        event.setCreatedAt(java.time.LocalDateTime.now());
        event.setUpdatedAt(java.time.LocalDateTime.now());
        event = eventRepository.save(event);

        //Tao cac loai ve cho su kien
        for (EventRequestDto.TicketRequest t : eventRequestDto.getTickets()) {
            Ticket ticket = new Ticket();
            ticket.setEvent(event);
            ticket.setTicketType(t.getTicketType());
            ticket.setQuantityTotal(t.getQuantityTotal());
            ticket.setQuantitySold(0);
            ticket.setPrice(t.getPrice());
            ticket.setSaleStart(eventRequestDto.getSaleStart());
            ticket.setSaleEnd(eventRequestDto.getSaleEnd());
            ticketRepository.save(ticket);
        }
        return event.getEventId();
    }
}
