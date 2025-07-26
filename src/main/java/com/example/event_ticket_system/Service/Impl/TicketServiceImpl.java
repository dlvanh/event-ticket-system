package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.response.TicketResponseDTO;
import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Repository.OrderRepository;
import com.example.event_ticket_system.Repository.OrderTicketRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService{
    private final JwtUtil jwtUtil;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;

    @Override
    public List<TicketResponseDTO> getTicketsByUserId(Integer userId, HttpServletRequest request) {
        String userRole = jwtUtil.extractRole(request.getHeader("Authorization").substring(7));
        if (!"ROLE_customer".equals(userRole)) {
            throw new SecurityException("User not found with id: " + userId);
        }
        List<Order> orders = orderRepository.findByUserId(userId);
        if (orders.isEmpty()) {
            return new ArrayList<>();
        }
        List<TicketResponseDTO> response = new ArrayList<>();

        for (Order order : orders) {
            List<OrderTicket> orderTickets = orderTicketRepository.findByOrderOrderId(order.getOrderId());

            List<TicketResponseDTO.TicketDetail> ticketDetails = orderTickets.stream()
                    .map(ot -> new TicketResponseDTO.TicketDetail(
                            ot.getTicket().getTicketId(),
                            ot.getTicket().getEvent().getEventName(),
                            ot.getTicket().getEvent().getStartTime(),
                            ot.getTicket().getEvent().getEndTime(),
                            ot.getTicket().getEvent().getCategory(),
                            ot.getTicket().getEvent().getAddressDetail(),
                            ot.getTicket().getTicketType(),
                            ot.getUnitPrice(),
                            ot.getQuantity()
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
}
