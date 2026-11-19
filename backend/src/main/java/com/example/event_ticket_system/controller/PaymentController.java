package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.service.PaymentService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping(path = "/payos_transfer_handler")
    public ObjectNode payosTransferHandler(@RequestBody ObjectNode body) {
        return paymentService.handlePayOsWebhook(body);
    }

    @PostMapping(path = "/verify-checkin")
    @PreAuthorize("hasAnyAuthority('ROLE_organizer', 'ROLE_admin')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> verifyCheckinToken(@RequestBody Map<String, String> payload) {
        boolean valid = paymentService.verifyCheckinToken(payload.get("token"));
        return ApiResponse.ok(Map.of("valid", valid), valid ? "Valid check-in ticket" : "Invalid or unpaid ticket token");
    }
}