package com.example.event_ticket_system.DTO.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateEventRequestDto {
    private String eventName;
    private String description;
    private String addressName;
    private String addressDetail;
    private Integer wardId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String category;
    private List<TicketTypeDto> ticketTypes;
    private List<DiscountDto> discounts;

    @Data
    public static class TicketTypeDto {
        private String ticketType;
        private Integer quantityTotal;
        private Double price;
    }

    @Data
    public static class DiscountDto {
        private String code;
        private String description;
        private String type; // "percentage" or "fixed_amount"
        private Double value;
        private LocalDate validFrom;
        private LocalDate validTo;
        private Integer maxUses;
    }
}
