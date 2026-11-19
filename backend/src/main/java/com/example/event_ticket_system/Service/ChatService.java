package com.example.event_ticket_system.Service;

public interface ChatService {
    /**
     * Processes a chat request and returns a response.
     *
     * @param message the message to process
     * @return a response containing the reply
     */
    String askGemini(String message);
}
