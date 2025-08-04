package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.DTO.response.TicketResponseDTO;
import com.example.event_ticket_system.Service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<?> getTicketsByUserId(@PathVariable Integer userId, HttpServletRequest request) {
        try {
            List<TicketResponseDTO> tickets = ticketService.getTicketsByUserId(userId, request);
            return APIResponse.responseBuilder(
                    tickets,
                    "Tickets retrieved successfully",
                    HttpStatus.OK
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    "You do not have permission to view tickets for this user.",
                    HttpStatus.FORBIDDEN
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An error occurred while retrieving tickets: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
