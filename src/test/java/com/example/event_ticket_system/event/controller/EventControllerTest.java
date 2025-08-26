package com.example.event_ticket_system.event.controller;

import com.example.event_ticket_system.Controller.EventController;
import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.request.UpdateEventRequestDto;
import com.example.event_ticket_system.DTO.response.DetailEventResponseDto;
import com.example.event_ticket_system.Service.EventService;
import com.example.event_ticket_system.Service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private EventController eventController;

    @Mock
    private MultipartFile logoFile;

    @Mock
    private MultipartFile backgroundFile;

    @Mock
    private HttpServletRequest request;

    @Mock
    private BindingResult bindingResult;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private EventRequestDto createValidEventRequest() {
        EventRequestDto requestDto = new EventRequestDto();
        requestDto.setEventName("Music Festival");
        requestDto.setDescription("A great music event");
        requestDto.setWardId(123);
        requestDto.setAddressDetail("123 Street");
        requestDto.setAddressName("Event Hall");
        requestDto.setCategory("Music");
        requestDto.setStartTime(LocalDateTime.now().plusDays(1));
        requestDto.setEndTime(LocalDateTime.now().plusDays(2));
        requestDto.setSaleStart(LocalDateTime.now());
        requestDto.setSaleEnd(LocalDateTime.now().plusDays(1));

        EventRequestDto.TicketRequest ticket = new EventRequestDto.TicketRequest();
        ticket.setTicketType("VIP");
        ticket.setQuantityTotal(100);
        ticket.setPrice(500.0);
        requestDto.setTickets(List.of(ticket));

        EventRequestDto.DiscountRequest discount = new EventRequestDto.DiscountRequest();
        discount.setDiscountCode("DISC10");
        discount.setDiscountDescription("10% off");
        discount.setDiscountType("percentage");
        discount.setDiscountValue(10.0);
        discount.setDiscountValidFrom(LocalDate.now());
        discount.setDiscountValidTo(LocalDate.now().plusDays(5));
        discount.setDiscountMaxUses(50);
        requestDto.setDiscounts(List.of(discount));

        return requestDto;
    }

    @Test
    void createEvent_ShouldReturn201_WhenSuccess() throws Exception {
        EventRequestDto requestDto = createValidEventRequest();
        String jsonData = objectMapper.writeValueAsString(requestDto);

        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile("logo", "logo.png", "image/png", "fake-logo".getBytes());
        MockMultipartFile backgroundPart = new MockMultipartFile("background", "bg.png", "image/png", "fake-bg".getBytes());

        when(eventService.createEvent(any(EventRequestDto.class), any(), any(), any(HttpServletRequest.class)))
                .thenReturn(1);

        mockMvc.perform(multipart("/api/events")
                        .file(dataPart)
                        .file(logoPart)
                        .file(backgroundPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.message").value("Event created successfully"))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void createEvent_ShouldReturnBadRequest_WhenValidationFails() {
        // Giả lập lỗi validation
        when(bindingResult.hasErrors()).thenReturn(true);

        // Giả lập field errors
        FieldError error1 = new FieldError("eventRequestDto", "eventName", "Event name cannot be null");
        FieldError error2 = new FieldError("eventRequestDto", "startTime", "Start time cannot be null");
        List<FieldError> fieldErrors = Arrays.asList(error1, error2);

        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        EventRequestDto invalidRequest = new EventRequestDto();

        ResponseEntity<Object> response = eventController.createEvent(invalidRequest, null, null, bindingResult, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertNotNull(responseBody);
        assertEquals("Validation failed", responseBody.get("message"));
        assertEquals(400, responseBody.get("status"));

        Map<String, String> errors = (Map<String, String>) responseBody.get("data");
        assertEquals("Event name cannot be null", errors.get("eventName"));
        assertEquals("Start time cannot be null", errors.get("startTime"));
    }

    @Test
    void createEvent_ShouldReturn403_WhenSecurityException() throws Exception {
        EventRequestDto requestDto = createValidEventRequest();
        String jsonData = objectMapper.writeValueAsString(requestDto);

        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile("logo", "logo.png", "image/png", "fake-logo".getBytes());
        MockMultipartFile backgroundPart = new MockMultipartFile("background", "bg.png", "image/png", "fake-bg".getBytes());

        doThrow(new SecurityException("You do not have permission"))
                .when(eventService).createEvent(any(EventRequestDto.class), any(), any(), any(HttpServletRequest.class));

        mockMvc.perform(multipart("/api/events")
                        .file(dataPart)
                        .file(logoPart)
                        .file(backgroundPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("403"))
                .andExpect(jsonPath("$.message").value("You do not have permission"));
    }

    @Test
    void createEvent_ShouldReturn404_WhenEntityNotFound() throws Exception {
        EventRequestDto requestDto = createValidEventRequest();
        String jsonData = objectMapper.writeValueAsString(requestDto);

        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile("logo", "logo.png", "image/png", "fake-logo".getBytes());
        MockMultipartFile backgroundPart = new MockMultipartFile("background", "bg.png", "image/png", "fake-bg".getBytes());

        doThrow(new EntityNotFoundException("Resource not found"))
                .when(eventService).createEvent(any(EventRequestDto.class), any(), any(), any(HttpServletRequest.class));

        mockMvc.perform(multipart("/api/events")
                        .file(dataPart)
                        .file(logoPart)
                        .file(backgroundPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("404"))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }

    @Test
    void createEvent_ShouldReturn500_WhenUnexpectedError() throws Exception {
        EventRequestDto requestDto = createValidEventRequest();
        String jsonData = objectMapper.writeValueAsString(requestDto);

        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile("logo", "logo.png", "image/png", "fake-logo".getBytes());
        MockMultipartFile backgroundPart = new MockMultipartFile("background", "bg.png", "image/png", "fake-bg".getBytes());

        doThrow(new RuntimeException("Unexpected error"))
                .when(eventService).createEvent(any(EventRequestDto.class), any(), any(), any(HttpServletRequest.class));

        mockMvc.perform(multipart("/api/events")
                        .file(dataPart)
                        .file(logoPart)
                        .file(backgroundPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("500"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred while creating the event"));
    }

    @Test
    void getEventsByOrganizer_ShouldReturnOk_WhenDataExists() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("events", Arrays.asList("Event 1", "Event 2"));

        when(eventService.getEventsByOrganizer(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(mockResponse);

        ResponseEntity<Object> response = eventController.getEventsByOrganizer(
                request, "test", "APPROVED", null, null, null,1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Events retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertTrue(((Map<String, Object>) body.get("data")).containsKey("events"));
    }

    @Test
    void getEventsByOrganizer_ShouldResetPageSize_WhenPageAndSizeAreInvalid() {
        when(eventService.getEventsByOrganizer(any(), any(), any(), any(), any(), any(), eq(0), eq(0)))
                .thenReturn(new HashMap<>());

        ResponseEntity<Object> response = eventController.getEventsByOrganizer(
                request, null, null, null, null, null,0, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(eventService).getEventsByOrganizer(
                any(), any(), any(), any(), any(), any(), eq(1), eq(1));
    }

    @Test
    void getEventsByOrganizer_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        when(eventService.getEventsByOrganizer(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SecurityException("Access denied"));

        ResponseEntity<Object> response = eventController.getEventsByOrganizer(
                request, null, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
    }

    @Test
    void getEventsByOrganizer_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        when(eventService.getEventsByOrganizer(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("Organizer not found"));

        ResponseEntity<Object> response = eventController.getEventsByOrganizer(
                request, null, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Organizer not found", body.get("message"));
    }

    @Test
    void getEventsByOrganizer_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        when(eventService.getEventsByOrganizer(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<Object> response = eventController.getEventsByOrganizer(
                request, null, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while retrieving events", body.get("message"));
    }

    @Test
    void getEventById_ShouldReturnOk_WhenEventExists() {
        DetailEventResponseDto mockEvent = new DetailEventResponseDto();
        mockEvent.setEventId(1);
        mockEvent.setEventName("Music Festival");

        when(eventService.getEventById(eq(1), any(HttpServletRequest.class)))
                .thenReturn(mockEvent);

        ResponseEntity<Object> response = eventController.getEventById(1, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Event retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertNotNull(body.get("data"));
        DetailEventResponseDto eventData = (DetailEventResponseDto) body.get("data");
        assertEquals(1, eventData.getEventId());
        assertEquals("Music Festival", eventData.getEventName());
    }

    @Test
    void getEventById_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        when(eventService.getEventById(eq(99), any(HttpServletRequest.class)))
                .thenThrow(new EntityNotFoundException("Event not found"));

        ResponseEntity<Object> response = eventController.getEventById(99, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Event not found", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getEventById_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        when(eventService.getEventById(eq(1), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<Object> response = eventController.getEventById(1, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while retrieving the event", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getRecommendEvents_ShouldReturnOk_WhenEventsRetrievedSuccessfully() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("events", List.of("Event 1", "Event 2"));

        when(eventService.getRecommendEvents(eq("music"), eq("Hanoi"), any(), any(), eq("Concert"),
                eq(1), eq(10), eq("date")))
                .thenReturn(mockResponse);

        ResponseEntity<Object> response = eventController.getRecommendEvents(
                "music", "Hanoi", null, null, "Concert", 1, 10, "date");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Recommended events retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertEquals(mockResponse, body.get("data"));
    }

    @Test
    void getRecommendEvents_ShouldResetPageAndSize_WhenPageAndSizeAreInvalid() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("events", List.of("Event A", "Event B"));

        when(eventService.getRecommendEvents(eq("art"), eq("HCM"), any(), any(), eq("Gallery"),
                eq(1), eq(1), eq("popularity")))
                .thenReturn(mockResponse);

        ResponseEntity<Object> response = eventController.getRecommendEvents(
                "art", "HCM", null, null, "Gallery", 0, 0, "popularity");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Recommended events retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertEquals(mockResponse, body.get("data"));
    }

    @Test
    void getRecommendEvents_ShouldReturnInternalServerError_WhenExceptionThrown() {
        when(eventService.getRecommendEvents(any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<Object> response = eventController.getRecommendEvents(
                null, null, null, null, null, 1, 10, null);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while retrieving recommended events", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getPendingEvents_ShouldReturnOk_WhenEventsRetrievedSuccessfully() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("events", List.of("Event 1", "Event 2"));

        when(eventService.getPendingEvents(eq(request), eq("Hanoi"), any(), any(), eq("Concert"),
                eq(1), eq(10)))
                .thenReturn(mockResponse);

        ResponseEntity<Object> response = eventController.getPendingEvents(
                request, "Hanoi", null, null, "Concert", 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Pending events retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertEquals(mockResponse, body.get("data"));
    }

    @Test
    void getPendingEvents_ShouldResetPageAndSize_WhenPageAndSizeAreInvalid() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("events", List.of("Event A", "Event B"));

        when(eventService.getPendingEvents(eq(request), eq("HCM"), any(), any(), eq("Gallery"),
                eq(1), eq(1)))
                .thenReturn(mockResponse);

        ResponseEntity<Object> response = eventController.getPendingEvents(
                request, "HCM", null, null, "Gallery", 0, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Pending events retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertEquals(mockResponse, body.get("data"));
    }

    @Test
    void getPendingEvents_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        when(eventService.getPendingEvents(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SecurityException("Access denied"));

        ResponseEntity<Object> response = eventController.getPendingEvents(
                request, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getPendingEvents_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        when(eventService.getPendingEvents(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("No pending events found"));

        ResponseEntity<Object> response = eventController.getPendingEvents(
                request, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("No pending events found", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getPendingEvents_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        when(eventService.getPendingEvents(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<Object> response = eventController.getPendingEvents(
                request, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while retrieving pending events", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getListEvents_ShouldReturnOk_WhenEventsRetrievedSuccessfully() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("events", List.of("Event 1", "Event 2"));

        when(eventService.getListEvents(eq(request), eq("ACTIVE"), eq("APPROVED"),
                eq("Hanoi"), any(), any(), eq("Concert"), eq(1), eq(10)))
                .thenReturn(mockResponse);

        ResponseEntity<Object> response = eventController.getListEvents(
                request, "ACTIVE", "APPROVED", "Hanoi",
                null, null, "Concert", 1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Events retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertEquals(mockResponse, body.get("data"));
    }

    @Test
    void getListEvents_ShouldResetPageAndSize_WhenPageAndSizeAreInvalid() {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("events", List.of("Event A", "Event B"));

        when(eventService.getListEvents(eq(request), eq(null), eq(null),
                eq("HCM"), any(), any(), eq("Gallery"), eq(1), eq(1)))
                .thenReturn(mockResponse);

        ResponseEntity<Object> response = eventController.getListEvents(
                request, null, null, "HCM",
                null, null, "Gallery", 0, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Events retrieved successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertEquals(mockResponse, body.get("data"));
    }

    @Test
    void getListEvents_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        when(eventService.getListEvents(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new SecurityException("Access denied"));

        ResponseEntity<Object> response = eventController.getListEvents(
                request, null, null, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getListEvents_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        when(eventService.getListEvents(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("No events found"));

        ResponseEntity<Object> response = eventController.getListEvents(
                request, null, null, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("No events found", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void getListEvents_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        when(eventService.getListEvents(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("Unexpected error"));

        ResponseEntity<Object> response = eventController.getListEvents(
                request, null, null, null, null, null, null, 1, 10);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while retrieving events", body.get("message"));
        assertNull(body.get("data"));
    }

    @Test
    void updateEvent_ShouldReturnOk_WhenUpdateSuccessful() {
        UpdateEventRequestDto eventDto = new UpdateEventRequestDto();

        when(bindingResult.hasErrors()).thenReturn(false);

        ResponseEntity<Object> response = eventController.updateEvent(
                1, eventDto, logoFile, backgroundFile, bindingResult, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Event updated successfully", body.get("message"));
        assertEquals(200, body.get("status"));
        assertNull(body.get("data"));

        verify(eventService, times(1))
                .updateEvent(1, eventDto, logoFile, backgroundFile, request);
    }

    @Test
    void updateEvent_ShouldReturnBadRequest_WhenValidationFails() {
        UpdateEventRequestDto eventDto = new UpdateEventRequestDto();

        when(bindingResult.hasErrors()).thenReturn(true);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("eventRequestDto", "name", "Name is required")
        ));

        ResponseEntity<Object> response = eventController.updateEvent(
                1, eventDto, null, null, bindingResult, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Validation failed", body.get("message"));
        Map<String, String> errors = (Map<String, String>) body.get("data");
        assertEquals("Name is required", errors.get("name"));

        verify(eventService, never()).updateEvent(any(), any(), any(), any(), any());
    }

    @Test
    void updateEvent_ShouldReturnBadRequest_WhenIllegalArgumentExceptionThrown() {
        UpdateEventRequestDto eventDto = new UpdateEventRequestDto();

        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new IllegalArgumentException("Invalid event data"))
                .when(eventService).updateEvent(anyInt(), any(), any(), any(), any());

        ResponseEntity<Object> response = eventController.updateEvent(
                1, eventDto, null, null, bindingResult, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Invalid event data", body.get("message"));
    }

    @Test
    void updateEvent_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        UpdateEventRequestDto eventDto = new UpdateEventRequestDto();

        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new SecurityException("Access denied"))
                .when(eventService).updateEvent(anyInt(), any(), any(), any(), any());

        ResponseEntity<Object> response = eventController.updateEvent(
                1, eventDto, null, null, bindingResult, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
    }

    @Test
    void updateEvent_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        UpdateEventRequestDto eventDto = new UpdateEventRequestDto();

        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new EntityNotFoundException("Event not found"))
                .when(eventService).updateEvent(anyInt(), any(), any(), any(), any());

        ResponseEntity<Object> response = eventController.updateEvent(
                1, eventDto, null, null, bindingResult, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Event not found", body.get("message"));
    }

    @Test
    void updateEvent_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        UpdateEventRequestDto eventDto = new UpdateEventRequestDto();

        when(bindingResult.hasErrors()).thenReturn(false);
        doThrow(new RuntimeException("Unexpected error"))
                .when(eventService).updateEvent(anyInt(), any(), any(), any(), any());

        ResponseEntity<Object> response = eventController.updateEvent(
                1, eventDto, null, null, bindingResult, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while updating the event", body.get("message"));
    }

    @Test
    void generateExcelReport_ShouldReturnExcelFile_WhenSuccess() {
        int eventId = 1;
        byte[] excelData = "dummy excel data".getBytes();

        when(eventService.generateExcelReport(request, eventId)).thenReturn(excelData);

        ResponseEntity<?> response = eventController.generateExcelReport(request, eventId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst("Content-Disposition")
                .contains("attachment; filename=event_report.xlsx"));
        assertArrayEquals(excelData, (byte[]) response.getBody());

        verify(eventService, times(1)).generateExcelReport(request, eventId);
    }

    @Test
    void generateExcelReport_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        int eventId = 1;

        doThrow(new SecurityException("Access denied"))
                .when(eventService).generateExcelReport(request, eventId);

        ResponseEntity<?> response = eventController.generateExcelReport(request, eventId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
        assertEquals(403, body.get("status"));
    }

    @Test
    void generateExcelReport_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        int eventId = 1;

        doThrow(new EntityNotFoundException("Event not found"))
                .when(eventService).generateExcelReport(request, eventId);

        ResponseEntity<?> response = eventController.generateExcelReport(request, eventId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Event not found", body.get("message"));
        assertEquals(404, body.get("status"));
    }

    @Test
    void generateExcelReport_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        int eventId = 1;

        doThrow(new RuntimeException("Unexpected error"))
                .when(eventService).generateExcelReport(request, eventId);

        ResponseEntity<?> response = eventController.generateExcelReport(request, eventId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while generating the report", body.get("message"));
        assertEquals(500, body.get("status"));
    }

    @Test
    void generatePdfReport_ShouldReturnPdfFile_WhenSuccess() {
        int eventId = 1;
        byte[] pdfData = "dummy pdf data".getBytes();

        when(eventService.generatePdfReport(request, eventId)).thenReturn(pdfData);

        ResponseEntity<?> response = eventController.generatePdfReport(request, eventId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst("Content-Disposition")
                .contains("attachment; filename=event_report.pdf"));
        assertArrayEquals(pdfData, (byte[]) response.getBody());

        verify(eventService, times(1)).generatePdfReport(request, eventId);
    }

    @Test
    void generatePdfReport_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        int eventId = 1;
        doThrow(new SecurityException("Access denied"))
                .when(eventService).generatePdfReport(request, eventId);

        ResponseEntity<?> response = eventController.generatePdfReport(request, eventId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
        assertEquals(403, body.get("status"));
    }

    @Test
    void generatePdfReport_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        int eventId = 1;
        doThrow(new EntityNotFoundException("Event not found"))
                .when(eventService).generatePdfReport(request, eventId);

        ResponseEntity<?> response = eventController.generatePdfReport(request, eventId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Event not found", body.get("message"));
        assertEquals(404, body.get("status"));
    }

    @Test
    void generatePdfReport_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        int eventId = 1;
        doThrow(new RuntimeException("Unexpected error"))
                .when(eventService).generatePdfReport(request, eventId);

        ResponseEntity<?> response = eventController.generatePdfReport(request, eventId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while generating the report", body.get("message"));
        assertEquals(500, body.get("status"));
    }

    @Test
    void generateBuyerReportExcel_ShouldReturnExcelFile_WhenSuccess() {
        int eventId = 1;
        byte[] excelData = "dummy excel data".getBytes();

        when(eventService.generateBuyerReportExcel(request, eventId)).thenReturn(excelData);

        ResponseEntity<?> response = eventController.generateBuyerReportExcel(request, eventId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst("Content-Disposition")
                .contains("buyer_report.xlsx"));
        assertArrayEquals(excelData, (byte[]) response.getBody());

        verify(eventService, times(1)).generateBuyerReportExcel(request, eventId);
    }

    @Test
    void generateBuyerReportExcel_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        int eventId = 1;
        doThrow(new SecurityException("Access denied"))
                .when(eventService).generateBuyerReportExcel(request, eventId);

        ResponseEntity<?> response = eventController.generateBuyerReportExcel(request, eventId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
        assertEquals(403, body.get("status"));
    }

    @Test
    void generateBuyerReportExcel_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        int eventId = 1;
        doThrow(new EntityNotFoundException("Event not found"))
                .when(eventService).generateBuyerReportExcel(request, eventId);

        ResponseEntity<?> response = eventController.generateBuyerReportExcel(request, eventId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Event not found", body.get("message"));
        assertEquals(404, body.get("status"));
    }

    @Test
    void generateBuyerReportExcel_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        int eventId = 1;
        doThrow(new RuntimeException("Unexpected error"))
                .when(eventService).generateBuyerReportExcel(request, eventId);

        ResponseEntity<?> response = eventController.generateBuyerReportExcel(request, eventId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while generating the buyer report", body.get("message"));
        assertEquals(500, body.get("status"));
    }

    @Test
    void generateBuyerReportPdf_ShouldReturnPdfFile_WhenSuccess() {
        int eventId = 1;
        byte[] pdfData = "dummy pdf data".getBytes();

        when(eventService.generateBuyerReportPdf(request, eventId)).thenReturn(pdfData);

        ResponseEntity<?> response = eventController.generateBuyerReportPdf(request, eventId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
        assertTrue(response.getHeaders().getFirst("Content-Disposition")
                .contains("buyer_report.pdf"));
        assertArrayEquals(pdfData, (byte[]) response.getBody());

        verify(eventService, times(1)).generateBuyerReportPdf(request, eventId);
    }

    @Test
    void generateBuyerReportPdf_ShouldReturnForbidden_WhenSecurityExceptionThrown() {
        int eventId = 1;
        doThrow(new SecurityException("Access denied"))
                .when(eventService).generateBuyerReportPdf(request, eventId);

        ResponseEntity<?> response = eventController.generateBuyerReportPdf(request, eventId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Access denied", body.get("message"));
        assertEquals(403, body.get("status"));
    }

    @Test
    void generateBuyerReportPdf_ShouldReturnNotFound_WhenEntityNotFoundExceptionThrown() {
        int eventId = 1;
        doThrow(new EntityNotFoundException("Event not found"))
                .when(eventService).generateBuyerReportPdf(request, eventId);

        ResponseEntity<?> response = eventController.generateBuyerReportPdf(request, eventId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Event not found", body.get("message"));
        assertEquals(404, body.get("status"));
    }

    @Test
    void generateBuyerReportPdf_ShouldReturnInternalServerError_WhenUnexpectedExceptionThrown() {
        int eventId = 1;
        doThrow(new RuntimeException("Unexpected error"))
                .when(eventService).generateBuyerReportPdf(request, eventId);

        ResponseEntity<?> response = eventController.generateBuyerReportPdf(request, eventId);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("An unexpected error occurred while generating the buyer report", body.get("message"));
        assertEquals(500, body.get("status"));
    }
}
