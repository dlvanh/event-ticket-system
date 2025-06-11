package com.example.event_ticket_system.DTO.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DeleteRequest {
    // Getter and Setter
    private List<Integer> ids;

}
