package com.example.event_ticket_system.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderPaidEvent(
        Integer orderId,
        String recipientEmail,
        List<TicketEmailItem> items
) {
    public record TicketEmailItem(
            Integer orderTicketId,
            String eventName,
            String ticketType,
            int quantity,
            BigDecimal unitPrice,
            byte[] qrCode
    ) {
    }
}
