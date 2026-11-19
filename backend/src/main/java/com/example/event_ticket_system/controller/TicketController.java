package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.dto.response.TicketResponseDTO;
import com.example.event_ticket_system.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/users/ticket/{userId}")
    @PreAuthorize("hasAuthority('ROLE_customer') and #userId == authentication.principal.userId")
    public ResponseEntity<ApiResponse<List<TicketResponseDTO>>> getTicketsByUserId(@PathVariable Integer userId) {
        List<TicketResponseDTO> tickets = ticketService.getTicketsByUserId(userId);
        return ApiResponse.ok(tickets, "Tickets retrieved successfully");
    }
}
