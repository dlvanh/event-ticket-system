package com.example.event_ticket_system.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {
    @NotNull(message = "Event ID cannot be null")
    @Positive(message = "Event ID must be a positive integer")
    private Integer eventId;
    private List<OrderTicketRequestDto> tickets;
    private String discountCode;
    @NotBlank(message = "Return URL cannot be blank")
    private String returnUrl;
    @NotBlank(message = "Cancel URL cannot be blank")
    private String cancelUrl;

    @Data
    public static class OrderTicketRequestDto {
        @NotNull(message = "Ticket ID cannot be null")
        @Positive(message = "Ticket ID must be a positive integer")
        private Integer ticketId;
        @NotNull(message = "Unit price cannot be null")
        @Positive(message = "Unit price must be a positive number")
        private int quantity;
    }
}
