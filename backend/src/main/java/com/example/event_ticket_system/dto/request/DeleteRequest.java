package com.example.event_ticket_system.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DeleteRequest {
    // Getter and Setter
    private List<Integer> ids;

}
