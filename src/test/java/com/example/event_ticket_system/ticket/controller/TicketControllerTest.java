package com.example.event_ticket_system.ticket.controller;

import com.example.event_ticket_system.Controller.TicketController;
import com.example.event_ticket_system.DTO.response.TicketResponseDTO;
import com.example.event_ticket_system.Service.TicketService;
import com.example.event_ticket_system.Enums.OrderStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController).build();
    }

    @Test
    void getTicketsByUserId_ShouldReturnTickets_WhenSuccess() throws Exception {
        Integer userId = 1;

        List<TicketResponseDTO> tickets = getTicketResponseDTOS();

        when(ticketService.getTicketsByUserId(eq(userId), any(HttpServletRequest.class)))
                .thenReturn(tickets);

        mockMvc.perform(get("/api/users/ticket/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tickets retrieved successfully"))
                .andExpect(jsonPath("$.data[0].orderId").value(5001))
                .andExpect(jsonPath("$.data[0].status").value("paid"))
                .andExpect(jsonPath("$.data[0].tickets[0].eventName").value("Concert A"))
                .andExpect(jsonPath("$.data[0].tickets[0].ticketType").value("VIP"))
                .andExpect(jsonPath("$.data[0].tickets[0].quantity").value(2));
    }

    private static List<TicketResponseDTO> getTicketResponseDTOS() {
        TicketResponseDTO.TicketDetail detail = new TicketResponseDTO.TicketDetail(
                101,
                1001,
                "Concert A",
                LocalDateTime.of(2025, 8, 20, 19, 0),
                LocalDateTime.of(2025, 8, 20, 22, 0),
                "Music",
                "Hanoi Opera House",
                "VIP",
                50.0,
                2,
                null
        );

        TicketResponseDTO ticketResponse = new TicketResponseDTO(
                5001,
                LocalDateTime.of(2025, 8, 1, 10, 0),
                100.0,
                OrderStatus.paid,
                123456789L,
                null,
                Collections.singletonList(detail)
        );

        List<TicketResponseDTO> tickets = List.of(ticketResponse);
        return tickets;
    }

    @Test
    void getTicketsByUserId_ShouldReturn403_WhenForbidden() throws Exception {
        Integer userId = 1;

        when(ticketService.getTicketsByUserId(eq(userId), any(HttpServletRequest.class)))
                .thenThrow(new SecurityException("Forbidden"));

        mockMvc.perform(get("/api/users/ticket/{userId}", userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to view tickets for this user."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getTicketsByUserId_ShouldReturn500_WhenUnexpectedError() throws Exception {
        Integer userId = 1;

        when(ticketService.getTicketsByUserId(eq(userId), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/users/ticket/{userId}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An error occurred while retrieving tickets: DB error"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
