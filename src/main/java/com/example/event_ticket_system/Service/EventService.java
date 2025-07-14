package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.request.UpdateEventRequestDto;
import com.example.event_ticket_system.DTO.response.DetailEventResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

public interface EventService {
    Integer createEvent(EventRequestDto eventRequestDto, MultipartFile logoFile, MultipartFile backgroundFile, HttpServletRequest request);
    DetailEventResponseDto getEventById(Integer eventId, HttpServletRequest request);
    Map<String,Object> getEventsByOrganizer(HttpServletRequest request, String status, String approveStatus, LocalDateTime startTime, LocalDateTime endTime, String name, Integer page, Integer size);
    Map<String, Object> getRecommendEvents(String category, String address, LocalDateTime startTime, LocalDateTime endTime, String name, Integer page, Integer size);
    Map<String, Object> getPendingEvents(HttpServletRequest request,String address, LocalDateTime startTime, LocalDateTime endTime, String name,  Integer page, Integer size);
    void updateEvent(Integer eventId, UpdateEventRequestDto eventRequestDto, MultipartFile logoFile, MultipartFile backgroundFile, HttpServletRequest request);
    Map<String, Object> getListEvents(HttpServletRequest request,String status, String approvalStatus, String address, LocalDateTime startTime, LocalDateTime endTime, String name, Integer page, Integer size);
}
