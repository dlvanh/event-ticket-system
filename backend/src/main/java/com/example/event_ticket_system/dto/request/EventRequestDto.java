package com.example.event_ticket_system.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRequestDto {

    @NotBlank(message = "Event name cannot be blank")
    private String eventName;

    private String description;

    @NotNull(message = "Ward ID cannot be null")
    @Positive(message = "Ward ID must be a positive integer")
    private Integer wardId;

    private String addressDetail;
    private String addressName;

    @NotBlank(message = "Category cannot be blank")
    private String category;

    @NotNull(message = "Start time cannot be null")
    private LocalDateTime startTime;

    @NotNull(message = "End time cannot be null")
    private LocalDateTime endTime;

    @Valid
    @NotEmpty(message = "Tickets cannot be empty")
    private List<@Valid TicketRequest> tickets;

    @NotNull(message = "Sale start time cannot be null")
    private LocalDateTime saleStart;

    @NotNull(message = "Sale end time cannot be null")
    private LocalDateTime saleEnd;

    @Valid
    private List<@Valid DiscountRequest> discounts;

    @Data
    public static class TicketRequest {
        @NotBlank(message = "Ticket type cannot be blank")
        private String ticketType;

        @NotNull(message = "Quantity total cannot be null")
        @Positive(message = "Quantity total must be a positive integer")
        private Integer quantityTotal;

        @NotNull(message = "Price cannot be null")
        @DecimalMin(value = "0.0", message = "Price cannot be negative")
        private BigDecimal price;
    }

    @Data
    public static class DiscountRequest {
        @NotBlank(message = "Discount code cannot be blank")
        private String discountCode;

        private String discountDescription;

        @NotNull(message = "Discount type cannot be null")
        @Pattern(regexp = "percentage|fixed_amount", message = "Discount type must be 'percentage' or 'fixed_amount'")
        private String discountType;

        @NotNull(message = "Discount value cannot be null")
        @DecimalMin(value = "0.01", message = "Discount value must be positive")
        private BigDecimal discountValue;

        @NotNull(message = "Discount validFrom cannot be null")
        private LocalDate discountValidFrom;

        @NotNull(message = "Discount validTo cannot be null")
        private LocalDate discountValidTo;

        @NotNull(message = "Discount max uses cannot be null")
        @Positive(message = "Discount max uses must be a positive integer")
        private Integer discountMaxUses;
    }
}
