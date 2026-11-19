package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.request.ChatRequest;
import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.dto.response.ChatResponse;
import com.example.event_ticket_system.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        String reply = chatService.askGemini(request.getMessage());
        return ApiResponse.ok(new ChatResponse(reply), "Chat reply generated successfully");
    }
}
