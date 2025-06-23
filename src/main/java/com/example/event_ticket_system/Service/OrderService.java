package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import jakarta.servlet.http.HttpServletRequest;

public interface OrderService {
    Integer createOrder(OrderRequestDto orderRequestDto, HttpServletRequest request);
}
