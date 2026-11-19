package com.example.event_ticket_system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    @Valid
    @NotEmpty(message = "Order must contain at least one ticket")
    private List<OrderTicketRequestDto> tickets;

    private String discountCode;

    @NotBlank(message = "Return URL cannot be blank")
    private String returnUrl;

    @NotBlank(message = "Cancel URL cannot be blank")
    private String cancelUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderTicketRequestDto {
        @NotNull(message = "Ticket ID cannot be null")
        @Positive(message = "Ticket ID must be a positive integer")
        private Integer ticketId;

        @Positive(message = "Quantity must be between 1 and 5")
        @Max(value = 5, message = "Quantity must be between 1 and 5")
        private int quantity;
    }
}
