package com.example.event_ticket_system.DTO.request;

import com.example.event_ticket_system.Enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class TicketResponseDTO {
    private Integer orderId;
    private LocalDateTime orderDate;
    private Double totalAmount;
    private OrderStatus status;
    private Long payosOrderCode;
    private String cancellationReason;
    private List<TicketDetail> tickets;

    @Data
    @AllArgsConstructor
    public static class TicketDetail {
        private Integer ticketId;
        private String eventName;
        private LocalDateTime eventStartTime;
        private LocalDateTime eventEndTime;
        private String eventCategory;
        private String eventLocation;
        private String ticketType;
        private Double unitPrice;
        private Integer quantity;
    }
}
