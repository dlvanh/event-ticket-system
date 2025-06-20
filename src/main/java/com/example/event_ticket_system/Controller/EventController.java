package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.EventRequestDto;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Service.EventService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
}
