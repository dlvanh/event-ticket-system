package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.ChatRequest;
import com.example.event_ticket_system.DTO.response.ChatResponse;
import com.example.event_ticket_system.Service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String userMessage = request.getMessage();
        String reply = chatService.askGemini(userMessage);
        return ResponseEntity.ok(new ChatResponse(reply));
    }
}

