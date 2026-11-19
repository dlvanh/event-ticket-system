package com.example.event_ticket_system.event.service;

import com.example.event_ticket_system.dto.response.DetailEventResponseDto;
import com.example.event_ticket_system.dto.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.entity.*;
import com.example.event_ticket_system.enums.ApprovalStatus;
import com.example.event_ticket_system.enums.EventStatus;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.EventRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.repository.TicketRepository;
import com.example.event_ticket_system.security.CustomUserPrincipal;
import com.example.event_ticket_system.service.EventQueryService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private OrderTicketRepository orderTicketRepository;

    @InjectMocks
    private EventQueryService eventQueryService;

    private Event sampleEvent;
    private Ticket vipTicket;
    private Ticket stdTicket;

    @BeforeEach
    void setUp() {
        User organizer = new User();
        organizer.setId(1);
        organizer.setFullName("Eventa Org");
        organizer.setEmail("org@eventa.test");
        organizer.setRole(UserRole.organizer);

        Province province = new Province(1, "Hà Nội");
        District district = new District(1, province, "Ba Đình");
        Ward ward = new Ward(1, district, "Kim Mã");

        sampleEvent = new Event();
        sampleEvent.setEventId(10);
        sampleEvent.setEventName("Symphony Concert");
        sampleEvent.setDescription("Classical orchestra night");
        sampleEvent.setAddressName("Opera House");
        sampleEvent.setAddressDetail("1 Tràng Tiền");
        sampleEvent.setWard(ward);
        sampleEvent.setStartTime(LocalDateTime.now().plusDays(3));
        sampleEvent.setEndTime(LocalDateTime.now().plusDays(3).plusHours(3));
        sampleEvent.setCreatedAt(LocalDateTime.now());
        sampleEvent.setUpdatedAt(LocalDateTime.now());
        sampleEvent.setCategory("Live Music");
        sampleEvent.setStatus(EventStatus.upcoming);
        sampleEvent.setApprovalStatus(ApprovalStatus.approved);
        sampleEvent.setOrganizer(organizer);

        vipTicket = new Ticket(1, sampleEvent, new BigDecimal("500000.00"), "VIP", 50, 10, LocalDateTime.now(), LocalDateTime.now().plusDays(2));
        stdTicket = new Ticket(2, sampleEvent, new BigDecimal("150000.00"), "Standard", 200, 40, LocalDateTime.now(), LocalDateTime.now().plusDays(2));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Integer userId, UserRole role) {
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "u" + userId + "@test.com", "User", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    void getEventById_ShouldReturnFullDetailsWithFormattedAddressAndTickets() {
        when(eventRepository.findByIdWithDetails(10)).thenReturn(Optional.of(sampleEvent));
        when(ticketRepository.findByEventEventId(10)).thenReturn(List.of(vipTicket, stdTicket));

        DetailEventResponseDto dto = eventQueryService.getEventById(10);

        assertThat(dto.getEventId()).isEqualTo(10);
        assertThat(dto.getEventName()).isEqualTo("Symphony Concert");
        assertThat(dto.getAddress()).isEqualTo("Opera House, 1 Tràng Tiền, Kim Mã, Ba Đình, Hà Nội");
        assertThat(dto.getTicketPrices()).containsEntry("VIP", new BigDecimal("500000.00"));
        assertThat(dto.getTicketPrices()).containsEntry("Standard", new BigDecimal("150000.00"));
        assertThat(dto.getOrganizerName()).isEqualTo("Eventa Org");
    }

    @Test
    void getEventById_ShouldThrowNotFound_WhenMissing() {
        when(eventRepository.findByIdWithDetails(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventQueryService.getEventById(999))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getRecommendEvents_ShouldBatchLoadMinPriceAndPaidSales() {
        Page<Event> page = new PageImpl<>(List.of(sampleEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(ticketRepository.findByEventEventIdIn(List.of(10))).thenReturn(List.of(vipTicket, stdTicket));
        when(orderTicketRepository.sumPaidQuantityByEventIds(List.of(10)))
                .thenReturn(List.<Object[]>of(new Object[]{10, 50L}));

        Map<String, Object> result = eventQueryService.getRecommendEvents(
                "Live Music", null, null, null, null, 1, 10, "totalTicketSold:DESC");

        @SuppressWarnings("unchecked")
        List<RecommendEventsResponseDto> items = (List<RecommendEventsResponseDto>) result.get("listEvents");
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getMinPrice()).isEqualTo("150000.00");
        assertThat(items.getFirst().getTotalTicketSold()).isEqualTo(50L);
    }

    @Test
    void getPendingEvents_ShouldRejectNonAdmin() {
        authenticateAs(1, UserRole.organizer);

        assertThatThrownBy(() -> eventQueryService.getPendingEvents(null, null, null, null, 1, 10))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getEventsByOrganizer_ShouldRejectCustomer() {
        authenticateAs(2, UserRole.customer);

        assertThatThrownBy(() -> eventQueryService.getEventsByOrganizer(null, null, null, null, "Concert", 1, 10))
                .isInstanceOf(SecurityException.class);
    }
}
