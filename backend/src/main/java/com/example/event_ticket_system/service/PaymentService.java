package com.example.event_ticket_system.service;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface PaymentService {
    ObjectNode handlePayOsWebhook(ObjectNode body);
    void completeOrderPayment(Long payosOrderCode, String paymentMethod, String transactionRef);
    boolean verifyCheckinToken(String token);
}
