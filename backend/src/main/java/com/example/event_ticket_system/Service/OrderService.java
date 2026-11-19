package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import vn.payos.type.CheckoutResponseData;

import java.time.LocalDateTime;
import java.util.Map;

public interface OrderService {
    CheckoutResponseData createOrder(OrderRequestDto orderRequestDto, HttpServletRequest request);
    void confirmPayment(String payosOrderCode, HttpServletRequest request);
    void cancelOrder(String orderCode, HttpServletRequest request);
    Map<String, Object> getListOrders(HttpServletRequest request, String status, Double startAmount, Double endAmount, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer size);
}
