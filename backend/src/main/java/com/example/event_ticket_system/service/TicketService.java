package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.response.DailyTicketSalesDTO;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesDTO;
import com.example.event_ticket_system.dto.response.TicketResponseDTO;

import java.util.List;

public interface TicketService {
    List<TicketResponseDTO> getTicketsByUserId(Integer userId);
    List<DailyTicketSalesDTO> getTicketsSoldPerDay(Integer eventId);
    List<DailyTicketTypeSalesDTO> getTicketsSoldPerTicketType(Integer eventId, String ticketType);
}
