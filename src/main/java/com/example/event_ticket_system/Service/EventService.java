package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

public interface EventService {
    Integer createEvent(EventRequestDto eventRequestDto,
                        MultipartFile logoFile,
                        MultipartFile backgroundFile,
                        HttpServletRequest request);

    Map<String,Object> getEventsByOrganizer(HttpServletRequest request, String approveStatus, LocalDateTime startTime, LocalDateTime endTime, String name, Integer page, Integer size);
}
