package com.example.event_ticket_system.event.controller;

import com.example.event_ticket_system.controller.EventController;
import com.example.event_ticket_system.dto.response.DailyTicketSalesDTO;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesDTO;
import com.example.event_ticket_system.exception.GlobalExceptionHandler;
import com.example.event_ticket_system.service.EventCommandService;
import com.example.event_ticket_system.service.EventQueryService;
import com.example.event_ticket_system.service.EventReportService;
import com.example.event_ticket_system.service.TicketService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTicketReportTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTicketsSoldPerDay_ReturnsDataSuccessfully() throws Exception {
        List<DailyTicketSalesDTO> mockData = List.of(
                new DailyTicketSalesDTO(LocalDate.of(2025, 8, 29), 100L),
                new DailyTicketSalesDTO(LocalDate.of(2025, 8, 30), 150L)
        );

        when(ticketService.getTicketsSoldPerDay(1)).thenReturn(mockData);

        mockMvc.perform(get("/api/events/report/ticket-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Ticket sales report retrieved successfully"))
                .andExpect(jsonPath("$.data[0].totalTicketSold").value(100))
                .andExpect(jsonPath("$.data[1].totalTicketSold").value(150));
    }

    @Test
    void getTicketsSoldPerDay_SecurityException_Returns403() throws Exception {
        when(ticketService.getTicketsSoldPerDay(1))
                .thenThrow(new SecurityException("Access denied"));

        mockMvc.perform(get("/api/events/report/ticket-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getTicketsSoldPerDay_EntityNotFound_Returns404() throws Exception {
        when(ticketService.getTicketsSoldPerDay(1))
                .thenThrow(new EntityNotFoundException("Event not found"));

        mockMvc.perform(get("/api/events/report/ticket-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found"));
    }

    @Test
    void getTicketsSoldPerTicketType_ReturnsDataSuccessfully() throws Exception {
        List<DailyTicketTypeSalesDTO> mockData = List.of(
                new DailyTicketTypeSalesDTO(LocalDate.of(2025, 8, 29), "VIP", 50L),
                new DailyTicketTypeSalesDTO(LocalDate.of(2025, 8, 30), "Standard", 80L)
        );

        when(ticketService.getTicketsSoldPerTicketType(1, "VIP")).thenReturn(mockData);

        mockMvc.perform(get("/api/events/report/ticket-type-per-day/1")
                        .param("ticketType", "VIP")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Ticket type sales report retrieved successfully"))
                .andExpect(jsonPath("$.data[0].ticketType").value("VIP"))
                .andExpect(jsonPath("$.data[0].totalQuantity").value(50))
                .andExpect(jsonPath("$.data[1].ticketType").value("Standard"))
                .andExpect(jsonPath("$.data[1].totalQuantity").value(80));
    }

    @Test
    void getTicketsSoldPerTicketType_SecurityException_Returns403() throws Exception {
        when(ticketService.getTicketsSoldPerTicketType(1, null))
                .thenThrow(new SecurityException("Access denied"));

        mockMvc.perform(get("/api/events/report/ticket-type-per-day/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
