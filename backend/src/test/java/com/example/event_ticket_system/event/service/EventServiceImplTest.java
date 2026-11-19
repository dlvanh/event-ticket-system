package com.example.event_ticket_system.event.service;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.request.UpdateEventRequestDto;
import com.example.event_ticket_system.DTO.response.DetailEventResponseDto;
import com.example.event_ticket_system.DTO.response.GetEventsResponseDto;
import com.example.event_ticket_system.DTO.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.DTO.response.TicketExportDto;
import com.example.event_ticket_system.Entity.*;
import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Enums.ApprovalStatus;
import com.example.event_ticket_system.Enums.EventStatus;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Repository.*;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.Impl.EventServiceImpl;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.event_ticket_system.Enums.UserRole.organizer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @InjectMocks
    private EventServiceImpl eventService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WardRepository wardRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private OrderTicketRepository orderTicketRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MultipartFile logoFile;

    @Mock
    private MultipartFile backgroundFile;

    @Mock
    private HttpServletRequest request;

    @Spy
    @InjectMocks
    private EventServiceImpl spyEventService;

    private EventRequestDto eventRequestDto;
    private User mockUser;
    private Ward mockWard;
    private Event mockEvent;
    private District mockDistrict;
    private Province mockProvince;
    private Ticket mockTicket;

    @BeforeEach
    void setUp() {
        eventRequestDto = new EventRequestDto();
        eventRequestDto.setEventName("Sample Event");
        eventRequestDto.setDescription("Sample Description");
        eventRequestDto.setWardId(1);
        eventRequestDto.setAddressDetail("123 Street");
        eventRequestDto.setAddressName("Sample Venue");
        eventRequestDto.setCategory("Music");
        eventRequestDto.setStartTime(LocalDateTime.now().plusDays(1));
        eventRequestDto.setEndTime(LocalDateTime.now().plusDays(2));
        eventRequestDto.setSaleStart(LocalDateTime.now());
        eventRequestDto.setSaleEnd(LocalDateTime.now().plusDays(1));

        EventRequestDto.TicketRequest ticket = new EventRequestDto.TicketRequest();
        ticket.setTicketType("VIP");
        ticket.setQuantityTotal(100);
        ticket.setPrice(50.0);
        eventRequestDto.setTickets(List.of(ticket));

        EventRequestDto.DiscountRequest discount = new EventRequestDto.DiscountRequest();
        discount.setDiscountCode("DISC10");
        discount.setDiscountDescription("10% Off");
        discount.setDiscountType("percentage");
        discount.setDiscountValue(10.0);
        discount.setDiscountValidFrom(LocalDate.now());
        discount.setDiscountValidTo(LocalDate.now().plusDays(5));
        discount.setDiscountMaxUses(100);
        eventRequestDto.setDiscounts(List.of(discount));

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setRole(organizer);
        mockUser.setFullName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setBio("Organizer Bio");
        mockUser.setProfilePicture("avatar.jpg");

        mockProvince = new Province();
        mockProvince.setId(1);
        mockProvince.setName("Sample Province");

        mockDistrict = new District();
        mockDistrict.setId(1);
        mockDistrict.setName("Sample District");
        mockDistrict.setProvince(mockProvince);

        mockWard = new Ward();
        mockWard.setId(1);
        mockWard.setName("Sample Ward");
        mockWard.setDistrict(mockDistrict);

        mockEvent = new Event();
        mockEvent.setEventId(123);
        mockEvent.setEventName("Music Concert");
        mockEvent.setDescription("Live Music");
        mockEvent.setAddressName("Venue A");
        mockEvent.setAddressDetail("123 Street");
        mockEvent.setWard(mockWard);
        mockEvent.setStartTime(LocalDateTime.now().plusDays(1));
        mockEvent.setEndTime(LocalDateTime.now().plusDays(2));
        mockEvent.setCreatedAt(LocalDateTime.now());
        mockEvent.setUpdatedAt(LocalDateTime.now());
        mockEvent.setCategory("Music");
        mockEvent.setStatus(EventStatus.upcoming);
        mockEvent.setApprovalStatus(ApprovalStatus.approved);
        mockEvent.setLogoUrl("logo.jpg");
        mockEvent.setBackgroundUrl("bg.jpg");
        mockEvent.setOrganizer(mockUser);

        mockTicket = new Ticket();
        mockTicket.setTicketId(1);
        mockTicket.setTicketType("VIP");
        mockTicket.setPrice(100.0);
        mockTicket.setQuantityTotal(50);
        mockTicket.setQuantitySold(10);
        mockTicket.setSaleStart(LocalDateTime.now());
        mockTicket.setSaleEnd(LocalDateTime.now().plusDays(1));

        // Mock JWT
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer token");
        lenient().when(jwtUtil.extractUserId("token")).thenReturn(1);

        // Mock repository
        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent), PageRequest.of(0, 10), 1);
        lenient().when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        lenient().when(ticketRepository.findByEvent(any(Event.class))).thenReturn(List.of(mockTicket));
        lenient().when(orderTicketRepository.sumQuantityByEvent(any(Event.class))).thenReturn(50L);
    }

    // Create Event
    @Test
    void createEvent_Success_ShouldReturnEventId() throws Exception {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.of(mockWard));

        when(logoFile.getContentType()).thenReturn("image/jpeg");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        doReturn("http://image.com/logo.jpg").when(spyEventService).uploadImageToImgbb(logoFile);
        doReturn("http://image.com/background.jpg").when(spyEventService).uploadImageToImgbb(backgroundFile);

        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

        Integer eventId = spyEventService.createEvent(eventRequestDto, logoFile, backgroundFile, request);

        assertEquals(123, eventId);
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(ticketRepository, times(1)).save(any(Ticket.class));
        verify(discountRepository, times(1)).save(any(Discount.class));
    }

    @Test
    void createEvent_WhenUserIsNotOrganizer_ShouldThrowSecurityException() {
        mockUser.setRole(UserRole.customer); // Not organizer
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        assertThrows(SecurityException.class, () ->
                eventService.createEvent(eventRequestDto, logoFile, backgroundFile, request));
    }

    @Test
    void createEvent_InvalidFileType_ShouldThrowIllegalArgumentException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.of(mockWard));

        when(logoFile.getContentType()).thenReturn("text/plain"); // Invalid type

        assertThrows(IllegalArgumentException.class, () ->
                eventService.createEvent(eventRequestDto, logoFile, backgroundFile, request));
    }

    @Test
    void createEvent_InvalidBackgroundFileType_ShouldThrowIllegalArgumentException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.of(mockWard));

        when(logoFile.getContentType()).thenReturn("image/jpeg"); // valid
        when(backgroundFile.getContentType()).thenReturn("text/plain"); // invalid

        assertThrows(IllegalArgumentException.class, () ->
                eventService.createEvent(eventRequestDto, logoFile, backgroundFile, request));
    }

    @Test
    void createEvent_NoDiscounts_ShouldNotSaveDiscounts() throws Exception {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.of(mockWard));

        when(logoFile.getContentType()).thenReturn("image/jpeg");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload image
        doReturn("http://image.com/logo.jpg").when(spyEventService).uploadImageToImgbb(logoFile);
        doReturn("http://image.com/background.jpg").when(spyEventService).uploadImageToImgbb(backgroundFile);

        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

        // Set discounts = null
        eventRequestDto.setDiscounts(null);

        Integer eventId = spyEventService.createEvent(eventRequestDto, logoFile, backgroundFile, request);

        assertEquals(123, eventId);
        verify(discountRepository, never()).save(any(Discount.class));
    }

    @Test
    void createEvent_WithDiscounts_ShouldSaveDiscounts() throws Exception {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.of(mockWard));

        when(logoFile.getContentType()).thenReturn("image/jpeg");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload image
        doReturn("http://image.com/logo.jpg").when(spyEventService).uploadImageToImgbb(logoFile);
        doReturn("http://image.com/background.jpg").when(spyEventService).uploadImageToImgbb(backgroundFile);

        when(eventRepository.save(any(Event.class))).thenReturn(mockEvent);

        // Set discount list
        EventRequestDto.DiscountRequest discountRequest = new EventRequestDto.DiscountRequest();
        discountRequest.setDiscountCode("DISCOUNT10");
        discountRequest.setDiscountDescription("10% off");
        discountRequest.setDiscountType("percentage");
        discountRequest.setDiscountValue(10.0);
        discountRequest.setDiscountValidFrom(LocalDate.now());
        discountRequest.setDiscountValidTo(LocalDate.now().plusDays(5));
        discountRequest.setDiscountMaxUses(100);
        eventRequestDto.setDiscounts(List.of(discountRequest));

        Integer eventId = spyEventService.createEvent(eventRequestDto, logoFile, backgroundFile, request);

        assertEquals(123, eventId);
        verify(discountRepository, times(1)).save(any(Discount.class));
    }

    @Test
    void createEvent_WhenWardNotFound_ShouldThrowEntityNotFoundException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                eventService.createEvent(eventRequestDto, logoFile, backgroundFile, request));
    }

    @Test
    void createEvent_WhenInvalidImageType_ShouldThrowIllegalArgumentException() {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.of(mockWard));

        when(logoFile.getContentType()).thenReturn("application/pdf"); // Invalid

        assertThrows(IllegalArgumentException.class, () ->
                eventService.createEvent(eventRequestDto, logoFile, backgroundFile, request));
    }

    @Test
    void createEvent_WhenUploadFails_ShouldThrowRuntimeException() throws Exception {
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));
        when(wardRepository.findById(1)).thenReturn(Optional.of(mockWard));
        when(logoFile.getContentType()).thenReturn("image/jpeg");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        doThrow(new RuntimeException("Upload failed")).when(spyEventService).uploadImageToImgbb(logoFile);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                spyEventService.createEvent(eventRequestDto, logoFile, backgroundFile, request));

        assertTrue(ex.getMessage().contains("Tạo sự kiện thất bại"));
    }

    @Test
    void uploadImageToImgbb_ShouldReturnImageUrl() throws Exception {
        // Arrange
        byte[] mockBytes = "fake-image".getBytes();
        when(logoFile.getBytes()).thenReturn(mockBytes);

        // Fake JSON response
        String fakeJsonResponse = "{ \"data\": { \"url\": \"http://image.com/fake.jpg\" } }";

        // Mock HttpResponse
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.body()).thenReturn(fakeJsonResponse);

        // Mock HttpClient
        HttpClient mockHttpClient = mock(HttpClient.class);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        // Mock HttpClient.newHttpClient()
        try (MockedStatic<HttpClient> mockedStatic = mockStatic(HttpClient.class)) {
            mockedStatic.when(HttpClient::newHttpClient).thenReturn(mockHttpClient);

            // Act
            String result = eventService.uploadImageToImgbb(logoFile);

            // Assert
            assertEquals("http://image.com/fake.jpg", result);
        }
    }

    // Get events by organizer
    @Test
    void shouldThrowEntityNotFoundException_WhenUserNotExist() {
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            eventService.getEventsByOrganizer(request, null, null, null, null, null, 1, 10);
        });
    }

    @Test
    void shouldThrowSecurityException_WhenUserIsNotOrganizer() {
        User user = new User();
        user.setId(1);
        user.setRole(UserRole.customer);

        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThrows(SecurityException.class, () -> {
            eventService.getEventsByOrganizer(request, null, null, null, null, null, 1, 10);
        });
    }

    @Test
    void shouldReturnEvents_WhenAllFiltersAreNull() {
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Event event = new Event();
        event.setEventId(100);
        event.setEventName("Music Fest");
        event.setStatus(EventStatus.upcoming);
        event.setApprovalStatus(ApprovalStatus.pending);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(2));
        event.setUpdatedAt(LocalDateTime.now());
        event.setLogoUrl("logo.png");

        Page<Event> page = new PageImpl<>(Collections.singletonList(event));

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = eventService.getEventsByOrganizer(
                request, null, null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, ((List<?>) result.get("listEvents")).size());
        assertEquals(1, result.get("pageNo"));
        assertEquals(1, result.get("totalPages"));
    }

    @Test
    void shouldApplyAllFilters_WhenValuesProvided() {
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Event event = new Event();
        event.setEventId(101);
        event.setEventName("Tech Summit");
        event.setStatus(EventStatus.upcoming);
        event.setApprovalStatus(ApprovalStatus.pending);
        event.setStartTime(LocalDateTime.now().plusDays(5));
        event.setEndTime(LocalDateTime.now().plusDays(6));
        event.setUpdatedAt(LocalDateTime.now());
        event.setLogoUrl("logo2.png");

        Page<Event> page = new PageImpl<>(Collections.singletonList(event));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = eventService.getEventsByOrganizer(
                request,
                "upcoming",
                "pending",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7),
                "Tech",
                2,
                10
        );

        assertNotNull(result);
        assertEquals(1, ((List<?>) result.get("listEvents")).size());
        assertEquals(1, result.get("pageNo"));
    }

    @Test
    void getEventsByOrganizer_ShouldNotDecrementPage_WhenPageIsZero() {
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Event event = new Event();
        event.setEventId(102);
        event.setEventName("Art Exhibition");
        event.setStatus(EventStatus.upcoming);
        event.setApprovalStatus(ApprovalStatus.pending);
        event.setStartTime(LocalDateTime.now().plusDays(3));
        event.setEndTime(LocalDateTime.now().plusDays(4));
        event.setUpdatedAt(LocalDateTime.now());
        event.setLogoUrl("logo3.png");

        Page<Event> page = new PageImpl<>(Collections.singletonList(event));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Map<String, Object> result = eventService.getEventsByOrganizer(
                request,
                "upcoming",
                "pending",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5),
                "Art",
                0, // Page is zero
                10
        );

        assertNotNull(result);
        assertEquals(1, ((List<?>) result.get("listEvents")).size());
        assertEquals(1, result.get("pageNo"));
    }

    @Test
    void getEventsByOrganizer_ShouldApplyStatusPredicate() {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        String status = "upcoming";

        eventService.getEventsByOrganizer(request, status, null, null, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = captor.getValue();

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        Path<Object> statusPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        when(root.get("status")).thenReturn(statusPath);

        Predicate mockPredicate = mock(Predicate.class);
        lenient().when(cb.equal(statusPath, status)).thenReturn(mockPredicate);

        capturedSpec.toPredicate(root, query, cb);

        verify(root).get("status");
        verify(cb).equal(statusPath, status);
    }

    @Test
    void getEventsByOrganizer_ShouldApplyApprovalStatusPredicate() {
        // Arrange
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        String approvalStatus = "pending";

        // Act
        eventService.getEventsByOrganizer(request, null, approvalStatus, null, null, null, 1, 10);

        // Assert
        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = captor.getValue();

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        Path<Object> approvalStatusPath = mock(Path.class);

        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        when(root.get("approvalStatus")).thenReturn(approvalStatusPath);

        Predicate mockPredicate = mock(Predicate.class);
        lenient().when(cb.equal(approvalStatusPath, approvalStatus)).thenReturn(mockPredicate);

        // Thực thi Specification
        capturedSpec.toPredicate(root, query, cb);

        // Verify gọi đúng field và giá trị
        verify(root).get("approvalStatus");
        verify(cb).equal(approvalStatusPath, approvalStatus);
    }

    @Test
    void getEventsByOrganizer_ShouldApplyStartAndEndTimeRange() {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(5);

        eventService.getEventsByOrganizer(request, null, null, startTime, endTime, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> spec = captor.getValue();

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        Path startPath = mock(Path.class);
        Path endPath = mock(Path.class);

        when(root.get("startTime")).thenReturn(startPath);
        when(root.get("endTime")).thenReturn(endPath);


        Predicate lessOrEqual = mock(Predicate.class);
        Predicate greaterOrEqual = mock(Predicate.class);
        Predicate combined = mock(Predicate.class);

        when(cb.lessThanOrEqualTo(startPath, endTime)).thenReturn(lessOrEqual);
        when(cb.greaterThanOrEqualTo(endPath, startTime)).thenReturn(greaterOrEqual);
        when(cb.and(lessOrEqual, greaterOrEqual)).thenReturn(combined);

        spec.toPredicate(root, query, cb);

        verify(cb).lessThanOrEqualTo(startPath, endTime);
        verify(cb).greaterThanOrEqualTo(endPath, startTime);
        verify(cb).and(lessOrEqual, greaterOrEqual);
    }

    @Test
    void getEventsByOrganizer_ShouldApplyStartTimeOnly() {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        LocalDateTime startTime = LocalDateTime.now();

        eventService.getEventsByOrganizer(request, null, null, startTime, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> spec = captor.getValue();

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        Path endPath = mock(Path.class);
        lenient().when(root.get("endTime")).thenReturn(endPath);

        Predicate predicate = mock(Predicate.class);
        when(cb.greaterThanOrEqualTo(endPath, startTime)).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(endPath, startTime);
    }

    @Test
    void getEventsByOrganizer_ShouldApplyEndTimeOnly() {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        LocalDateTime endTime = LocalDateTime.now().plusDays(5);

        eventService.getEventsByOrganizer(request, null, null, null, endTime, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> spec = captor.getValue();

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        Path startPath = mock(Path.class);
        lenient().when(root.get("startTime")).thenReturn(startPath);

        Predicate predicate = mock(Predicate.class);
        when(cb.lessThanOrEqualTo(startPath, endTime)).thenReturn(predicate);

        spec.toPredicate(root, query, cb);

        verify(cb).lessThanOrEqualTo(startPath, endTime);
    }

    @Test
    void getEventsByOrganizer_ShouldApplyNameFilter() {
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        String name = "music";

        eventService.getEventsByOrganizer(request, null, null, null, null, name, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> spec = captor.getValue();

        // Use real CriteriaBuilder via mockito or Hibernate CriteriaBuilder is complex
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        Predicate predicate = mock(Predicate.class);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result, "Predicate should not be null");
    }

    @Test
    void shouldNotApplyApproveStatusPredicate_WhenStatusIsNullOrEmpty() {
        String approvalStatus = ""; // Empty string
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        eventService.getEventsByOrganizer(request, null, approvalStatus, null, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        Path<Object> approvalStausPath = mock(Path.class);
        lenient().when(root.get("approvalStatus")).thenReturn(approvalStausPath);

        capturedSpec.toPredicate(root, query, cb);

        verify(cb, never()).equal(any(), any());
    }

    @Test
    void shouldNotApplyNamePredicate_WhenStatusIsNullOrEmpty() {
        String name = ""; // Empty string
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        eventService.getEventsByOrganizer(request, null, null, null, null, name, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        Path<Object> namePath = mock(Path.class);
        lenient().when(root.get("eventName")).thenReturn(namePath);

        capturedSpec.toPredicate(root, query, cb);

        verify(cb, never()).equal(any(), any());
    }

    @Test
    void shouldNotApplyStatusPredicate_WhenStatusIsNullOrEmpty() {
        String status = ""; // Empty string
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        eventService.getEventsByOrganizer(request, status, null, null, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> organizerPath = mock(Path.class);
        when(root.get("organizer")).thenReturn(organizerPath);
        when(organizerPath.get("id")).thenReturn(mock(Path.class));
        Path<Object> statusPath = mock(Path.class);
        lenient().when(root.get("status")).thenReturn(statusPath);

        capturedSpec.toPredicate(root, query, cb);

        verify(cb, never()).equal(any(), any());
    }

    // Get event by id
    @Test
    void getEventById_ShouldReturnDetailEventResponseDto_WhenEventExists() {
        when(eventRepository.findById(123)).thenReturn(Optional.of(mockEvent));
        when(ticketRepository.findByEvent(mockEvent)).thenReturn(List.of(mockTicket));

        DetailEventResponseDto result = eventService.getEventById(123, request);

        assertNotNull(result);
        assertEquals(123, result.getEventId());
        assertEquals("Music Concert", result.getEventName());
        assertEquals("Live Music", result.getDescription());
        assertTrue(result.getAddress().contains("Venue A"));
        assertEquals("Music", result.getCategory());
        assertEquals("upcoming", result.getStatus());
        assertEquals("approved", result.getApprovalStatus());
        assertEquals("John Doe", result.getOrganizerName());
        assertTrue(result.getTicketTypes().containsValue("VIP"));
        assertEquals(100.0, result.getTicketPrices().get("VIP"));
    }

    @Test
    void getEventById_ShouldThrowException_WhenEventNotFound() {
        when(eventRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            eventService.getEventById(999, request);
        });
    }

    @Test
    void getEventById_ShouldHandleNullFields() {
        mockEvent.setAddressName(null);
        mockEvent.setAddressDetail(null);
        mockEvent.setRejectionReason(null);

        when(eventRepository.findById(123)).thenReturn(Optional.of(mockEvent));
        when(ticketRepository.findByEvent(mockEvent)).thenReturn(Collections.emptyList());

        DetailEventResponseDto result = eventService.getEventById(123, request);

        assertNotNull(result);
        assertTrue(result.getAddress().startsWith("Sample Ward")); // không bị lỗi null
        assertEquals("N/A", result.getRejectReason());
        assertTrue(result.getTicketTypes().isEmpty());
    }

    @Test
    void getEventById_ShouldHandleMultipleTickets() {
        Ticket ticket2 = new Ticket();
        ticket2.setTicketId(2);
        ticket2.setTicketType("Standard");
        ticket2.setPrice(50.0);
        ticket2.setQuantityTotal(100);
        ticket2.setQuantitySold(30);

        when(eventRepository.findById(123)).thenReturn(Optional.of(mockEvent));
        when(ticketRepository.findByEvent(mockEvent)).thenReturn(List.of(mockTicket, ticket2));

        DetailEventResponseDto result = eventService.getEventById(123, request);

        assertEquals(2, result.getTicketTypes().size());
        assertEquals(100.0, result.getTicketPrices().get("VIP"));
        assertEquals(50.0, result.getTicketPrices().get("Standard"));
    }

    @Test
    void testGetEventById_WithRejectionReason() {
        // Arrange
        mockEvent.setRejectionReason("Invalid details");

        when(eventRepository.findById(123)).thenReturn(Optional.of(mockEvent));

        Ticket ticket = new Ticket();
        ticket.setTicketId(1);
        ticket.setTicketType("VIP");
        ticket.setPrice(50.0);
        ticket.setQuantityTotal(100);
        ticket.setQuantitySold(10);
        ticket.setSaleStart(LocalDateTime.now());
        ticket.setSaleEnd(LocalDateTime.now().plusDays(1));

        when(ticketRepository.findByEvent(mockEvent)).thenReturn(List.of(ticket));

        // Act
        DetailEventResponseDto response = eventService.getEventById(123, request);

        // Assert
        assertEquals("Invalid details", response.getRejectReason());
    }

    // Get recommend events
    @Test
    void testGetRecommendEvents_ShouldReturnCorrectResponse() {
        // Given
        String category = "Music";
        String address = "Sample";
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(3);
        String name = "Concert";
        Integer page = 1;
        Integer size = 10;
        String sortBy = "updatedAt:DESC";

        // When
        Map<String, Object> result = eventService.getRecommendEvents(category, address, startTime, endTime, name, page, size, sortBy);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("listEvents"));

        List<?> events = (List<?>) result.get("listEvents");
        assertEquals(1, events.size());

        RecommendEventsResponseDto dto = (RecommendEventsResponseDto) events.get(0);
        assertEquals("Music Concert", dto.getEventName());
        assertEquals("Music", dto.getCategory());
        assertEquals("100.0", dto.getMinPrice());
        assertEquals(50L, dto.getTotalTicketSold());

        assertEquals(10, result.get("pageSize"));
        assertEquals(1, result.get("pageNo"));
        assertEquals(1, result.get("totalPages"));
    }

    @Test
    void getRecommendEvents_ShouldNotDecrementPage_WhenPageIsZero() {
        // Given
        String category = "Music";
        String address = "Sample";
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(3);
        String name = "Concert";
        Integer page = 0; // Page is zero
        Integer size = 10;
        String sortBy = "updatedAt:DESC";

        // When
        Map<String, Object> result = eventService.getRecommendEvents(category, address, startTime, endTime, name, page, size, sortBy);

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("listEvents"));

        List<?> events = (List<?>) result.get("listEvents");
        assertEquals(1, events.size());

        RecommendEventsResponseDto dto = (RecommendEventsResponseDto) events.get(0);
        assertEquals("Music Concert", dto.getEventName());
        assertEquals("Music", dto.getCategory());
        assertEquals("100.0", dto.getMinPrice());
        assertEquals(50L, dto.getTotalTicketSold());

        assertEquals(10, result.get("pageSize"));
        assertEquals(1, result.get("pageNo")); // Should be 1, not 0
        assertEquals(1, result.get("totalPages"));
    }

    @Test
    void getRecommendEvents_ShouldNotApplySortBy_WhenSortByIsNullOrEmpty() {
        // Given
        String category = "Music";
        String address = "Sample";
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(3);
        String name = "Concert";
        Integer page = 1;
        Integer size = 10;

        // When
        Map<String, Object> result = eventService.getRecommendEvents(category, address, startTime, endTime, name, page, size, " ");

        // Then
        assertNotNull(result);
        assertTrue(result.containsKey("listEvents"));

        List<?> events = (List<?>) result.get("listEvents");
        assertEquals(1, events.size());

        RecommendEventsResponseDto dto = (RecommendEventsResponseDto) events.get(0);
        assertEquals("Music Concert", dto.getEventName());
        assertEquals("Music", dto.getCategory());
        assertEquals("100.0", dto.getMinPrice());
        assertEquals(50L, dto.getTotalTicketSold());

        assertEquals(10, result.get("pageSize"));
        assertEquals(1, result.get("pageNo"));
        assertEquals(1, result.get("totalPages"));
    }

    @Test
    void testGetRecommendEvents_ShouldSortByEventNameAsc() {
        // Given
        when(orderTicketRepository.sumQuantityByEvent(any(Event.class))).thenReturn(50L);

        String sortBy = "eventName:ASC";

        // When
        Map<String, Object> result = eventService.getRecommendEvents(
                "Music", "Sample", LocalDateTime.now(), LocalDateTime.now().plusDays(3),
                "Concert", 1, 10, sortBy
        );

        // Then
        assertNotNull(result);
        assertEquals(1, ((List<?>) result.get("listEvents")).size());
    }

    @Test
    void testGetRecommendEvents_ShouldSortByTotalTicketSoldAsc() {
        // Given
        Event secondEvent = new Event();
        secondEvent.setEventId(456);
        secondEvent.setEventName("Rock Concert");
        secondEvent.setAddressName("Venue B");
        secondEvent.setAddressDetail("456 Street");
        secondEvent.setWard(mockEvent.getWard());
        secondEvent.setStartTime(LocalDateTime.now().plusDays(1));
        secondEvent.setEndTime(LocalDateTime.now().plusDays(2));
        secondEvent.setCategory("Music");
        secondEvent.setStatus(EventStatus.upcoming);
        secondEvent.setApprovalStatus(ApprovalStatus.approved);
        secondEvent.setLogoUrl("logo2.jpg");
        secondEvent.setBackgroundUrl("bg2.jpg");

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent, secondEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(ticketRepository.findByEvent(any(Event.class))).thenReturn(List.of(mockTicket));
        when(orderTicketRepository.sumQuantityByEvent(mockEvent)).thenReturn(100L);
        when(orderTicketRepository.sumQuantityByEvent(secondEvent)).thenReturn(50L);

        String sortBy = "totalTicketSold:ASC";

        // When
        Map<String, Object> result = eventService.getRecommendEvents(
                "Music", "Sample", LocalDateTime.now(), LocalDateTime.now().plusDays(3),
                null, 1, 10, sortBy
        );

        // Then
        List<RecommendEventsResponseDto> events = (List<RecommendEventsResponseDto>) result.get("listEvents");
        assertEquals(2, events.size());
        assertTrue(events.get(0).getTotalTicketSold() <= events.get(1).getTotalTicketSold());
    }

    @Test
    void testGetRecommendEvents_ShouldSortByTotalTicketSoldDesc() {
        // Given
        Event secondEvent = new Event();
        secondEvent.setEventId(456);
        secondEvent.setEventName("Rock Concert");
        secondEvent.setAddressName("Venue B");
        secondEvent.setAddressDetail("456 Street");
        secondEvent.setWard(mockEvent.getWard());
        secondEvent.setStartTime(LocalDateTime.now().plusDays(1));
        secondEvent.setEndTime(LocalDateTime.now().plusDays(2));
        secondEvent.setCategory("Music");
        secondEvent.setStatus(EventStatus.upcoming);
        secondEvent.setApprovalStatus(ApprovalStatus.approved);
        secondEvent.setLogoUrl("logo2.jpg");
        secondEvent.setBackgroundUrl("bg2.jpg");

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent, secondEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);
        when(ticketRepository.findByEvent(any(Event.class))).thenReturn(List.of(mockTicket));
        when(orderTicketRepository.sumQuantityByEvent(mockEvent)).thenReturn(50L);
        when(orderTicketRepository.sumQuantityByEvent(secondEvent)).thenReturn(100L);

        String sortBy = "totalTicketSold:DESC";

        // When
        Map<String, Object> result = eventService.getRecommendEvents(
                "Music", "Sample", LocalDateTime.now(), LocalDateTime.now().plusDays(3),
                null, 1, 10, sortBy
        );

        // Then
        List<RecommendEventsResponseDto> events = (List<RecommendEventsResponseDto>) result.get("listEvents");
        assertEquals(2, events.size());
        assertTrue(events.get(0).getTotalTicketSold() >= events.get(1).getTotalTicketSold());
    }

    @Test
    void testGetRecommendEvents_ShouldSkipAddressDetailIfNull() {
        // Given
        mockEvent.setAddressDetail(null);
        when(orderTicketRepository.sumQuantityByEvent(any(Event.class))).thenReturn(50L);

        Map<String, Object> result = eventService.getRecommendEvents(
                "Music", "Sample", LocalDateTime.now(), LocalDateTime.now().plusDays(3),
                null, 1, 10, "eventName:ASC"
        );

        List<RecommendEventsResponseDto> events = (List<RecommendEventsResponseDto>) result.get("listEvents");
        String address = events.getFirst().getAddress();

        assertTrue(address.contains("Venue A"));
        assertFalse(address.contains("null"));
    }

    @Test
    void testGetRecommendEvents_ShouldSkipAddressNameIfNull() {
        // Given
        mockEvent.setAddressName(null);
        when(orderTicketRepository.sumQuantityByEvent(any(Event.class))).thenReturn(50L);

        Map<String, Object> result = eventService.getRecommendEvents(
                "Music", "Sample", LocalDateTime.now(), LocalDateTime.now().plusDays(3),
                null, 1, 10, "eventName:ASC"
        );

        List<RecommendEventsResponseDto> events = (List<RecommendEventsResponseDto>) result.get("listEvents");
        String address = events.getFirst().getAddress();

        assertFalse(address.startsWith("null"));
        assertTrue(address.contains("123 Street"));
    }

    // Get pending events
    @Test
    public void testGetPendingEvents_success() {
        // Mock JWT token extraction
        when(request.getHeader("Authorization")).thenReturn("Bearer fakeToken");
        when(jwtUtil.extractUserId("fakeToken")).thenReturn(1);

        // Mock user
        User user = new User();
        user.setId(1);
        user.setRole(UserRole.admin);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Mock event data
        Event event = new Event();
        event.setEventId(100);
        event.setEventName("Test Event");
        event.setStatus(EventStatus.upcoming);
        event.setApprovalStatus(ApprovalStatus.pending);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(2));
        event.setUpdatedAt(LocalDateTime.now());
        User organizer = new User();
        organizer.setFullName("Organizer Name");
        event.setOrganizer(organizer);

        List<Event> events = Collections.singletonList(event);
        Page<Event> eventPage = new PageImpl<>(events, PageRequest.of(0, 10), 1);

        // Mock repository call
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(eventPage);

        // Call the service
        Map<String, Object> response = eventService.getPendingEvents(
                request, "some address", null, null, "Test", 1, 10
        );

        // Assertions
        assertNotNull(response);
        assertTrue(response.containsKey("listEvents"));
        List<GetEventsResponseDto> eventList = (List<GetEventsResponseDto>) response.get("listEvents");
        assertEquals(1, eventList.size());

        GetEventsResponseDto eventDto = eventList.getFirst();
        assertEquals("Test Event", ((GetEventsResponseDto) eventList.getFirst()).getEventName());
        assertEquals("Organizer Name", ((GetEventsResponseDto) eventList.getFirst()).getOrganizerName());
        assertEquals(1, response.get("pageNo"));
        assertEquals(1, response.get("totalPages"));
    }

    @Test
    public void testGetPendingEvents_notAdmin() {
        // Mock JWT token extraction
        when(request.getHeader("Authorization")).thenReturn("Bearer fakeToken");
        when(jwtUtil.extractUserId("fakeToken")).thenReturn(2);

        // Mock non-admin user
        User user = new User();
        user.setId(2);
        user.setRole(UserRole.customer);
        when(userRepository.findById(2)).thenReturn(Optional.of(user));

        // Expect SecurityException
        assertThrows(SecurityException.class, () -> {
            eventService.getPendingEvents(request, null, null, null, null, 1, 10);
        });
    }

    @Test
    public void testGetPendingEvents_userNotFound() {
        when(request.getHeader("Authorization")).thenReturn("Bearer fakeToken");
        when(jwtUtil.extractUserId("fakeToken")).thenReturn(99);
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            eventService.getPendingEvents(request, null, null, null, null, 1, 10);
        });
    }

    @Test
    void getPendingEvents_ShouldNotDecrementPage_WhenPageIsZero() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer fakeToken");
        when(jwtUtil.extractUserId("fakeToken")).thenReturn(1);

        User user = new User();
        user.setId(1);
        user.setRole(UserRole.admin);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event event = new Event();
        event.setEventId(100);
        event.setEventName("Test Event");
        event.setStatus(EventStatus.upcoming);
        event.setApprovalStatus(ApprovalStatus.pending);
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(2));
        event.setUpdatedAt(LocalDateTime.now());
        event.setOrganizer(user);

        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(event), PageRequest.of(0, 10), 1);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(eventPage);

        // Act
        Map<String, Object> response = eventService.getPendingEvents(request, null, null, null, null, 0, 10);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("listEvents"));
        List<GetEventsResponseDto> eventList = (List<GetEventsResponseDto>) response.get("listEvents");
        assertEquals(1, eventList.size());
        assertEquals(1, response.get("pageNo")); // Page number should be 1, not 0
    }

    @Test
    void getPendingEvents_ShouldBuildPredicatesCorrectly() {
        // Mock user extraction
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        // Mock pageable return
        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        String address = "Main Street";
        String name = "Test Event";
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        eventService.getPendingEvents(request, address, startTime, endTime, name, 1, 10);

        // Capture specification passed to repository
        ArgumentCaptor<Specification<Event>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(specCaptor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = specCaptor.getValue();

        // Mock CriteriaBuilder and Root
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path addressNamePath = mock(Path.class);
        Path addressDetailPath = mock(Path.class);
        Path wardPath = mock(Path.class);
        Path wardNamePath = mock(Path.class);
        Path districtPath = mock(Path.class);
        Path districtNamePath = mock(Path.class);
        Path provincePath = mock(Path.class);
        Path provinceNamePath = mock(Path.class);
        Path eventNamePath = mock(Path.class);
        Path approvalStatusPath = mock(Path.class);
        Path startTimePath = mock(Path.class);
        Path endTimePath = mock(Path.class);

        when(root.get("addressName")).thenReturn(addressNamePath);
        when(root.get("addressDetail")).thenReturn(addressDetailPath);
        when(root.get("ward")).thenReturn(wardPath);
        when(wardPath.get("name")).thenReturn(wardNamePath);
        when(wardPath.get("district")).thenReturn(districtPath);
        when(districtPath.get("name")).thenReturn(districtNamePath);
        when(districtPath.get("province")).thenReturn(provincePath);
        when(provincePath.get("name")).thenReturn(provinceNamePath);
        when(root.get("eventName")).thenReturn(eventNamePath);
        when(root.get("approvalStatus")).thenReturn(approvalStatusPath);
        when(root.get("startTime")).thenReturn(startTimePath);
        when(root.get("endTime")).thenReturn(endTimePath);

        Predicate addressPredicate = mock(Predicate.class);
        Predicate startEndPredicate = mock(Predicate.class);
        Predicate namePredicate = mock(Predicate.class);
        Predicate approvalPredicate = mock(Predicate.class);

        when(cb.lower(addressNamePath)).thenReturn(addressNamePath);
        when(cb.lower(addressDetailPath)).thenReturn(addressDetailPath);
        when(cb.lower(wardNamePath)).thenReturn(wardNamePath);
        when(cb.lower(districtNamePath)).thenReturn(districtNamePath);
        when(cb.lower(provinceNamePath)).thenReturn(provinceNamePath);
        when(cb.lower(eventNamePath)).thenReturn(eventNamePath);

        when(cb.like(any(), anyString())).thenReturn(addressPredicate);
        when(cb.or(any(), any(), any(), any(), any())).thenReturn(addressPredicate);
        when(cb.lessThanOrEqualTo(startTimePath, endTime)).thenReturn(startEndPredicate);
        when(cb.greaterThanOrEqualTo(endTimePath, startTime)).thenReturn(startEndPredicate);
        when(cb.like(eventNamePath, "%test event%")).thenReturn(namePredicate);
        when(cb.equal(approvalStatusPath, ApprovalStatus.pending)).thenReturn(approvalPredicate);
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        // Execute toPredicate
        capturedSpec.toPredicate(root, query, cb);

        // Verify all predicates were accessed
        verify(root).get("addressName");
        verify(root).get("addressDetail");
        verify(root, atMost(3)).get("ward");
        verify(root).get("eventName");
        verify(root).get("approvalStatus");

        verify(cb).like(addressNamePath, "%main street%");
        verify(cb).like(addressDetailPath, "%main street%");
        verify(cb).like(eventNamePath, "%test event%");
        verify(cb).equal(approvalStatusPath, ApprovalStatus.pending);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void shouldNotApplyAddressPredicate_WhenAddressIsNullOrEmpty() {
        // Mock user extraction
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Empty string
        eventService.getPendingEvents(request, "", null, null, null, 1, 10);
        // Null
        eventService.getPendingEvents(request, null, null, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository, times(2)).findAll(captor.capture(), any(Pageable.class));

        for (Specification<Event> spec : captor.getAllValues()) {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            Root<Event> root = mock(Root.class);
            Path<Object> addressNamePath = mock(Path.class);
            Path<Object> addressDetailPath = mock(Path.class);
            Path<Object> wardPath = mock(Path.class);
            lenient().when(root.get("addressName")).thenReturn(addressNamePath);
            lenient().when(root.get("addressDetail")).thenReturn(addressDetailPath);
            lenient().when(root.get("ward")).thenReturn(wardPath);

            spec.toPredicate(root, query, cb);

            // Should never call like/or since address is empty/null
            verify(cb, never()).like(any(), anyString());
            verify(cb, never()).or(any(), any(), any(), any(), any());
        }
    }

    @Test
    void shouldNotApplyNamePredicate_WhenNameIsNullOrEmpty() {
        // Mock user extraction
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // Empty string
        eventService.getPendingEvents(request, null, null, null, "", 1, 10);
        // Null
        eventService.getPendingEvents(request, null, null, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository, times(2)).findAll(captor.capture(), any(Pageable.class));

        for (Specification<Event> spec : captor.getAllValues()) {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            Root<Event> root = mock(Root.class);
            Path<Object> namePath = mock(Path.class);
            lenient().when(root.get("eventName")).thenReturn(namePath);

            spec.toPredicate(root, query, cb);

            verify(cb, never()).like(any(), anyString());
        }
    }

    @Test
    void shouldApplyStartTimePredicate_WhenOnlyStartTimeIsProvided() {
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        LocalDateTime startTime = LocalDateTime.now().minusDays(1);

        eventService.getPendingEvents(request, null, startTime, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path endPath = mock(Path.class);
        lenient().when(root.get("endTime")).thenReturn(endPath);

        capturedSpec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(eq(endPath), eq(startTime));
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(Expression.class));
    }

    @Test
    void shouldApplyEndTimePredicate_WhenOnlyEndTimeIsProvided() {
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        eventService.getPendingEvents(request, null, null, endTime, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path startPath = mock(Path.class);
        lenient().when(root.get("startTime")).thenReturn(startPath);

        capturedSpec.toPredicate(root, query, cb);

        verify(cb).lessThanOrEqualTo(eq(startPath), eq(endTime));
        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(Expression.class));
    }

    // Get all events (admin)
    @Test
    void shouldReturnEventList_WhenAdminRequests() {
        // GIVEN
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        String status = "upcoming";
        String approvalStatus = "approved";
        String address = "Hanoi";
        String name = "Music";
        LocalDateTime startTime = LocalDateTime.of(2025, 8, 19, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 8, 21, 0, 0);

        // WHEN
        Map<String, Object> result = eventService.getListEvents(
                request, status, approvalStatus, address,
                startTime, endTime, name, 1, 10
        );

        // THEN
        assertNotNull(result);
        assertTrue(result.containsKey("listEvents"));
        assertTrue(result.containsKey("pageSize"));
        assertTrue(result.containsKey("pageNo"));
        assertTrue(result.containsKey("totalPages"));

        List<GetEventsResponseDto> listEvents = (List<GetEventsResponseDto>) result.get("listEvents");
        assertEquals(1, listEvents.size());
        assertEquals("Music Concert", listEvents.get(0).getEventName());
        assertEquals("upcoming", listEvents.get(0).getStatus());
        assertEquals("approved", listEvents.get(0).getApprovalStatus());
        assertEquals("John Doe", listEvents.get(0).getOrganizerName());

        verify(eventRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldThrowSecurityException_WhenUserIsNotAdmin() {
        // GIVEN
        User normalUser = new User();
        normalUser.setId(2);
        normalUser.setRole(UserRole.organizer);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId(anyString())).thenReturn(2);
        when(userRepository.findById(2)).thenReturn(Optional.of(normalUser));

        // WHEN & THEN
        assertThrows(SecurityException.class, () -> {
            eventService.getListEvents(request, null, null, null, null, null, null, 1, 10);
        });
    }

    @Test
    void shouldApplyTimeFilter_WhenStartAndEndTimeProvided() {
        // GIVEN
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        LocalDateTime startTime = LocalDateTime.of(2025, 8, 19, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 8, 21, 0, 0);

        // WHEN
        Map<String, Object> result = eventService.getListEvents(
                request, null, null, null, startTime, endTime, null, 1, 10
        );

        // THEN
        assertNotNull(result);
        assertEquals(1, ((List<?>) result.get("listEvents")).size());

        verify(eventRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getListEvents_ShouldNotDecrementPage_WhenPageIsZero() {
        // GIVEN
        mockUser = mock(User.class);
        when(mockUser.getRole()).thenReturn(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // WHEN
        Map<String, Object> result = eventService.getListEvents(
                request, null, null, null, null, null, null, 0, 10
        );

        // THEN
        assertNotNull(result);
        assertEquals(1, ((List<?>) result.get("listEvents")).size());
        assertEquals(1, result.get("pageNo")); // Page number should be 1, not 0

        verify(eventRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldApplyAllFilters_WhenAllParametersProvided() {
        // Mock admin user
        mockUser = new User();
        mockUser.setRole(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        // Mock data
        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        String address = "Main Street";
        String name = "Test Event";
        String status = "ACTIVE";
        String approvalStatus = "APPROVED";
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        eventService.getListEvents(request, status, approvalStatus, address, startTime, endTime, name, 2, 10);

        ArgumentCaptor<Specification<Event>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(specCaptor.capture(), any(Pageable.class));

        Specification<Event> spec = specCaptor.getValue();

        // Mock Criteria objects
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path<Object> addressPath = mock(Path.class);
        Path<Object> namePath = mock(Path.class);
        Path<Object> statusPath = mock(Path.class);
        Path<Object> approvalPath = mock(Path.class);
        Path startPath = mock(Path.class);
        Path endPath = mock(Path.class);
        Path<Object> wardPath = mock(Path.class);
        Path<Object> districtPath = mock(Path.class);
        Path<Object> provincePath = mock(Path.class);

        when(root.get("addressName")).thenReturn(addressPath);
        when(root.get("addressDetail")).thenReturn(addressPath);
        when(root.get("ward")).thenReturn(wardPath);
        when(wardPath.get("name")).thenReturn(addressPath);
        when(wardPath.get("district")).thenReturn(districtPath);
        when(districtPath.get("name")).thenReturn(addressPath);
        when(districtPath.get("province")).thenReturn(provincePath);
        when(provincePath.get("name")).thenReturn(addressPath);

        when(root.get("eventName")).thenReturn(namePath);
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("approvalStatus")).thenReturn(approvalPath);
        when(root.get("startTime")).thenReturn(startPath);
        when(root.get("endTime")).thenReturn(endPath);

        lenient().when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
        lenient().when(cb.equal(any(), any())).thenReturn(mock(Predicate.class));
        lenient().when(cb.lessThanOrEqualTo(any(Expression.class), any(Expression.class))).thenReturn(mock(Predicate.class));
        lenient().when(cb.greaterThanOrEqualTo(any(Expression.class), any(Expression.class))).thenReturn(mock(Predicate.class));
        lenient().when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        spec.toPredicate(root, query, cb);

        // Verify predicates
        verify(cb, atMost(5)).like(any(), eq("%main street%"));
        verify(cb).like(any(), eq("%test event%"));
        verify(cb).equal(statusPath, status);
        verify(cb).equal(approvalPath, approvalStatus);
        verify(cb).lessThanOrEqualTo(startPath, endTime);
        verify(cb).greaterThanOrEqualTo(endPath, startTime);
    }

    @Test
    void shouldApplyOnlyStartTime_WhenOnlyStartTimeProvided() {
        mockUser = new User();
        mockUser.setRole(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        LocalDateTime startTime = LocalDateTime.now();

        eventService.getListEvents(request, null, null, null, startTime, null, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> spec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path endPath = mock(Path.class);
        when(root.get("endTime")).thenReturn(endPath);

        spec.toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(endPath, startTime);
        verify(cb, never()).lessThanOrEqualTo(any(Expression.class), any(Expression.class));
    }

    @Test
    void shouldApplyOnlyEndTime_WhenOnlyEndTimeProvided() {
        mockUser = new User();
        mockUser.setRole(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        LocalDateTime endTime = LocalDateTime.now().plusDays(1);

        eventService.getListEvents(request, null, null, null, null, endTime, null, 1, 10);

        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(captor.capture(), any(Pageable.class));

        Specification<Event> spec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Root<Event> root = mock(Root.class);

        Path startPath = mock(Path.class);
        when(root.get("startTime")).thenReturn(startPath);

        spec.toPredicate(root, query, cb);

        verify(cb).lessThanOrEqualTo(startPath, endTime);
        verify(cb, never()).greaterThanOrEqualTo(any(Expression.class), any(Expression.class));
    }

    @Test
    void getListEvents_ShouldNotApplyAddressPredicate_WhenAddressIsNullOrEmpty() {
        // GIVEN
        mockUser = new User();
        mockUser.setRole(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // WHEN
        eventService.getListEvents(request, null, null, "", null, null, null, 1, 10);
        eventService.getListEvents(request, null, null, null, null, null, null, 1, 10);

        // THEN
        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository, times(2)).findAll(captor.capture(), any(Pageable.class));

        for (Specification<Event> spec : captor.getAllValues()) {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            Root<Event> root = mock(Root.class);
            Path<Object> addressNamePath = mock(Path.class);
            Path<Object> addressDetailPath = mock(Path.class);
            Path<Object> wardPath = mock(Path.class);
            lenient().when(root.get("addressName")).thenReturn(addressNamePath);
            lenient().when(root.get("addressDetail")).thenReturn(addressDetailPath);
            lenient().when(root.get("ward")).thenReturn(wardPath);

            spec.toPredicate(root, query, cb);

            verify(cb, never()).like(any(), anyString());
            verify(cb, never()).or(any(), any(), any(), any(), any());
        }
    }

    @Test
    void getListEvents_ShouldNotApplyNamePredicate_WhenNameIsNullOrEmpty() {
        // GIVEN
        mockUser = new User();
        mockUser.setRole(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // WHEN
        eventService.getListEvents(request, null, null, null, null, null, "", 1, 10);
        eventService.getListEvents(request, null, null, null, null, null, null, 1, 10);

        // THEN
        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository, times(2)).findAll(captor.capture(), any(Pageable.class));

        for (Specification<Event> spec : captor.getAllValues()) {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            Root<Event> root = mock(Root.class);
            Path<Object> namePath = mock(Path.class);
            lenient().when(root.get("eventName")).thenReturn(namePath);

            spec.toPredicate(root, query, cb);

            verify(cb, never()).like(any(), anyString());
        }
    }

    @Test
    void getListEvents_ShouldNotApplyStatusPredicate_WhenStatusIsNullOrEmpty() {
        // GIVEN
        mockUser = new User();
        mockUser.setRole(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // WHEN
        eventService.getListEvents(request, "", null, null, null, null, null, 1, 10);
        eventService.getListEvents(request, null, null, null, null, null, null, 1, 10);

        // THEN
        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository, times(2)).findAll(captor.capture(), any(Pageable.class));

        for (Specification<Event> spec : captor.getAllValues()) {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            Root<Event> root = mock(Root.class);
            Path<Object> statusPath = mock(Path.class);
            lenient().when(root.get("status")).thenReturn(statusPath);

            spec.toPredicate(root, query, cb);

            verify(cb, never()).equal(any(), any());
        }
    }

    @Test
    void getListEvents_ShouldNotApplyApprovalStatusPredicate_WhenApprovalStatusIsNullOrEmpty() {
        // GIVEN
        mockUser = new User();
        mockUser.setRole(UserRole.admin);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(mockUser));

        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(mockPage);

        // WHEN
        eventService.getListEvents(request, null, "", null, null, null, null, 1, 10);
        eventService.getListEvents(request, null, null, null, null, null, null, 1, 10);

        // THEN
        ArgumentCaptor<Specification<Event>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository, times(2)).findAll(captor.capture(), any(Pageable.class));

        for (Specification<Event> spec : captor.getAllValues()) {
            CriteriaBuilder cb = mock(CriteriaBuilder.class);
            CriteriaQuery<?> query = mock(CriteriaQuery.class);
            Root<Event> root = mock(Root.class);
            Path<Object> approvalPath = mock(Path.class);
            lenient().when(root.get("approvalStatus")).thenReturn(approvalPath);

            spec.toPredicate(root, query, cb);

            verify(cb, never()).equal(any(), any());
        }
    }

    // Update event
    @Test
    void updateEvent_Success_WhenOrganizerUpdatesEvent() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("New Event");
        dto.setDescription("Desc");
        dto.setAddressName("Addr");
        dto.setAddressDetail("Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");

        // Ticket DTO
        UpdateEventRequestDto.TicketTypeDto ticketDto = new UpdateEventRequestDto.TicketTypeDto();
        ticketDto.setTicketType("VIP");
        ticketDto.setQuantityTotal(100);
        ticketDto.setPrice(500000.0);
        dto.setTicketTypes(List.of(ticketDto));

        // Discount DTO
        UpdateEventRequestDto.DiscountDto discountDto = new UpdateEventRequestDto.DiscountDto();
        discountDto.setCode("DISCOUNT10");
        discountDto.setDescription("10% off");
        discountDto.setType("percentage");
        discountDto.setValue(10.0);
        discountDto.setValidFrom(LocalDate.now());
        discountDto.setValidTo(LocalDate.now().plusDays(5));
        discountDto.setMaxUses(50);
        dto.setDiscounts(List.of(discountDto));

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(logoFile.getContentType()).thenReturn("image/png");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload
        EventServiceImpl spyService = Mockito.spy(eventService);
        doReturn("logo-url").when(spyService).uploadImageToImgbb(logoFile);
        doReturn("background-url").when(spyService).uploadImageToImgbb(backgroundFile);

        // Act
        spyService.updateEvent(10, dto, logoFile, backgroundFile, request);

        // Assert
        verify(eventRepository).save(any(Event.class));
        verify(ticketRepository).save(any(Ticket.class));
        verify(discountRepository).save(any(Discount.class));
    }

    @Test
    void updateEvent_ShouldThrow_WhenNotOrganizer() {
        User user = new User();
        user.setId(1);
        user.setRole(UserRole.customer);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UpdateEventRequestDto dto = new UpdateEventRequestDto();

        assertThrows(SecurityException.class, () ->
                eventService.updateEvent(10, dto, null, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenStartTimeAfterEndTime() {
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setStartTime(LocalDateTime.now().plusDays(5));
        dto.setEndTime(LocalDateTime.now().plusDays(1));

        assertThrows(IllegalArgumentException.class, () ->
                eventService.updateEvent(10, dto, null, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenOrganizerNotFound() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        UpdateEventRequestDto dto = new UpdateEventRequestDto();

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> eventService.updateEvent(100, dto, null, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenUserIsNotOrganizer() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setRole(UserRole.customer); // không phải organizer
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        UpdateEventRequestDto dto = new UpdateEventRequestDto();

        assertThrows(SecurityException.class,
                () -> eventService.updateEvent(100, dto, null, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenEventNotFound() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setRole(UserRole.organizer);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        when(eventRepository.findById(100)).thenReturn(Optional.empty());

        UpdateEventRequestDto dto = new UpdateEventRequestDto();

        assertThrows(EntityNotFoundException.class,
                () -> eventService.updateEvent(100, dto, null, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenInvalidLogoType() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setRole(UserRole.organizer);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event event = new Event();
        User organizer = new User();
        organizer.setId(1);
        event.setOrganizer(organizer);
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusDays(1));

        when(logoFile.getContentType()).thenReturn("application/pdf");

        assertThrows(IllegalArgumentException.class,
                () -> eventService.updateEvent(100, dto, logoFile, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenOrganizerNotOwnerOfEvent() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setRole(UserRole.organizer);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event event = new Event();
        User organizer = new User();
        organizer.setId(2); // khác organizerId
        event.setOrganizer(organizer);
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusDays(1));

        assertThrows(SecurityException.class,
                () -> eventService.updateEvent(100, dto, null, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenLogoFileHasInvalidType() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setRole(UserRole.organizer);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event event = new Event();
        User organizer = new User();
        organizer.setId(1);
        event.setOrganizer(organizer);
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusDays(1));

        when(logoFile.getContentType()).thenReturn("application/pdf"); // Sai định dạng

        assertThrows(IllegalArgumentException.class,
                () -> eventService.updateEvent(100, dto, logoFile, null, request));
    }

    @Test
    void updateEvent_ShouldThrow_WhenBackgroundFileHasInvalidType() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setRole(UserRole.organizer);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event event = new Event();
        User organizer = new User();
        organizer.setId(1);
        event.setOrganizer(organizer);
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusDays(1));

        when(backgroundFile.getContentType()).thenReturn("application/pdf"); // Sai định dạng

        assertThrows(IllegalArgumentException.class,
                () -> eventService.updateEvent(100, dto, null, backgroundFile, request));
    }

    @Test
    void updateEvent_ShouldPass_WhenTicketDtosIsNull() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("New Event");
        dto.setDescription("Description");
        dto.setAddressName("Address");
        dto.setAddressDetail("Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");
        dto.setTicketTypes(null); // Ticket DTOs are null

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(logoFile.getContentType()).thenReturn("image/png");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload
        EventServiceImpl spyService = Mockito.spy(eventService);
        doReturn("logo-url").when(spyService).uploadImageToImgbb(logoFile);
        doReturn("background-url").when(spyService).uploadImageToImgbb(backgroundFile);

        // Act
        spyService.updateEvent(10, dto, logoFile, backgroundFile, request);

        // Assert
        verify(eventRepository).save(any(Event.class));
        verify(ticketRepository, never()).save(any(Ticket.class)); // No tickets should be saved
    }

    @Test
    void updateEvent_ShouldDeleteOldTicket_WhenNotInIncomingTicketTypes() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        Ticket existingTicket = new Ticket();
        existingTicket.setTicketType("VIP");
        existingTicket.setEvent(event);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setAddressName("Updated Address");
        dto.setAddressDetail("Updated Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");

        // New ticket types (does not include "VIP")
        UpdateEventRequestDto.TicketTypeDto newTicketDto = new UpdateEventRequestDto.TicketTypeDto();
        newTicketDto.setTicketType("Standard");
        newTicketDto.setQuantityTotal(200);
        newTicketDto.setPrice(300000.0);
        dto.setTicketTypes(List.of(newTicketDto));

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(ticketRepository.findByEvent(event)).thenReturn(List.of(existingTicket));
        when(logoFile.getContentType()).thenReturn("image/png");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload
        EventServiceImpl spyService = Mockito.spy(eventService);
        doReturn("logo-url").when(spyService).uploadImageToImgbb(logoFile);
        doReturn("background-url").when(spyService).uploadImageToImgbb(backgroundFile);

        // Act
        spyService.updateEvent(10, dto, logoFile, backgroundFile, request);

        // Assert
        verify(ticketRepository).delete(existingTicket); // Ensure old ticket is deleted
        verify(ticketRepository).save(any(Ticket.class)); // Ensure new ticket is saved
    }

    @Test
    void updateEvent_ShouldPass_WhenBackgroundFileIsNull() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("New Event");
        dto.setDescription("Description");
        dto.setAddressName("Address");
        dto.setAddressDetail("Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(logoFile.getContentType()).thenReturn("image/png");

        // Mock upload
        EventServiceImpl spyService = Mockito.spy(eventService);
        doReturn("logo-url").when(spyService).uploadImageToImgbb(logoFile);

        // Act
        spyService.updateEvent(10, dto, logoFile, null, request);

        // Assert
        verify(eventRepository).save(any(Event.class));
        verify(ticketRepository, never()).save(any(Ticket.class)); // No tickets should be saved
        verify(discountRepository, never()).save(any(Discount.class)); // No discounts should be saved
    }

    @Test
    void updateEvent_ShouldPass_WhenLogoFileIsNull() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("New Event");
        dto.setDescription("Description");
        dto.setAddressName("Address");
        dto.setAddressDetail("Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload
        EventServiceImpl spyService = Mockito.spy(eventService);
        doReturn("background-url").when(spyService).uploadImageToImgbb(backgroundFile);

        // Act
        spyService.updateEvent(10, dto, null, backgroundFile, request);

        // Assert
        verify(eventRepository).save(any(Event.class));
        verify(ticketRepository, never()).save(any(Ticket.class)); // No tickets should be saved
        verify(discountRepository, never()).save(any(Discount.class)); // No discounts should be saved
    }

    @Test
    void updateEvent_ShouldDeleteOldDiscount_WhenNotInIncomingDiscountCodes() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        Discount existingDiscount = new Discount();
        existingDiscount.setCode("OLD_DISCOUNT");
        existingDiscount.setEvent(event);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setAddressName("Updated Address");
        dto.setAddressDetail("Updated Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");

        // New discount codes (does not include "OLD_DISCOUNT")
        UpdateEventRequestDto.DiscountDto newDiscountDto = new UpdateEventRequestDto.DiscountDto();
        newDiscountDto.setCode("NEW_DISCOUNT");
        newDiscountDto.setDescription("New Discount Description");
        newDiscountDto.setType("percentage");
        newDiscountDto.setValue(15.0);
        newDiscountDto.setValidFrom(LocalDate.now());
        newDiscountDto.setValidTo(LocalDate.now().plusDays(5));
        newDiscountDto.setMaxUses(100);
        dto.setDiscounts(List.of(newDiscountDto));

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(discountRepository.findByEvent(event)).thenReturn(List.of(existingDiscount));
        when(logoFile.getContentType()).thenReturn("image/png");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload
        EventServiceImpl spyService = Mockito.spy(eventService);
        doReturn("logo-url").when(spyService).uploadImageToImgbb(logoFile);
        doReturn("background-url").when(spyService).uploadImageToImgbb(backgroundFile);

        // Act
        spyService.updateEvent(10, dto, logoFile, backgroundFile, request);

        // Assert
        verify(discountRepository).delete(existingDiscount); // Ensure old discount is deleted
        verify(discountRepository).save(any(Discount.class)); // Ensure new discount is saved
    }

    @Test
    void updateEvent_ShouldNotDeleteOldDiscount_WhenInIncomingDiscountCodes() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        Discount existingDiscount = new Discount();
        existingDiscount.setCode("EXISTING_DISCOUNT");
        existingDiscount.setEvent(event);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("Updated Event");
        dto.setDescription("Updated Description");
        dto.setAddressName("Updated Address");
        dto.setAddressDetail("Updated Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");

        // New discount codes (includes "EXISTING_DISCOUNT")
        UpdateEventRequestDto.DiscountDto newDiscountDto = new UpdateEventRequestDto.DiscountDto();
        newDiscountDto.setCode("EXISTING_DISCOUNT");
        newDiscountDto.setDescription("Existing Discount Description");
        newDiscountDto.setType("percentage");
        newDiscountDto.setValue(20.0);
        newDiscountDto.setValidFrom(LocalDate.now());
        newDiscountDto.setValidTo(LocalDate.now().plusDays(10));
        newDiscountDto.setMaxUses(200);
        dto.setDiscounts(List.of(newDiscountDto));

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(discountRepository.findByEvent(event)).thenReturn(List.of(existingDiscount));
        when(logoFile.getContentType()).thenReturn("image/png");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload
        EventServiceImpl spyService = Mockito.spy(eventService);
        doReturn("logo-url").when(spyService).uploadImageToImgbb(logoFile);
        doReturn("background-url").when(spyService).uploadImageToImgbb(backgroundFile);

        // Act
        spyService.updateEvent(10, dto, logoFile, backgroundFile, request);

        // Assert
        verify(discountRepository, never()).delete(existingDiscount); // Ensure old discount is not deleted
        verify(discountRepository).save(any(Discount.class)); // Ensure new discount is saved
    }

    @Test
    void updateEvent_ShouldThrowRuntimeException_WhenIOExceptionOccursDuringFileUpload() throws Exception {
        // Arrange
        User organizer = new User();
        organizer.setId(1);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(10);
        event.setOrganizer(organizer);

        Ward ward = new Ward();
        ward.setId(5);

        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("New Event");
        dto.setDescription("Description");
        dto.setAddressName("Address");
        dto.setAddressDetail("Detail");
        dto.setWardId(5);
        dto.setStartTime(LocalDateTime.now().plusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(2));
        dto.setCategory("Music");

        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        lenient().when(wardRepository.findById(5)).thenReturn(Optional.of(ward));
        when(logoFile.getContentType()).thenReturn("image/png");
        when(backgroundFile.getContentType()).thenReturn("image/png");

        // Mock upload to throw IOException
        EventServiceImpl spyService = Mockito.spy(eventService);
        doThrow(new IOException("File upload failed")).when(spyService).uploadImageToImgbb(any(MultipartFile.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                spyService.updateEvent(10, dto, logoFile, backgroundFile, request));

        assertEquals("File upload failed", exception.getCause().getMessage());
    }

    // Generate excel report
    @Test
    void getTicketStatsByEvent_ShouldReturnCorrectStats() {
        Event event = new Event();
        event.setEventId(10);

        Ticket ticket1 = new Ticket();
        ticket1.setTicketType("VIP");
        ticket1.setQuantityTotal(100);
        ticket1.setPrice(200.0);

        Ticket ticket2 = new Ticket();
        ticket2.setTicketType("Standard");
        ticket2.setQuantityTotal(50);
        ticket2.setPrice(100.0);

        List<Ticket> tickets = Arrays.asList(ticket1, ticket2);

        when(eventRepository.findById(10)).thenReturn(Optional.of(event));
        when(ticketRepository.findByEvent(event)).thenReturn(tickets);
        when(orderTicketRepository.sumQuantityByTicket(ticket1)).thenReturn(30); // sold
        when(orderTicketRepository.sumQuantityByTicket(ticket2)).thenReturn(10);

        List<TicketExportDto> result = eventService.getTicketStatsByEvent(10);

        assertEquals(2, result.size());
        TicketExportDto vipStats = result.getFirst();
        assertEquals("VIP", vipStats.getTicketType());
        assertEquals(100, vipStats.getTotalQuantity());
        assertEquals(30, vipStats.getSoldQuantity());
        assertEquals(70, vipStats.getRemainingQuantity());
        assertEquals(30 * 200.0, vipStats.getRevenue());

        TicketExportDto standardStats = result.get(1);
        assertEquals("Standard", standardStats.getTicketType());
        assertEquals(50, standardStats.getTotalQuantity());
        assertEquals(10, standardStats.getSoldQuantity());
        assertEquals(40, standardStats.getRemainingQuantity());
        assertEquals(10 * 100.0, standardStats.getRevenue());
    }

    @Test
    void generateExcelReport_ShouldReturnByteArray_WhenUserIsOrganizer() throws IOException {
        // Arrange
        Integer eventId = 1;
        Integer userId = 100;

        User organizer = new User();
        organizer.setId(userId);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(organizer);

        Ticket ticket = new Ticket();
        ticket.setTicketType("VIP");
        ticket.setQuantityTotal(100);
        ticket.setPrice(50.0);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(ticketRepository.findByEvent(event)).thenReturn(List.of(ticket));
        when(orderTicketRepository.sumQuantityByTicket(ticket)).thenReturn(10);

        // Act
        byte[] result = eventService.generateExcelReport(request, eventId);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateExcelReport_ShouldThrowSecurityException_WhenUserNotOrganizerOrAdmin() {
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setRole(UserRole.customer);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThrows(SecurityException.class, () -> eventService.generateExcelReport(request, 100));
    }

    @Test
    void generateExcelReport_ShouldThrowSecurityException_WhenUserIsCustomer() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        User user = new User();
        user.setId(1);
        user.setRole(UserRole.customer); // Customer role
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class,
                () -> eventService.generateExcelReport(request, 100));
        assertEquals("You do not have permission to generate reports.", exception.getMessage());
    }

    @Test
    void generateExcelReport_ShouldThrowSecurityException_WhenOrganizerNotOwner() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(2);

        User user = new User();
        user.setId(2);
        user.setRole(UserRole.organizer); // Organizer but not event owner
        when(userRepository.findById(2)).thenReturn(Optional.of(user));

        Event event = new Event();
        User eventOwner = new User();
        eventOwner.setId(99); // Different from userId (2)
        event.setOrganizer(eventOwner);
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class,
                () -> eventService.generateExcelReport(request, 100));
        assertEquals("You do not have permission to access this event.", exception.getMessage());
    }

    @Test
    void generateExcelReport_ShouldReturnByteArray_WhenUserIsAdmin() throws IOException {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);

        // Mock user as Admin
        User adminUser = new User();
        adminUser.setId(1);
        adminUser.setRole(UserRole.admin);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));

        // Mock Event
        Event event = new Event();
        User organizer = new User();
        organizer.setId(2);
        event.setOrganizer(organizer);
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        // Mock getTicketStatsByEvent
        List<TicketExportDto> mockDtos = List.of(
                new TicketExportDto("VIP", 100, 80, 20, 8000.0)
        );
        EventServiceImpl spyService = spy(eventService);
        doReturn(mockDtos).when(spyService).getTicketStatsByEvent(100);

        // Mock exportToExcel
        ByteArrayInputStream mockStream = new ByteArrayInputStream("test".getBytes());
        doReturn(mockStream).when(spyService).exportToExcel(mockDtos);

        // Act
        byte[] result = spyService.generateExcelReport(request, 100);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateExcelReport_ShouldThrowRuntimeException_WhenExportToExcelFails() throws IOException {
        // Arrange
        Integer eventId = 1;
        Integer userId = 100;

        User organizer = new User();
        organizer.setId(userId);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(organizer);

        Ticket ticket = new Ticket();
        ticket.setTicketType("VIP");
        ticket.setQuantityTotal(100);
        ticket.setPrice(50.0);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(ticketRepository.findByEvent(event)).thenReturn(List.of(ticket));
        when(orderTicketRepository.sumQuantityByTicket(ticket)).thenReturn(10);

        // Spy EventServiceImpl để mock exportToExcel
        EventServiceImpl spyService = Mockito.spy(eventService);
        doThrow(new IOException("IO error")).when(spyService).exportToExcel(anyList());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> spyService.generateExcelReport(request, eventId));

        assertEquals("Failed to generate Excel report", exception.getMessage());
        assertTrue(exception.getCause() instanceof IOException);
    }

    // Generate pdf report
    @Test
    void generatePdfReport_ShouldReturnBytes_WhenUserIsAdmin() throws Exception {
        // Arrange
        Integer eventId = 1;
        Integer userId = 100;

        User adminUser = new User();
        adminUser.setId(userId);
        adminUser.setRole(UserRole.admin);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(adminUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        List<TicketExportDto> dtos = List.of(new TicketExportDto("VIP", 100, 50, 50, 5000.0));
        doReturn(dtos).when(spyEventService).getTicketStatsByEvent(100);

        ByteArrayInputStream mockStream = new ByteArrayInputStream("pdf".getBytes());
        doReturn(mockStream).when(spyEventService).exportToPdf(dtos);

        // Act
        byte[] result = spyEventService.generatePdfReport(request, 100);

        // Assert
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generatePdfReport_ShouldThrowSecurityException_WhenUserNotAdminOrOrganizer() {
        // Arrange
        User customer = new User();
        customer.setId(3);
        customer.setRole(UserRole.customer);
        when(userRepository.findById(1)).thenReturn(Optional.of(customer));

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> eventService.generatePdfReport(request, 100));
    }

    @Test
    void generatePdfReport_ShouldThrowSecurityException_WhenOrganizerNotEventOwner() {
        // Arrange
        Integer eventId = 1;
        Integer userId = 1;

        User organizer = new User();
        organizer.setId(userId);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(organizer);
        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        Event otherEvent = new Event();
        User otherOrganizer = new User();
        otherOrganizer.setId(99);
        otherEvent.setOrganizer(otherOrganizer);
        when(eventRepository.findById(100)).thenReturn(Optional.of(otherEvent));

        // Act & Assert
        assertThrows(SecurityException.class,
                () -> eventService.generatePdfReport(request, 100));
    }

    @Test
    void generatePdfReport_ShouldThrowRuntimeException_WhenEventNotFound() {
        // Arrange
        Integer eventId = 1;
        Integer userId = 100;

        User adminUser = new User();
        adminUser.setId(userId);
        adminUser.setRole(UserRole.admin);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(adminUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(eventRepository.findById(100)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> eventService.generatePdfReport(request, 100));
    }

    @Test
    void generatePdfReport_ShouldThrowRuntimeException_WhenExportToPdfFails() throws Exception {
        // Arrange
        Integer eventId = 100;
        Integer userId = 1;

        User adminUser = new User();
        adminUser.setId(userId);
        adminUser.setRole(UserRole.admin);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(adminUser);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        List<TicketExportDto> dtos = List.of(new TicketExportDto("VIP", 100, 50, 50, 5000.0));
        doReturn(dtos).when(spyEventService).getTicketStatsByEvent(100);

        doThrow(new IOException("PDF error")).when(spyEventService).exportToPdf(dtos);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> spyEventService.generatePdfReport(request, 100));
        assertTrue(ex.getMessage().contains("Error generating PDF report"));
    }

    @Test
    void exportToPdf_ShouldReturnPdfStream_WhenValidData() throws Exception {
        List<TicketExportDto> dtos = Arrays.asList(
                new TicketExportDto("VIP", 100, 50, 50, 500000.0),
                new TicketExportDto("Standard", 200, 150, 50, 750000.0)
        );

        ByteArrayInputStream result = eventService.exportToPdf(dtos);

        assertNotNull(result);
        byte[] pdfBytes = result.readAllBytes();
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void loadVietnameseFont_ShouldReturnFontObject() throws Exception {
        // Arrange
        float fontSize = 12.0f;
        boolean isBold = true;

        // Act
        Font font = eventService.loadVietnameseFont(fontSize, isBold);

        // Assert
        assertNotNull(font, "Font object should not be null");
        assertEquals(fontSize, font.getSize(), "Font size should match the input size");
        assertTrue(font.isBold(), "Font should be bold");
        assertEquals("Arial", font.getFamilyname(), "Font family should be Arial");
    }

    // Generate buyer excel report
    @Test
    void generateBuyerReportExcel_ShouldThrowSecurityException_WhenUserIsCustomer() {
        User customer = new User();
        customer.setId(3);
        customer.setRole(UserRole.customer);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(3);
        when(userRepository.findById(3)).thenReturn(Optional.of(customer));

        SecurityException exception = assertThrows(SecurityException.class, () ->
                eventService.generateBuyerReportExcel(request, 100)
        );

        assertEquals("You do not have permission to generate reports.", exception.getMessage());
    }

    @Test
    void generateBuyerReportExcel_ShouldReturnExcelBytes_WhenUserIsOrganizerAndOwner() {
        Integer eventId = 100;
        Integer userId = 1;

        User organizer = new User();
        organizer.setId(userId);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(organizer);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        byte[] result = eventService.generateBuyerReportExcel(request, 100);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateBuyerReportExcel_ShouldReturnExcelBytes_WhenValidData() {
        Integer eventId = 100;
        Integer userId = 1;

        User adminUser = new User();
        adminUser.setId(userId);
        adminUser.setRole(UserRole.admin);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(adminUser);

        Ticket ticketVIP, ticketStandard;
        ticketVIP = new Ticket();
        ticketVIP.setTicketType("VIP");
        ticketVIP.setPrice(500_000.0);
        ticketVIP.setEvent(event);

        ticketStandard = new Ticket();
        ticketStandard.setTicketType("Standard");
        ticketStandard.setPrice(200_000.0);
        ticketStandard.setEvent(event);

        Order order1 = new Order();
        order1.setUser(adminUser);

        OrderTicket orderTicket1, orderTicket2;
        orderTicket1 = new OrderTicket();
        orderTicket1.setOrder(order1);
        orderTicket1.setTicket(ticketVIP);
        orderTicket1.setQuantity(2);

        orderTicket2 = new OrderTicket();
        orderTicket2.setOrder(order1);
        orderTicket2.setTicket(ticketStandard);
        orderTicket2.setQuantity(3);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));
        when(ticketRepository.findByEvent(event)).thenReturn(Arrays.asList(ticketVIP, ticketStandard));
        when(orderTicketRepository.findAllByTicketEvent(event)).thenReturn(Arrays.asList(orderTicket1, orderTicket2));

        byte[] result = eventService.generateBuyerReportExcel(request, 100);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateBuyerReportExcel_ShouldThrowRuntimeException_WhenEventNotFound() {
        Integer eventId = 100;
        Integer userId = 1;

        User adminUser = new User();
        adminUser.setId(userId);
        adminUser.setRole(UserRole.admin);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(adminUser);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
        when(eventRepository.findById(100)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                eventService.generateBuyerReportExcel(request, 100)
        );

        assertEquals("Event not found", exception.getMessage());
    }

    // Generate buyer pdf report
    @Test
    void generateBuyerReportPdf_ShouldReturnPdfBytes_WhenDataIsValid() {
        EventRepository eventRepository = mock(EventRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        WardRepository wardRepository = mock(WardRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        OrderTicketRepository orderTicketRepository = mock(OrderTicketRepository.class);
        DiscountRepository discountRepository = mock(DiscountRepository.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        EventServiceImpl eventService = new EventServiceImpl(
                eventRepository, userRepository, wardRepository, ticketRepository, orderTicketRepository, discountRepository, jwtUtil
        );

        HttpServletRequest request = mock(HttpServletRequest.class);

        // Mock dữ liệu
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(jwtUtil.extractUserId("token123")).thenReturn(1);

        User user = new User();
        user.setId(1);
        user.setRole(UserRole.admin);
        user.setFullName("Nguyen Van A");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        Event event = new Event();
        event.setEventId(100);
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        Ticket ticket1 = new Ticket();
        ticket1.setTicketType("VIP");
        ticket1.setPrice(500.0);
        ticket1.setEvent(event);

        Ticket ticket2 = new Ticket();
        ticket2.setTicketType("Standard");
        ticket2.setPrice(200.0);
        ticket2.setEvent(event);

        when(ticketRepository.findByEvent(event)).thenReturn(List.of(ticket1, ticket2));

        User buyer = new User();
        buyer.setId(2);
        buyer.setFullName("Tran Thi B");

        Order order = new Order();
        order.setUser(buyer);

        OrderTicket ot1 = new OrderTicket();
        ot1.setOrder(order);
        ot1.setTicket(ticket1);
        ot1.setQuantity(2); // VIP x2 (1000)

        OrderTicket ot2 = new OrderTicket();
        ot2.setOrder(order);
        ot2.setTicket(ticket2);
        ot2.setQuantity(3); // Standard x3 (600)

        when(orderTicketRepository.findAllByTicketEvent(event)).thenReturn(List.of(ot1, ot2));

        byte[] result = eventService.generateBuyerReportPdf(request, 100);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateBuyerReportPdf_ShouldThrowSecurityException_WhenUserIsNotAdminOrOrganizer() {
        EventRepository eventRepository = mock(EventRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        WardRepository wardRepository = mock(WardRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        OrderTicketRepository orderTicketRepository = mock(OrderTicketRepository.class);
        DiscountRepository discountRepository = mock(DiscountRepository.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        EventServiceImpl eventService = new EventServiceImpl(
                eventRepository, userRepository, wardRepository, ticketRepository, orderTicketRepository, discountRepository, jwtUtil
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(jwtUtil.extractUserId("token123")).thenReturn(1);

        User user = new User();
        user.setId(1);
        user.setRole(UserRole.customer);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        assertThrows(SecurityException.class, () -> eventService.generateBuyerReportPdf(request, 100));
    }

    @Test
    void generateBuyerReportPdf_ShouldThrowEntityNotFoundException_WhenUserDoesNotExist() {
        EventRepository eventRepository = mock(EventRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        WardRepository wardRepository = mock(WardRepository.class);
        TicketRepository ticketRepository = mock(TicketRepository.class);
        OrderTicketRepository orderTicketRepository = mock(OrderTicketRepository.class);
        DiscountRepository discountRepository = mock(DiscountRepository.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        EventServiceImpl eventService = new EventServiceImpl(
                eventRepository, userRepository, wardRepository, ticketRepository, orderTicketRepository, discountRepository, jwtUtil
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(jwtUtil.extractUserId("token123")).thenReturn(1);

        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> eventService.generateBuyerReportPdf(request, 100));
    }

    @Test
    void generateBuyerReportPdf_ShouldReturnPdfBytes_WhenUserIsOrganizerAndOwner() {
        Integer eventId = 100;
        Integer userId = 1;

        User organizer = new User();
        organizer.setId(userId);
        organizer.setRole(UserRole.organizer);

        Event event = new Event();
        event.setEventId(eventId);
        event.setOrganizer(organizer);

        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.extractUserId("token")).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(organizer));
        when(eventRepository.findById(100)).thenReturn(Optional.of(event));

        byte[] result = eventService.generateBuyerReportPdf(request, 100);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}

