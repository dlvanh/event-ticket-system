package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.DTO.response.DetailEventResponseDto;
import com.example.event_ticket_system.Service.EventService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/events")
@RequiredArgsConstructor
public class EventController {
    @Autowired
    private final EventService eventService;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<Object> createEvent (@RequestPart("data") @Valid EventRequestDto eventRequestDto,
                                               @RequestPart("logo") MultipartFile logoFile,
                                               @RequestPart("background") MultipartFile backgroundFile,
                                               BindingResult bindingResult,
                                               HttpServletRequest request) {
        try {
            Map<String, String> errors = new HashMap<>();
            if (bindingResult.hasErrors()) {
                bindingResult.getFieldErrors().forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );
            }
            if (!errors.isEmpty()) {
                return APIResponse.responseBuilder(
                        errors,
                        "Validation failed",
                        HttpStatus.BAD_REQUEST
                );
            }
            Integer eventId = eventService.createEvent(eventRequestDto, logoFile,backgroundFile, request);
            return APIResponse.responseBuilder(
                    eventId,
                    "Event created successfully",
                    HttpStatus.CREATED
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while creating the event",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/by-organizer")
    public ResponseEntity<Object> getEventsByOrganizer(
            HttpServletRequest request,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approveStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            if(page<=0&&size<=0) {
            page = 1;
            size = 1;
            }
            Map<String, Object> response = eventService.getEventsByOrganizer(request, status, approveStatus, startTime, endTime, name, page, size);
            return APIResponse.responseBuilder(
                    response,
                    "Events retrieved successfully",
                    HttpStatus.OK
            );
        }  catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        }catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving events",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<Object> getEventById(@PathVariable Integer eventId, HttpServletRequest request) {
        try {
            DetailEventResponseDto event = eventService.getEventById(eventId, request);
            return APIResponse.responseBuilder(
                    event,
                    "Event retrieved successfully",
                    HttpStatus.OK
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving the event",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/recommend")
    public ResponseEntity<Object> getRecommendEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String address,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            if(page<=0&&size<=0) {
                page = 1;
                size = 1;
            }
            Map<String, Object> response = eventService.getRecommendEvents(category, address, startTime, endTime, name, page, size);
            return APIResponse.responseBuilder(
                    response,
                    "Recommended events retrieved successfully",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving recommended events",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<Object> getPendingEvents(
            HttpServletRequest request,
            @RequestParam(required = false) String address,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            if(page<=0&&size<=0) {
                page = 1;
                size = 1;
            }
            Map<String, Object> response = eventService.getPendingEvents(request,address,startTime,endTime,name, page, size);
            return APIResponse.responseBuilder(
                    response,
                    "Pending events retrieved successfully",
                    HttpStatus.OK
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        }
        catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving pending events",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping()
        public ResponseEntity<Object> getListEvents(
                HttpServletRequest request,
                @RequestParam(required = false) String status,
                @RequestParam(required = false) String approvalStatus,
                @RequestParam(required = false) String address,
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                LocalDateTime startTime,
                @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                LocalDateTime endTime,
                @RequestParam(required = false) String name,
                @RequestParam(defaultValue = "1") Integer page,
                @RequestParam(defaultValue = "10") Integer size) {
        try {
            if(page<=0&&size<=0) {
                page = 1;
                size = 1;
            }
            Map<String, Object> response = eventService.getListEvents(request, status, approvalStatus, address, startTime, endTime, name, page, size);
            return APIResponse.responseBuilder(
                    response,
                    "Events retrieved successfully",
                    HttpStatus.OK
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred while retrieving events",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
