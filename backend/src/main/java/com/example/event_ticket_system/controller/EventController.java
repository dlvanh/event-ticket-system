package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.request.EventRequestDto;
import com.example.event_ticket_system.dto.request.UpdateEventRequestDto;
import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.dto.response.DailyTicketSalesDTO;
import com.example.event_ticket_system.dto.response.DailyTicketTypeSalesDTO;
import com.example.event_ticket_system.dto.response.DetailEventResponseDto;
import com.example.event_ticket_system.service.EventCommandService;
import com.example.event_ticket_system.service.EventQueryService;
import com.example.event_ticket_system.service.EventReportService;
import com.example.event_ticket_system.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventCommandService eventCommandService;
    private final EventQueryService eventQueryService;
    private final EventReportService eventReportService;
    private final TicketService ticketService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_organizer')")
    public ResponseEntity<ApiResponse<Integer>> createEvent(
            @RequestPart("data") @Valid EventRequestDto eventRequestDto,
            @RequestPart("logo") MultipartFile logoFile,
            @RequestPart("background") MultipartFile backgroundFile) {
        Integer eventId = eventCommandService.createEvent(eventRequestDto, logoFile, backgroundFile);
        return ApiResponse.created(eventId, "Event created successfully");
    }

    @GetMapping("/by-organizer")
    @PreAuthorize("hasAuthority('ROLE_organizer')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEventsByOrganizer(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approveStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.ok(
                eventQueryService.getEventsByOrganizer(status, approveStatus, startTime, endTime, name, page, size),
                "Events retrieved successfully"
        );
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<DetailEventResponseDto>> getEventById(@PathVariable Integer eventId) {
        return ApiResponse.ok(eventQueryService.getEventById(eventId), "Event retrieved successfully");
    }

    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendEvents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String sortBy) {
        return ApiResponse.ok(
                eventQueryService.getRecommendEvents(category, address, startTime, endTime, name, page, size, sortBy),
                "Recommended events retrieved successfully"
        );
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingEvents(
            @RequestParam(required = false) String address,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.ok(
                eventQueryService.getPendingEvents(address, startTime, endTime, name, page, size),
                "Pending events retrieved successfully"
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getListEvents(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ApiResponse.ok(
                eventQueryService.getListEvents(status, approvalStatus, address, startTime, endTime, name, page, size),
                "Events retrieved successfully"
        );
    }

    @PutMapping(value = "/{eventId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_organizer')")
    public ResponseEntity<ApiResponse<Void>> updateEvent(
            @PathVariable Integer eventId,
            @RequestPart("data") @Valid UpdateEventRequestDto eventRequestDto,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile,
            @RequestPart(value = "background", required = false) MultipartFile backgroundFile) {
        eventCommandService.updateEvent(eventId, eventRequestDto, logoFile, backgroundFile);
        return ApiResponse.ok(null, "Event updated successfully");
    }

    @GetMapping("/{eventId}/report/excel")
    @PreAuthorize("hasAnyAuthority('ROLE_admin', 'ROLE_organizer')")
    public ResponseEntity<byte[]> generateExcelReport(@PathVariable Integer eventId) {
        byte[] excelData = eventReportService.generateExcelReport(eventId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=event_report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    @GetMapping("/{eventId}/report/pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_admin', 'ROLE_organizer')")
    public ResponseEntity<byte[]> generatePdfReport(@PathVariable Integer eventId) {
        byte[] pdfData = eventReportService.generatePdfReport(eventId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=event_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @GetMapping("/{eventId}/report/buyer-excel")
    @PreAuthorize("hasAnyAuthority('ROLE_admin', 'ROLE_organizer')")
    public ResponseEntity<byte[]> generateBuyerReportExcel(@PathVariable Integer eventId) {
        byte[] excelData = eventReportService.generateBuyerReportExcel(eventId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=buyer_report.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelData);
    }

    @GetMapping("/{eventId}/report/buyer-pdf")
    @PreAuthorize("hasAnyAuthority('ROLE_admin', 'ROLE_organizer')")
    public ResponseEntity<byte[]> generateBuyerReportPdf(@PathVariable Integer eventId) {
        byte[] pdfData = eventReportService.generateBuyerReportPdf(eventId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=buyer_report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @GetMapping("/report/ticket-per-day/{eventId}")
    @PreAuthorize("hasAnyAuthority('ROLE_admin', 'ROLE_organizer')")
    public ResponseEntity<ApiResponse<List<DailyTicketSalesDTO>>> getTicketsSoldPerDay(@PathVariable Integer eventId) {
        List<DailyTicketSalesDTO> response = ticketService.getTicketsSoldPerDay(eventId);
        return ApiResponse.ok(response, "Ticket sales report retrieved successfully");
    }

    @GetMapping("/report/ticket-type-per-day/{eventId}")
    @PreAuthorize("hasAnyAuthority('ROLE_admin', 'ROLE_organizer')")
    public ResponseEntity<ApiResponse<List<DailyTicketTypeSalesDTO>>> getTicketsSoldPerTicketType(
            @PathVariable Integer eventId,
            @RequestParam(required = false) String ticketType) {
        List<DailyTicketTypeSalesDTO> response = ticketService.getTicketsSoldPerTicketType(eventId, ticketType);
        return ApiResponse.ok(response, "Ticket type sales report retrieved successfully");
    }
}
