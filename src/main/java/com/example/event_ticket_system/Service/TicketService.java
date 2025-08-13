package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.response.DailyTicketSalesDTO;
import com.example.event_ticket_system.DTO.response.DailyTicketTypeSalesDTO;
import com.example.event_ticket_system.DTO.response.TicketResponseDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface TicketService {
    List<TicketResponseDTO> getTicketsByUserId(Integer userId, HttpServletRequest request);
    List<DailyTicketSalesDTO> getTicketsSoldPerDay(Integer eventId, HttpServletRequest request);
    List<DailyTicketTypeSalesDTO> getTicketsSoldPerTicketType(Integer eventId, String ticketType, HttpServletRequest request);
}
