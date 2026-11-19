package com.example.event_ticket_system.event.service;

import com.example.event_ticket_system.dto.request.EventRequestDto;
import com.example.event_ticket_system.dto.request.UpdateEventRequestDto;
import com.example.event_ticket_system.entity.*;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.repository.*;
import com.example.event_ticket_system.security.CustomUserPrincipal;
import com.example.event_ticket_system.service.EventCommandService;
import com.example.event_ticket_system.service.ImageStorageService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCommandServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private WardRepository wardRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private OrderTicketRepository orderTicketRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private ImageStorageService imageStorageService;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private MultipartFile logoFile;
    @Mock private MultipartFile backgroundFile;

    private EventCommandService eventCommandService;
    private EventRequestDto validDto;
    private User organizer;
    private Ward ward;

    @BeforeEach
    void setUp() {
        eventCommandService = new EventCommandService(
                eventRepository, userRepository, wardRepository,
                ticketRepository, orderTicketRepository, discountRepository,
                imageStorageService, transactionTemplate
        );

        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        lenient().doAnswer(inv -> {
            Consumer<TransactionStatus> action = inv.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);
        organizer.setFullName("Organizer A");
        organizer.setEmail("org@test.com");

        ward = new Ward();
        ward.setId(10);
        ward.setName("Ward 1");

        validDto = new EventRequestDto();
        validDto.setEventName("Rock Festival");
        validDto.setDescription("Live Music");
        validDto.setWardId(10);
        validDto.setAddressDetail("123 Main St");
        validDto.setAddressName("National Stadium");
        validDto.setCategory("Live Music");
        validDto.setStartTime(LocalDateTime.now().plusDays(2));
        validDto.setEndTime(LocalDateTime.now().plusDays(3));
        validDto.setSaleStart(LocalDateTime.now());
        validDto.setSaleEnd(LocalDateTime.now().plusDays(1));

        EventRequestDto.TicketRequest ticket = new EventRequestDto.TicketRequest();
        ticket.setTicketType("VIP");
        ticket.setQuantityTotal(100);
        ticket.setPrice(new BigDecimal("500000"));
        validDto.setTickets(List.of(ticket));

        EventRequestDto.DiscountRequest discount = new EventRequestDto.DiscountRequest();
        discount.setDiscountCode("ROCK10");
        discount.setDiscountDescription("10% Off");
        discount.setDiscountType("percentage");
        discount.setDiscountValue(new BigDecimal("10"));
        discount.setDiscountValidFrom(LocalDate.now());
        discount.setDiscountValidTo(LocalDate.now().plusDays(5));
        discount.setDiscountMaxUses(20);
        validDto.setDiscounts(List.of(discount));
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
    void createEvent_ShouldUploadImagesOutsideTxAndPersistEntities() {
        authenticateAs(1, UserRole.organizer);
        when(imageStorageService.uploadImage(logoFile)).thenReturn("https://img.example/logo.png");
        when(imageStorageService.uploadImage(backgroundFile)).thenReturn("https://img.example/bg.png");
        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(wardRepository.findById(10)).thenReturn(Optional.of(ward));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event saved = inv.getArgument(0);
            saved.setEventId(123);
            return saved;
        });

        Integer eventId = eventCommandService.createEvent(validDto, logoFile, backgroundFile);

        assertThat(eventId).isEqualTo(123);
        verify(imageStorageService).validateImageFile(logoFile);
        verify(imageStorageService).validateImageFile(backgroundFile);
        verify(ticketRepository).save(any(Ticket.class));
        verify(discountRepository).save(any(Discount.class));
    }

    @Test
    void createEvent_ShouldRejectNonOrganizer() {
        authenticateAs(2, UserRole.customer);

        assertThatThrownBy(() -> eventCommandService.createEvent(validDto, logoFile, backgroundFile))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(imageStorageService, eventRepository);
    }

    @Test
    void createEvent_ShouldRejectStartTimeAfterEndTime() {
        authenticateAs(1, UserRole.organizer);
        validDto.setStartTime(LocalDateTime.now().plusDays(5));
        validDto.setEndTime(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> eventCommandService.createEvent(validDto, logoFile, backgroundFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    void updateEvent_ShouldThrowSecurityException_WhenOrganizerDoesNotOwnEvent() {
        authenticateAs(99, UserRole.organizer);
        Event existing = new Event();
        existing.setEventId(123);
        existing.setOrganizer(organizer); // owned by id=1
        when(eventRepository.findById(123)).thenReturn(Optional.of(existing));

        UpdateEventRequestDto updateDto = new UpdateEventRequestDto();
        updateDto.setEventName("Hijacked Name");

        assertThatThrownBy(() -> eventCommandService.updateEvent(123, updateDto, null, null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void updateEvent_ShouldPreventDeletingTicketTypeWithExistingOrders() {
        authenticateAs(1, UserRole.organizer);
        Event existing = new Event();
        existing.setEventId(123);
        existing.setOrganizer(organizer);
        when(eventRepository.findById(123)).thenReturn(Optional.of(existing));

        Ticket existingVip = new Ticket();
        existingVip.setTicketId(5);
        existingVip.setTicketType("VIP");
        existingVip.setQuantityTotal(50);
        existingVip.setQuantitySold(3);
        when(ticketRepository.findByEvent(existing)).thenReturn(List.of(existingVip));
        when(orderTicketRepository.existsByTicket(existingVip)).thenReturn(true);

        UpdateEventRequestDto updateDto = new UpdateEventRequestDto();
        updateDto.setEventName("Updated Event");
        UpdateEventRequestDto.TicketTypeDto std = new UpdateEventRequestDto.TicketTypeDto();
        std.setTicketType("Standard");
        std.setQuantityTotal(100);
        std.setPrice(new BigDecimal("200000"));
        updateDto.setTicketTypes(List.of(std));

        assertThatThrownBy(() -> eventCommandService.updateEvent(123, updateDto, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VIP");
    }

    @Test
    void updateEvent_ShouldThrowEntityNotFound_WhenEventDoesNotExist() {
        authenticateAs(1, UserRole.organizer);
        when(eventRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventCommandService.updateEvent(404, new UpdateEventRequestDto(), null, null))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
