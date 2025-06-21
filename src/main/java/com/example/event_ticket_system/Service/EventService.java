package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface EventService {
    Integer createEvent(EventRequestDto eventRequestDto,
                        MultipartFile logoFile,
                        MultipartFile backgroundFile,
                        HttpServletRequest request);
}
