package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import jakarta.servlet.http.HttpServletRequest;

public interface EventService {
    Integer createEvent(EventRequestDto eventRequestDto, HttpServletRequest request);
}
