package com.example.event_ticket_system.ticket.controller;

import com.example.event_ticket_system.controller.TicketController;
import com.example.event_ticket_system.dto.response.TicketResponseDTO;
import com.example.event_ticket_system.enums.OrderStatus;
import com.example.event_ticket_system.exception.GlobalExceptionHandler;
import com.example.event_ticket_system.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getTicketsByUserId_ShouldReturnTickets_WhenSuccess() throws Exception {
        Integer userId = 1;
        List<TicketResponseDTO> tickets = getTicketResponseDTOS();

        when(ticketService.getTicketsByUserId(userId)).thenReturn(tickets);

        mockMvc.perform(get("/api/users/ticket/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Tickets retrieved successfully"))
                .andExpect(jsonPath("$.data[0].orderId").value(5001))
                .andExpect(jsonPath("$.data[0].status").value("paid"))
                .andExpect(jsonPath("$.data[0].tickets[0].eventName").value("Concert A"))
                .andExpect(jsonPath("$.data[0].tickets[0].ticketType").value("VIP"))
                .andExpect(jsonPath("$.data[0].tickets[0].quantity").value(2))
                .andExpect(jsonPath("$.data[0].tickets[0].checkinToken").value("v1:5001:1001:101:1:10:2.sig"));
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
                new BigDecimal("50.00"),
                2,
                "v1:5001:1001:101:1:10:2.sig",
                null
        );

        TicketResponseDTO ticketResponse = new TicketResponseDTO(
                5001,
                LocalDateTime.of(2025, 8, 1, 10, 0),
                new BigDecimal("100.00"),
                OrderStatus.paid,
                123456789L,
                null,
                Collections.singletonList(detail)
        );

        return List.of(ticketResponse);
    }

    @Test
    void getTicketsByUserId_ShouldReturn403_WhenForbidden() throws Exception {
        Integer userId = 1;

        when(ticketService.getTicketsByUserId(userId))
                .thenThrow(new SecurityException("You do not have permission to view tickets for this user."));

        mockMvc.perform(get("/api/users/ticket/{userId}", userId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You do not have permission to view tickets for this user."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getTicketsByUserId_ShouldReturn500_WhenUnexpectedError() throws Exception {
        Integer userId = 1;

        when(ticketService.getTicketsByUserId(userId))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/users/ticket/{userId}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
