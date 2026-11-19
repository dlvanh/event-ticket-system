package com.example.event_ticket_system.dto.response;

import com.example.event_ticket_system.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class GetOrdersResponseDto {
    private Integer orderId;
    private Integer userId;
    private LocalDateTime orderDate;
    private String userName;
    private String userEmail;
    private Long orderPayOSCode;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String cancellationReason;
    private Integer totalTicketsCount;
}
