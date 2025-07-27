package com.example.event_ticket_system.DTO.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.annotations.Formula;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRequestDto {
    @NotNull(message = "Event name cannot be null")
    @NotBlank(message = "Event name cannot be blank")
    private String eventName;
    private String description;
    @NotNull(message = "Ward ID cannot be null")
    @Positive(message = "Ward ID must be a positive integer")
    // liên kết đến bảng Ward
    private Integer wardId;
    // số nhà, tên đường
    private String addressDetail;
    // tên địa điểm tổ chức
    private String addressName;
    private String category;
    @NotNull(message = "Start time cannot be null")
    private LocalDateTime startTime;
    @NotNull(message = "End time cannot be null")
    private LocalDateTime endTime;

    @Valid
    @NotNull(message = "Tickets cannot be null")
    private List<@Valid TicketRequest> tickets;
    @NotNull(message = "Sale start time cannot be null")
    private LocalDateTime saleStart;
    @NotNull(message = "Sale end time cannot be null")
    private LocalDateTime saleEnd;

    @Valid
    private List<@Valid DiscountRequest> discounts;

    @Data
    public static class TicketRequest {
        @NotNull(message = "Ticket type cannot be null")
        private String ticketType;
        @NotNull(message = "Quantity total cannot be null")
        @Positive(message = "Quantity total must be a positive integer")
        private Integer quantityTotal;
        @NotNull(message="price cannot be null")
        private Double price;
    }

    @Data
    public static class DiscountRequest {
        @NotNull(message = "Discount code cannot be null")
        @NotBlank(message = "Discount code cannot be blank")
        private String discountCode;

        private String discountDescription;

        @NotNull(message = "Discount type cannot be null")
        @Pattern(regexp = "percentage|fixed_amount", message = "Discount type must be 'percentage' or 'fixed_amount'")
        private String discountType;

        @NotNull(message = "Discount value cannot be null")
        @Positive(message = "Discount value must be positive")
        private Double discountValue;

        @NotNull(message = "Discount validFrom cannot be null")
        private LocalDate discountValidFrom;

        @NotNull(message = "Discount validTo cannot be null")
        private LocalDate discountValidTo;

        @NotNull(message = "Discount max uses cannot be null")
        @Positive(message = "Discount max uses must be a positive integer")
        private Integer discountMaxUses;
    }
}
