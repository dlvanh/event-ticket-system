package com.example.event_ticket_system.ticket.service;

import com.example.event_ticket_system.DTO.response.*;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Entity.Ticket;
import com.example.event_ticket_system.Enums.OrderStatus;
import com.example.event_ticket_system.Repository.EventRepository;
import com.example.event_ticket_system.Repository.OrderRepository;
import com.example.event_ticket_system.Repository.OrderTicketRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.Impl.TicketServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrderTicketRepository orderTicketRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Order order;
    private OrderTicket orderTicket;
    private Event event;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setEventName("Concert A");
        event.setStartTime(LocalDateTime.of(2025, 8, 20, 19, 0));
        event.setEndTime(LocalDateTime.of(2025, 8, 20, 22, 0));
        event.setCategory("Music");
        event.setAddressDetail("Hanoi Opera House");

        ticket = new Ticket();
        ticket.setTicketId(101);
        ticket.setTicketType("VIP");
        ticket.setEvent(event);

        orderTicket = new OrderTicket();
        orderTicket.setOrderTicketId(1001);
        orderTicket.setTicket(ticket);
        orderTicket.setUnitPrice(50.0);
        orderTicket.setQuantity(2);

        order = new Order();
        order.setOrderId(5001);
        order.setOrderDate(LocalDateTime.of(2025, 8, 1, 10, 0));
        order.setTotalAmount(100.0);
        order.setStatus(OrderStatus.paid);
        order.setPayosOrderCode(123456789L);
        order.setCancellationReason(null);
    }

    @Test
    void getTicketsByUserId_ShouldReturnTicketResponse_WhenValidRequest() {
        Integer userId = 1;
        String token = "mockToken";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_customer");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(orderRepository.findByUserId(userId)).thenReturn(Collections.singletonList(order));
        when(orderTicketRepository.findByOrderOrderId(order.getOrderId())).thenReturn(Collections.singletonList(orderTicket));

        List<TicketResponseDTO> result = ticketService.getTicketsByUserId(userId, request);

        assertThat(result).hasSize(1);
        TicketResponseDTO response = result.getFirst();
        assertThat(response.getOrderId()).isEqualTo(5001);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.paid);
        assertThat(response.getTickets()).hasSize(1);
        assertThat(response.getTickets().getFirst().getEventName()).isEqualTo("Concert A");
        assertThat(response.getTickets().getFirst().getTicketType()).isEqualTo("VIP");
    }

    @Test
    void getTicketsByUserId_ShouldThrowSecurityException_WhenRoleIsNotCustomer() {
        Integer userId = 1;
        String token = "mockToken";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_admin");

        assertThatThrownBy(() -> ticketService.getTicketsByUserId(userId, request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view tickets for this user.");
    }

    @Test
    void getTicketsByUserId_ShouldThrowSecurityException_WhenUserIdMismatch() {
        Integer userId = 1;
        String token = "mockToken";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_customer");
        when(jwtUtil.extractUserId(token)).thenReturn(99);

        assertThatThrownBy(() -> ticketService.getTicketsByUserId(userId, request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view tickets for this user.");
    }

    @Test
    void getTicketsByUserId_ShouldThrowIllegalArgumentException_WhenNoOrders() {
        Integer userId = 1;
        String token = "mockToken";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_customer");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(orderRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> ticketService.getTicketsByUserId(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No tickets found for this user.");
    }

    @Test
    void getTicketsSoldPerDay_ShouldReturnData_WhenOrganizerOwnsEvent() {
        Integer eventId = 10;
        String token = "mockToken";
        Integer userId = 1;

        // Mock request + JWT
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_organizer");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);

        // Mock Event
        event.setEventId(eventId);
        event.setOrganizer(new com.example.event_ticket_system.Entity.User());
        event.getOrganizer().setId(userId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Mock projection data
        DailyTicketSalesProjection projection = mock(DailyTicketSalesProjection.class);
        when(projection.getDate()).thenReturn(LocalDate.of(2025, 8, 18));
        when(projection.getTotalTicketSold()).thenReturn(50L);

        when(orderTicketRepository.findTicketSoldPerDayByEventId(eventId))
                .thenReturn(Collections.singletonList(projection));

        // Call method
        List<DailyTicketSalesDTO> result = ticketService.getTicketsSoldPerDay(eventId, request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDate()).isEqualTo(LocalDate.of(2025, 8, 18));
        assertThat(result.getFirst().getTotalTicketSold()).isEqualTo(50L);
    }

    @Test
    void getTicketsSoldPerDay_ShouldThrowSecurityException_WhenRoleMismatch() {
        String token = "mockToken";
        Integer eventId = 101;

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_customer");

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerDay(eventId, request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view this event's ticket sales.");
    }

    @Test
    void getTicketsSoldPerDay_ShouldThrowSecurityException_WhenUserIdMismatch() {
        String token = "mockToken";
        Integer eventId = 101;
        Integer organizerId = 10;
        Integer userId = 99;

        event.setEventId(eventId);
        event.setOrganizer(new com.example.event_ticket_system.Entity.User());
        event.getOrganizer().setId(organizerId);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_organizer");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerDay(eventId, request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view this event's ticket sales.");
    }

    @Test
    void getTicketsSoldPerTicketType_ShouldThrowSecurityException_WhenRoleMismatch() {
        String token = "mockToken";
        Integer eventId = 101;

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_customer");

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerTicketType(eventId, "VIP", request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view this event's ticket sales.");
    }

    @Test
    void getTicketsSoldPerTicketType_ShouldThrowSecurityException_WhenUserIdMismatch() {
        String token = "mockToken";
        Integer eventId = 101;
        Integer organizerId = 10;
        Integer userId = 99;

        event.setEventId(eventId);
        event.setOrganizer(new com.example.event_ticket_system.Entity.User());
        event.getOrganizer().setId(organizerId);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_organizer");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerTicketType(eventId, "VIP", request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view this event's ticket sales.");
    }

    @Test
    void getTicketsSoldPerTicketType_ShouldThrowEntityNotFound_WhenEventDoesNotExist() {
        String token = "mockToken";
        Integer eventId = 101;

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_organizer");
        when(jwtUtil.extractUserId(token)).thenReturn(1);
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerTicketType(eventId, "VIP", request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event with ID " + eventId + " does not exist.");
    }

    @Test
    void getTicketsSoldPerDay_ShouldThrowSecurityException_WhenRoleIsNotOrganizer() {
        Integer eventId = 10;
        String token = "mockToken";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_customer");

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerDay(eventId, request))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view this event's ticket sales.");
    }

    @Test
    void getTicketsSoldPerDay_ShouldThrowEntityNotFound_WhenEventDoesNotExist() {
        Integer eventId = 10;
        String token = "mockToken";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_organizer");
        when(jwtUtil.extractUserId(token)).thenReturn(1);
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerDay(eventId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event with ID " + eventId + " does not exist.");
    }

    @Test
    void getTicketsSoldPerTicketType_ShouldFilterByTicketType() {
        Integer eventId = 10;
        String token = "mockToken";
        Integer userId = 1;

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_organizer");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);

        event.setEventId(eventId);
        event.setOrganizer(new com.example.event_ticket_system.Entity.User());
        event.getOrganizer().setId(userId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        // Mock projection data
        DailyTicketTypeSalesProjection projection1 = mock(DailyTicketTypeSalesProjection.class);
        when(projection1.getDate()).thenReturn(LocalDate.of(2025, 8, 18));
        when(projection1.getTicketType()).thenReturn("VIP");
        when(projection1.getTotalQuantity()).thenReturn(20L);

        DailyTicketTypeSalesProjection projection2 = mock(DailyTicketTypeSalesProjection.class);
        when(projection2.getTicketType()).thenReturn("Standard");

        when(orderTicketRepository.findTicketSoldPerTicketTypeByEventId(eventId))
                .thenReturn(Arrays.asList(projection1, projection2));

        // Call method with filter = VIP
        List<DailyTicketTypeSalesDTO> result = ticketService.getTicketsSoldPerTicketType(eventId, "VIP", request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicketType()).isEqualTo("VIP");
        assertThat(result.getFirst().getTotalQuantity()).isEqualTo(20L);
    }

    @Test
    void getTicketsSoldPerTicketType_ShouldReturnAll_WhenTicketTypeIsNull() {
        Integer eventId = 10;
        String token = "mockToken";
        Integer userId = 1;

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractRole(token)).thenReturn("ROLE_organizer");
        when(jwtUtil.extractUserId(token)).thenReturn(userId);

        event.setEventId(eventId);
        event.setOrganizer(new com.example.event_ticket_system.Entity.User());
        event.getOrganizer().setId(userId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        DailyTicketTypeSalesProjection projection = mock(DailyTicketTypeSalesProjection.class);
        when(projection.getDate()).thenReturn(LocalDate.of(2025, 8, 18));
        when(projection.getTicketType()).thenReturn("VIP");
        when(projection.getTotalQuantity()).thenReturn(20L);

        when(orderTicketRepository.findTicketSoldPerTicketTypeByEventId(eventId))
                .thenReturn(Collections.singletonList(projection));

        List<DailyTicketTypeSalesDTO> result = ticketService.getTicketsSoldPerTicketType(eventId, null, request);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicketType()).isEqualTo("VIP");
    }
}
