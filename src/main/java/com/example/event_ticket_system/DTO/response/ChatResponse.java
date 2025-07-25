package com.example.event_ticket_system.DTO.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatResponse {
    private String reply;
    public ChatResponse(String reply) {
        this.reply = reply;
    }
}
