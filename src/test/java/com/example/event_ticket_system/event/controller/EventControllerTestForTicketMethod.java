package com.example.event_ticket_system.event.controller;

import com.example.event_ticket_system.Controller.EventController;
import com.example.event_ticket_system.DTO.response.DailyTicketSalesDTO;
import com.example.event_ticket_system.DTO.response.DailyTicketTypeSalesDTO;
import com.example.event_ticket_system.Service.EventService;
import com.example.event_ticket_system.Service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class EventControllerTestForTicketMethod {

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

    @Test
    void getTicketsSoldPerDay_ReturnsDataSuccessfully() throws Exception {
        // Mock data for the service
        List<DailyTicketSalesDTO> mockData = List.of(
                new DailyTicketSalesDTO(LocalDate.of(2025, 8, 29), 100L),
                new DailyTicketSalesDTO(LocalDate.of(2025, 8, 30), 150L)
        );

        // Ensure the service mock returns the expected data
        when(ticketService.getTicketsSoldPerDay(eq(1), any(HttpServletRequest.class)))
                .thenReturn(mockData);

        // Perform the request and verify the response
        mockMvc.perform(get("/api/events/report/ticket-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Expect HTTP 200 OK
                .andExpect(jsonPath("$.message").value("Ticket sales report retrieved successfully"))

                // Check first date array [2025, 8, 29]
                .andExpect(jsonPath("$.data[0].date[0]").value(2025))
                .andExpect(jsonPath("$.data[0].date[1]").value(8))
                .andExpect(jsonPath("$.data[0].date[2]").value(29))
                .andExpect(jsonPath("$.data[0].totalTicketSold").value(100))

                // Check second date array [2025, 8, 30]
                .andExpect(jsonPath("$.data[1].date[0]").value(2025))
                .andExpect(jsonPath("$.data[1].date[1]").value(8))
                .andExpect(jsonPath("$.data[1].date[2]").value(30))
                .andExpect(jsonPath("$.data[1].totalTicketSold").value(150));
    }

    @Test
    void getTicketsSoldPerDay_NoDataFound() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();

        when(ticketService.getTicketsSoldPerDay(eq(1), any(HttpServletRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/events/report/ticket-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No data found for this event"));
    }

    @Test
    void getTicketsSoldPerDay_SecurityException() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();

        when(ticketService.getTicketsSoldPerDay(eq(1), any(HttpServletRequest.class)))
                .thenThrow(new SecurityException("Access denied"));

        mockMvc.perform(get("/api/events/report/ticket-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getTicketsSoldPerDay_EntityNotFoundException() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();

        when(ticketService.getTicketsSoldPerDay(eq(1), any(HttpServletRequest.class)))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("Event not found"));

        mockMvc.perform(get("/api/events/report/ticket-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found"));
    }

    @Test
    void getTicketsSoldPerDay_InternalServerError() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();

        doThrow(new RuntimeException("Unexpected error"))
                .when(ticketService).getTicketsSoldPerDay(eq(1), any(HttpServletRequest.class));

        ResponseEntity<?> response = eventController.getTicketsSoldPerDay(1, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assert body != null;
        assertEquals("Unexpected error", body.get("message"));
        assertEquals(500, body.get("status"));
    }

    @Test
    void getTicketsSoldPerTicketType_ReturnsDataSuccessfully() throws Exception {
        List<DailyTicketTypeSalesDTO> mockData = List.of(
                new DailyTicketTypeSalesDTO(LocalDate.of(2025, 8, 29), "VIP", 50L),
                new DailyTicketTypeSalesDTO(LocalDate.of(2025, 8, 30), "Standard", 80L)
        );

        when(ticketService.getTicketsSoldPerTicketType(eq(1), eq("VIP"), any(HttpServletRequest.class)))
                .thenReturn(mockData);

        mockMvc.perform(get("/api/events/report/ticket-type-per-day/1")
                        .param("ticketType", "VIP")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Ticket type sales report retrieved successfully"))

                // First item
                .andExpect(jsonPath("$.data[0].date[0]").value(2025))
                .andExpect(jsonPath("$.data[0].date[1]").value(8))
                .andExpect(jsonPath("$.data[0].date[2]").value(29))
                .andExpect(jsonPath("$.data[0].ticketType").value("VIP"))
                .andExpect(jsonPath("$.data[0].totalQuantity").value(50))

                // Second item
                .andExpect(jsonPath("$.data[1].date[0]").value(2025))
                .andExpect(jsonPath("$.data[1].date[1]").value(8))
                .andExpect(jsonPath("$.data[1].date[2]").value(30))
                .andExpect(jsonPath("$.data[1].ticketType").value("Standard"))
                .andExpect(jsonPath("$.data[1].totalQuantity").value(80));
    }

    @Test
    void getTicketsSoldPerTicketType_NoDataFound() throws Exception {
        when(ticketService.getTicketsSoldPerTicketType(eq(1), eq("VIP"), any(HttpServletRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/events/report/ticket-type-per-day/1")
                        .param("ticketType", "VIP")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No data found for this event"));
    }

    @Test
    void getTicketsSoldPerTicketType_SecurityException() throws Exception {
        when(ticketService.getTicketsSoldPerTicketType(eq(1), any(), any(HttpServletRequest.class)))
                .thenThrow(new SecurityException("Access denied"));

        mockMvc.perform(get("/api/events/report/ticket-type-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getTicketsSoldPerTicketType_EntityNotFoundException() throws Exception {
        when(ticketService.getTicketsSoldPerTicketType(eq(1), any(), any(HttpServletRequest.class)))
                .thenThrow(new EntityNotFoundException("Event not found"));

        mockMvc.perform(get("/api/events/report/ticket-type-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found"));
    }

    @Test
    void getTicketsSoldPerTicketType_InternalServerError() throws Exception {
        when(ticketService.getTicketsSoldPerTicketType(eq(1), any(), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/events/report/ticket-type-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"));
    }
}
