package com.example.event_ticket_system.dto.response;

import com.example.event_ticket_system.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class TicketResponseDTO {
    private Integer orderId;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Long payosOrderCode;
    private String cancellationReason;
    private List<TicketDetail> tickets;

    @Data
    @AllArgsConstructor
    public static class TicketDetail {
        private Integer ticketId;
        private Integer orderTicketId;
        private String eventName;
        private LocalDateTime eventStartTime;
        private LocalDateTime eventEndTime;
        private String eventCategory;
        private String eventLocation;
        private String ticketType;
        private BigDecimal unitPrice;
        private Integer quantity;
        private String checkinToken;
        private byte[] qrCode;
    }
}
