package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.dto.response.DailyTicketSalesDTO;
import com.example.event_ticket_system.dto.response.DailyTicketSalesProjection;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesDTO;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesProjection;
import com.example.event_ticket_system.dto.response.TicketResponseDTO;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Order;
import com.example.event_ticket_system.entity.OrderTicket;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import com.example.event_ticket_system.service.TicketService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final EventRepository eventRepository;

    @Override
    public List<TicketResponseDTO> getTicketsByUserId(Integer userId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.customer) || !userId.equals(currentUserId)) {
            throw new SecurityException("You do not have permission to view tickets for this user.");
        }

        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);
        if (orders.isEmpty()) {
            return List.of();
        }

        // Batch-load all OrderTickets for all user orders in a single query
        List<Integer> orderIds = orders.stream().map(Order::getOrderId).toList();
        Map<Integer, List<OrderTicket>> orderTicketsByOrderId = orderTicketRepository.findByOrderIdsWithDetails(orderIds)
                .stream()
                .collect(Collectors.groupingBy(ot -> ot.getOrder().getOrderId()));

        List<TicketResponseDTO> response = new ArrayList<>(orders.size());
        for (Order order : orders) {
            List<OrderTicket> orderTickets = orderTicketsByOrderId.getOrDefault(order.getOrderId(), List.of());
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
                            ot.getCheckinToken(),
                            ot.getQrCode()
                    ))
                    .toList();

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
    public List<DailyTicketSalesDTO> getTicketsSoldPerDay(Integer eventId) {
        authorizeOrganizerEventAccess(eventId);
        List<DailyTicketSalesProjection> projections = orderTicketRepository.findTicketSoldPerDayByEventId(eventId);
        return projections.stream()
                .map(p -> new DailyTicketSalesDTO(p.getDate(), p.getTotalTicketSold()))
                .toList();
    }

    @Override
    public List<DailyTicketTypeSalesDTO> getTicketsSoldPerTicketType(Integer eventId, String ticketType) {
        authorizeOrganizerEventAccess(eventId);
        List<DailyTicketTypeSalesProjection> projections = orderTicketRepository.findTicketSoldPerTicketTypeByEventId(eventId);

        if (ticketType != null && !ticketType.isBlank()) {
            projections = projections.stream()
                    .filter(p -> ticketType.equalsIgnoreCase(p.getTicketType()))
                    .toList();
        }
        return projections.stream()
                .map(p -> new DailyTicketTypeSalesDTO(p.getDate(), p.getTicketType(), p.getTotalQuantity()))
                .toList();
    }

    private void authorizeOrganizerEventAccess(Integer eventId) {
        UserRole role = SecurityUtils.getCurrentUserRole();
        Integer userId = SecurityUtils.getCurrentUserId();
        if (role != UserRole.organizer && role != UserRole.admin) {
            throw new SecurityException("You do not have permission to view this event's ticket sales.");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + eventId + " does not exist."));

        if (role != UserRole.admin && !event.getOrganizer().getId().equals(userId)) {
            throw new SecurityException("You do not have permission to view this event's ticket sales.");
        }
    }
}
