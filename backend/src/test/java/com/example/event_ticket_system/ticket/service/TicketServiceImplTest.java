package com.example.event_ticket_system.ticket.service;

import com.example.event_ticket_system.dto.response.DailyTicketSalesDTO;
import com.example.event_ticket_system.dto.response.DailyTicketSalesProjection;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesDTO;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesProjection;
import com.example.event_ticket_system.dto.response.TicketResponseDTO;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Order;
import com.example.event_ticket_system.entity.OrderTicket;
import com.example.event_ticket_system.entity.Ticket;
import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.enums.OrderStatus;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.security.CustomUserPrincipal;
import com.example.event_ticket_system.service.impl.TicketServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
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
    private OrderRepository orderRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrderTicketRepository orderTicketRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    private Order order;
    private OrderTicket orderTicket;
    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setEventId(10);
        event.setEventName("Concert A");
        event.setStartTime(LocalDateTime.of(2025, 8, 20, 19, 0));
        event.setEndTime(LocalDateTime.of(2025, 8, 20, 22, 0));
        event.setCategory("Music");
        event.setAddressDetail("Hanoi Opera House");

        Ticket ticket = new Ticket();
        ticket.setTicketId(101);
        ticket.setTicketType("VIP");
        ticket.setEvent(event);

        order = new Order();
        order.setOrderId(5001);
        order.setOrderDate(LocalDateTime.of(2025, 8, 1, 10, 0));
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setStatus(OrderStatus.paid);
        order.setPayosOrderCode(123456789L);

        orderTicket = new OrderTicket();
        orderTicket.setOrderTicketId(1001);
        orderTicket.setOrder(order);
        orderTicket.setTicket(ticket);
        orderTicket.setUnitPrice(new BigDecimal("50.00"));
        orderTicket.setQuantity(2);
        orderTicket.setCheckinToken("v1:5001:1001:101:1:10:2.sig");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Integer userId, UserRole role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "user" + userId + "@test.com", "User " + userId, role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    void getTicketsByUserId_ShouldReturnTicketResponse_WhenValidCustomer() {
        Integer userId = 1;
        authenticateAs(userId, UserRole.customer);

        when(orderRepository.findByUserIdOrderByOrderDateDesc(userId)).thenReturn(Collections.singletonList(order));
        when(orderTicketRepository.findByOrderIdsWithDetails(List.of(order.getOrderId())))
                .thenReturn(Collections.singletonList(orderTicket));

        List<TicketResponseDTO> result = ticketService.getTicketsByUserId(userId);

        assertThat(result).hasSize(1);
        TicketResponseDTO response = result.getFirst();
        assertThat(response.getOrderId()).isEqualTo(5001);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.paid);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getTickets()).hasSize(1);
        assertThat(response.getTickets().getFirst().getEventName()).isEqualTo("Concert A");
        assertThat(response.getTickets().getFirst().getTicketType()).isEqualTo("VIP");
        assertThat(response.getTickets().getFirst().getCheckinToken()).isEqualTo("v1:5001:1001:101:1:10:2.sig");
    }

    @Test
    void getTicketsByUserId_ShouldThrowSecurityException_WhenRoleIsNotCustomer() {
        authenticateAs(1, UserRole.admin);

        assertThatThrownBy(() -> ticketService.getTicketsByUserId(1))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view tickets for this user.");
    }

    @Test
    void getTicketsByUserId_ShouldThrowSecurityException_WhenUserIdMismatch() {
        authenticateAs(99, UserRole.customer);

        assertThatThrownBy(() -> ticketService.getTicketsByUserId(1))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view tickets for this user.");
    }

    @Test
    void getTicketsByUserId_ShouldReturnEmptyList_WhenNoOrders() {
        authenticateAs(1, UserRole.customer);
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1)).thenReturn(Collections.emptyList());

        List<TicketResponseDTO> result = ticketService.getTicketsByUserId(1);

        assertThat(result).isEmpty();
    }

    @Test
    void getTicketsSoldPerDay_ShouldReturnData_WhenOrganizerOwnsEvent() {
        Integer eventId = 10;
        Integer userId = 1;
        authenticateAs(userId, UserRole.organizer);

        User organizer = new User();
        organizer.setId(userId);
        event.setOrganizer(organizer);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        DailyTicketSalesProjection projection = mock(DailyTicketSalesProjection.class);
        when(projection.getDate()).thenReturn(LocalDate.of(2025, 8, 18));
        when(projection.getTotalTicketSold()).thenReturn(50L);
        when(orderTicketRepository.findTicketSoldPerDayByEventId(eventId))
                .thenReturn(Collections.singletonList(projection));

        List<DailyTicketSalesDTO> result = ticketService.getTicketsSoldPerDay(eventId);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDate()).isEqualTo(LocalDate.of(2025, 8, 18));
        assertThat(result.getFirst().getTotalTicketSold()).isEqualTo(50L);
    }

    @Test
    void getTicketsSoldPerDay_ShouldReturnData_WhenAdmin() {
        Integer eventId = 10;
        authenticateAs(999, UserRole.admin);

        User organizer = new User();
        organizer.setId(1);
        event.setOrganizer(organizer);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(orderTicketRepository.findTicketSoldPerDayByEventId(eventId)).thenReturn(List.of());

        List<DailyTicketSalesDTO> result = ticketService.getTicketsSoldPerDay(eventId);

        assertThat(result).isEmpty();
    }

    @Test
    void getTicketsSoldPerDay_ShouldThrowSecurityException_WhenRoleIsCustomer() {
        authenticateAs(1, UserRole.customer);

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerDay(101))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view this event's ticket sales.");
    }

    @Test
    void getTicketsSoldPerDay_ShouldThrowSecurityException_WhenOrganizerDoesNotOwnEvent() {
        Integer eventId = 101;
        authenticateAs(99, UserRole.organizer);

        User owner = new User();
        owner.setId(10);
        event.setEventId(eventId);
        event.setOrganizer(owner);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerDay(eventId))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to view this event's ticket sales.");
    }

    @Test
    void getTicketsSoldPerDay_ShouldThrowEntityNotFound_WhenEventDoesNotExist() {
        authenticateAs(1, UserRole.organizer);
        when(eventRepository.findById(10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketsSoldPerDay(10))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Event with ID 10 does not exist.");
    }

    @Test
    void getTicketsSoldPerTicketType_ShouldFilterByTicketType() {
        Integer eventId = 10;
        Integer userId = 1;
        authenticateAs(userId, UserRole.organizer);

        User owner = new User();
        owner.setId(userId);
        event.setOrganizer(owner);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        DailyTicketTypeSalesProjection projection1 = mock(DailyTicketTypeSalesProjection.class);
        when(projection1.getDate()).thenReturn(LocalDate.of(2025, 8, 18));
        when(projection1.getTicketType()).thenReturn("VIP");
        when(projection1.getTotalQuantity()).thenReturn(20L);

        DailyTicketTypeSalesProjection projection2 = mock(DailyTicketTypeSalesProjection.class);
        when(projection2.getTicketType()).thenReturn("Standard");

        when(orderTicketRepository.findTicketSoldPerTicketTypeByEventId(eventId))
                .thenReturn(Arrays.asList(projection1, projection2));

        List<DailyTicketTypeSalesDTO> result = ticketService.getTicketsSoldPerTicketType(eventId, "VIP");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicketType()).isEqualTo("VIP");
        assertThat(result.getFirst().getTotalQuantity()).isEqualTo(20L);
    }

    @Test
    void getTicketsSoldPerTicketType_ShouldReturnAll_WhenTicketTypeIsNull() {
        Integer eventId = 10;
        Integer userId = 1;
        authenticateAs(userId, UserRole.organizer);

        User owner = new User();
        owner.setId(userId);
        event.setOrganizer(owner);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        DailyTicketTypeSalesProjection projection = mock(DailyTicketTypeSalesProjection.class);
        when(projection.getDate()).thenReturn(LocalDate.of(2025, 8, 18));
        when(projection.getTicketType()).thenReturn("VIP");
        when(projection.getTotalQuantity()).thenReturn(20L);

        when(orderTicketRepository.findTicketSoldPerTicketTypeByEventId(eventId))
                .thenReturn(Collections.singletonList(projection));

        List<DailyTicketTypeSalesDTO> result = ticketService.getTicketsSoldPerTicketType(eventId, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTicketType()).isEqualTo("VIP");
    }
}
