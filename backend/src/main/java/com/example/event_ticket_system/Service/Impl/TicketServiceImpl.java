package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.response.*;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.OrderRepository;
import com.example.event_ticket_system.Repository.OrderTicketRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.TicketService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService{
    private final JwtUtil jwtUtil;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final EventRepository eventRepository;

    @Override
    public List<TicketResponseDTO> getTicketsByUserId(Integer userId, HttpServletRequest request) {
        String userRole = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        Integer tokenUserId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        if (!"ROLE_customer".equals(userRole)) {
            throw new SecurityException("You do not have permission to view tickets for this user.");
        }
        if (!userId.equals(tokenUserId)) {
            throw new SecurityException("You do not have permission to view tickets for this user.");
        }

        List<Order> orders = orderRepository.findByUserId(userId);
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("No tickets found for this user.");
        }
        List<TicketResponseDTO> response = new ArrayList<>();

        for (Order order : orders) {
            List<OrderTicket> orderTickets = orderTicketRepository.findByOrderOrderId(order.getOrderId());

            List<TicketResponseDTO.TicketDetail> ticketDetails = orderTickets.stream()
                    .filter(ot -> ot.getTicket() != null && ot.getTicket().getEvent() != null)
                    .map(ot -> new TicketResponseDTO.TicketDetail(
                            ot.getTicket().getTicketId(),
                            ot.getOrderTicketId(),
                            ot.getTicket().getEvent().getEventName(),
                            ot.getTicket().getEvent().getStartTime(),
                            ot.getTicket().getEvent().getEndTime(),
                            ot.getTicket().getEvent().getCategory(),
                            ot.getTicket().getEvent().getAddressDetail(),
                            ot.getTicket().getTicketType(),
                            ot.getUnitPrice(),
                            ot.getQuantity(),
                            ot.getQrCode()
                    ))
                    .collect(Collectors.toList());

            response.add(new TicketResponseDTO(
                    order.getOrderId(),
                    order.getOrderDate(),
                    order.getTotalAmount(),
                    order.getStatus(),
                    order.getPayosOrderCode(),
                    order.getCancellationReason(),
                    ticketDetails
            ));
        }
        return response;
    }

    @Override
    public List<DailyTicketSalesDTO> getTicketsSoldPerDay(Integer eventId, HttpServletRequest request) {
        try {
            String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
            Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));

            if (!role.equals("ROLE_organizer")) {
                throw new SecurityException("You do not have permission to view this event's ticket sales.");
            }

            Optional<Event> eventOptional = eventRepository.findById(eventId);
            if (eventOptional.isEmpty()) {
                throw new EntityNotFoundException("Event with ID " + eventId + " does not exist.");
            }

            Event event = eventOptional.get();
            if (!event.getOrganizer().getId().equals(userId)) {
                throw new SecurityException("You do not have permission to view this event's ticket sales.");
            }

            List<DailyTicketSalesProjection> projections = orderTicketRepository.findTicketSoldPerDayByEventId(eventId);

            return projections.stream()
                    .map(p -> new DailyTicketSalesDTO(p.getDate(), p.getTotalTicketSold()))
                    .collect(Collectors.toList());

        } catch (SecurityException | EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error processing ticket sales data: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DailyTicketTypeSalesDTO> getTicketsSoldPerTicketType(Integer eventId, String ticketType, HttpServletRequest request) {
        try {
            String role = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
            Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));

            if (!role.equals("ROLE_organizer")) {
                throw new SecurityException("You do not have permission to view this event's ticket sales.");
            }

            Optional<Event> eventOptional = eventRepository.findById(eventId);
            if (eventOptional.isEmpty()) {
                throw new EntityNotFoundException("Event with ID " + eventId + " does not exist.");
            }

            Event event = eventOptional.get();
            if (!event.getOrganizer().getId().equals(userId)) {
                throw new SecurityException("You do not have permission to view this event's ticket sales.");
            }

            List<DailyTicketTypeSalesProjection> projections = orderTicketRepository.findTicketSoldPerTicketTypeByEventId(eventId);

            if (ticketType != null && !ticketType.isEmpty()) {
                projections = projections.stream()
                        .filter(p -> ticketType.equalsIgnoreCase(p.getTicketType()))
                        .toList();
            }

            return projections.stream()
                    .map(p -> new DailyTicketTypeSalesDTO(p.getDate(), p.getTicketType(), p.getTotalQuantity()))
                    .collect(Collectors.toList());

        } catch (SecurityException | EntityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error processing ticket sales data: " + e.getMessage(), e);
        }
    }

}
