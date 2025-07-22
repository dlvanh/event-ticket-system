package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import vn.payos.type.CheckoutResponseData;

public interface OrderService {
    CheckoutResponseData createOrder(OrderRequestDto orderRequestDto, HttpServletRequest request);
    void confirmPayment(String payosOrderCode, HttpServletRequest request);
    void cancelOrder(String orderCode, HttpServletRequest request);
}
