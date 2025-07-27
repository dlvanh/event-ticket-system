package com.example.event_ticket_system.DTO.response;

import com.example.event_ticket_system.Enums.OrderStatus;
import lombok.Data;

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
    private Double totalAmount;
    private String cancellationReason;
    private Integer totalTicketsCount;
}
