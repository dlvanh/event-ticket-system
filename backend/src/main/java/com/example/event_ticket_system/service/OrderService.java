package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.request.OrderRequestDto;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentLinkData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public interface OrderService {
    CheckoutResponseData createOrder(OrderRequestDto orderRequestDto);

    void confirmPayment(long payosOrderCode);

    PaymentLinkData cancelOrder(long payosOrderCode, String reason);

    PaymentLinkData getPaymentLinkInformation(long payosOrderCode);

    String confirmWebhook(String webhookUrl);

    Map<String, Object> getListOrders(String status,
                                      BigDecimal startAmount,
                                      BigDecimal endAmount,
                                      LocalDateTime startTime,
                                      LocalDateTime endTime,
                                      Integer page,
                                      Integer size);
}
