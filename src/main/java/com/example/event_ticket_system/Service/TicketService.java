package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.response.TicketResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface TicketService {
    List<TicketResponseDTO> getTicketsByUserId(Integer userId, HttpServletRequest request);
}
