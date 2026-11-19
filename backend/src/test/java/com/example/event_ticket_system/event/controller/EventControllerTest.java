package com.example.event_ticket_system.event.controller;

import com.example.event_ticket_system.controller.EventController;
import com.example.event_ticket_system.dto.request.EventRequestDto;
import com.example.event_ticket_system.dto.request.UpdateEventRequestDto;
import com.example.event_ticket_system.dto.response.DetailEventResponseDto;
import com.example.event_ticket_system.exception.GlobalExceptionHandler;
import com.example.event_ticket_system.service.EventCommandService;
import com.example.event_ticket_system.service.EventQueryService;
import com.example.event_ticket_system.service.EventReportService;
import com.example.event_ticket_system.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventCommandService eventCommandService;

    @Mock
    private EventQueryService eventQueryService;

    @Mock
    private EventReportService eventReportService;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
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
        ticket.setPrice(new BigDecimal("500.00"));
        requestDto.setTickets(List.of(ticket));

        EventRequestDto.DiscountRequest discount = new EventRequestDto.DiscountRequest();
        discount.setDiscountCode("DISC10");
        discount.setDiscountDescription("10% off");
        discount.setDiscountType("percentage");
        discount.setDiscountValue(new BigDecimal("10.00"));
        discount.setDiscountValidFrom(LocalDate.now());
        discount.setDiscountValidTo(LocalDate.now().plusDays(5));
        discount.setDiscountMaxUses(50);
        requestDto.setDiscounts(List.of(discount));

        return requestDto;
    }

    @Test
    void createEvent_ShouldReturn201_WhenSuccess() throws Exception {
        String jsonData = objectMapper.writeValueAsString(createValidEventRequest());
        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile("logo", "logo.png", "image/png", "fake-logo".getBytes());
        MockMultipartFile backgroundPart = new MockMultipartFile("background", "bg.png", "image/png", "fake-bg".getBytes());

        when(eventCommandService.createEvent(any(EventRequestDto.class), any(), any())).thenReturn(1);

        mockMvc.perform(multipart("/api/events")
                        .file(dataPart)
                        .file(logoPart)
                        .file(backgroundPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Event created successfully"))
                .andExpect(jsonPath("$.data").value(1));
    }

    @Test
    void createEvent_ShouldReturn400_WhenValidationFails() throws Exception {
        String invalidJson = "{}";
        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", invalidJson.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile("logo", "logo.png", "image/png", "fake-logo".getBytes());
        MockMultipartFile backgroundPart = new MockMultipartFile("background", "bg.png", "image/png", "fake-bg".getBytes());

        mockMvc.perform(multipart("/api/events")
                        .file(dataPart)
                        .file(logoPart)
                        .file(backgroundPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createEvent_ShouldReturn403_WhenSecurityException() throws Exception {
        String jsonData = objectMapper.writeValueAsString(createValidEventRequest());
        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        MockMultipartFile logoPart = new MockMultipartFile("logo", "logo.png", "image/png", "fake-logo".getBytes());
        MockMultipartFile backgroundPart = new MockMultipartFile("background", "bg.png", "image/png", "fake-bg".getBytes());

        doThrow(new SecurityException("You do not have permission"))
                .when(eventCommandService).createEvent(any(EventRequestDto.class), any(), any());

        mockMvc.perform(multipart("/api/events")
                        .file(dataPart)
                        .file(logoPart)
                        .file(backgroundPart))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission"));
    }

    @Test
    void getEventsByOrganizer_ShouldReturnOk_WhenDataExists() throws Exception {
        when(eventQueryService.getEventsByOrganizer(eq("upcoming"), eq("approved"), any(), any(), eq("Concert"), eq(1), eq(10)))
                .thenReturn(Map.of("listEvents", List.of(), "pageNo", 1, "totalPages", 1));

        mockMvc.perform(get("/api/events/by-organizer")
                        .param("name", "Concert")
                        .param("status", "upcoming")
                        .param("approveStatus", "approved")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Events retrieved successfully"))
                .andExpect(jsonPath("$.data.pageNo").value(1));
    }

    @Test
    void getEventById_ShouldReturnOk_WhenEventExists() throws Exception {
        DetailEventResponseDto mockEvent = new DetailEventResponseDto();
        mockEvent.setEventId(1);
        mockEvent.setEventName("Music Festival");

        when(eventQueryService.getEventById(1)).thenReturn(mockEvent);

        mockMvc.perform(get("/api/events/{eventId}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Event retrieved successfully"))
                .andExpect(jsonPath("$.data.eventId").value(1))
                .andExpect(jsonPath("$.data.eventName").value("Music Festival"));
    }

    @Test
    void getEventById_ShouldReturn404_WhenNotFound() throws Exception {
        when(eventQueryService.getEventById(99)).thenThrow(new EntityNotFoundException("Event not found"));

        mockMvc.perform(get("/api/events/{eventId}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found"));
    }

    @Test
    void getRecommendEvents_ShouldReturnOk() throws Exception {
        when(eventQueryService.getRecommendEvents(eq("Music"), eq("Hanoi"), any(), any(), eq("Concert"), eq(1), eq(10), eq("startTime:ASC")))
                .thenReturn(Map.of("listEvents", List.of(), "pageNo", 1));

        mockMvc.perform(get("/api/events/recommend")
                        .param("category", "Music")
                        .param("address", "Hanoi")
                        .param("name", "Concert")
                        .param("sortBy", "startTime:ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recommended events retrieved successfully"));
    }

    @Test
    void getPendingEvents_ShouldReturnOk() throws Exception {
        when(eventQueryService.getPendingEvents(eq("Hanoi"), any(), any(), eq("Concert"), eq(1), eq(10)))
                .thenReturn(Map.of("listEvents", List.of(), "pageNo", 1));

        mockMvc.perform(get("/api/events/pending")
                        .param("address", "Hanoi")
                        .param("name", "Concert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pending events retrieved successfully"));
    }

    @Test
    void updateEvent_ShouldReturnOk_WhenUpdateSuccessful() throws Exception {
        UpdateEventRequestDto dto = new UpdateEventRequestDto();
        dto.setEventName("Updated Concert");
        String jsonData = objectMapper.writeValueAsString(dto);

        MockMultipartFile dataPart = new MockMultipartFile("data", "", "application/json", jsonData.getBytes());
        doNothing().when(eventCommandService).updateEvent(eq(1), any(UpdateEventRequestDto.class), any(), any());

        mockMvc.perform(multipart("/api/events/{eventId}", 1)
                        .file(dataPart)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Event updated successfully"));

        verify(eventCommandService).updateEvent(eq(1), any(UpdateEventRequestDto.class), any(), any());
    }

    @Test
    void generateExcelReport_ShouldReturnExcelBytes_WhenSuccess() throws Exception {
        byte[] excelData = "dummy-excel".getBytes();
        when(eventReportService.generateExcelReport(1)).thenReturn(excelData);

        mockMvc.perform(get("/api/events/{eventId}/report/excel", 1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=event_report.xlsx"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(excelData));
    }

    @Test
    void generatePdfReport_ShouldReturnPdfBytes_WhenSuccess() throws Exception {
        byte[] pdfData = "%PDF-1.4".getBytes();
        when(eventReportService.generatePdfReport(1)).thenReturn(pdfData);

        mockMvc.perform(get("/api/events/{eventId}/report/pdf", 1))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=event_report.pdf"))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfData));
    }

    @Test
    void generateBuyerReportPdf_ShouldReturn403_WhenForbidden() throws Exception {
        when(eventReportService.generateBuyerReportPdf(1))
                .thenThrow(new SecurityException("You do not have permission to access reports for this event."));

        mockMvc.perform(get("/api/events/{eventId}/report/buyer-pdf", 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to access reports for this event."));
    }
}
