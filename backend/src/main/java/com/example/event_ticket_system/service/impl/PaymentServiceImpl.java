package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.entity.Order;
import com.example.event_ticket_system.entity.OrderTicket;
import com.example.event_ticket_system.entity.Payment;
import com.example.event_ticket_system.enums.OrderStatus;
import com.example.event_ticket_system.enums.PaymentStatus;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.event.OrderPaidEvent;
import com.example.event_ticket_system.repository.OrderRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.repository.PaymentRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import com.example.event_ticket_system.service.PaymentService;
import com.example.event_ticket_system.util.QRCodeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PayOS payOS;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final PaymentRepository paymentRepository;
    private final QRCodeUtil qrCodeUtil;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ObjectNode handlePayOsWebhook(ObjectNode body) {
        ObjectNode response = objectMapper.createObjectNode();
        try {
            Webhook webhookBody = objectMapper.treeToValue(body, Webhook.class);
            WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);
            if (!Boolean.TRUE.equals(webhookBody.getSuccess())) {
                log.warn("PayOS webhook reported unsuccessful payment for orderCode={}", data.getOrderCode());
                response.put("error", 0);
                response.put("message", "Payment not successful");
                response.set("data", null);
                return response;
            }

            Long orderCode = data.getOrderCode();
            Order order = orderRepository.findByPayosOrderCodeForUpdate(orderCode)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found for PayOS order code: " + orderCode));

            // Idempotency guard: duplicate PayOS webhook deliveries must be a no-op
            if (order.getStatus() == OrderStatus.paid) {
                log.info("Order {} (payosCode={}) is already paid; ignoring duplicate webhook.", order.getOrderId(), orderCode);
                response.put("error", 0);
                response.put("message", "Webhook already processed");
                response.set("data", null);
                return response;
            }

            int expectedAmount = order.getTotalAmount().setScale(0, java.math.RoundingMode.HALF_UP).intValueExact();
            if (data.getAmount() == null || data.getAmount() != expectedAmount) {
                log.error("PayOS webhook amount mismatch for order {} (payosCode={}): expected={}, actual={}",
                        order.getOrderId(), orderCode, expectedAmount, data.getAmount());
                response.put("error", -1);
                response.put("message", "Amount mismatch");
                response.set("data", null);
                return response;
            }

            if (order.getStatus() != OrderStatus.pending) {
                log.error("Received PayOS webhook for non-pending order {} (status={}, payosCode={}, ref={}); recording for reconciliation.",
                        order.getOrderId(), order.getStatus(), orderCode, data.getReference());
                if (!paymentRepository.existsByOrderIdAndPaymentStatus(order.getOrderId(), PaymentStatus.failed)) {
                    Payment reconciliationPayment = new Payment();
                    reconciliationPayment.setOrderId(order.getOrderId());
                    reconciliationPayment.setPaymentMethod("PayOS-LateWebhook");
                    reconciliationPayment.setPaymentDate(LocalDateTime.now());
                    reconciliationPayment.setAmountPaid(order.getTotalAmount());
                    reconciliationPayment.setPaymentStatus(PaymentStatus.failed);
                    reconciliationPayment.setTransactionReference(data.getReference());
                    paymentRepository.save(reconciliationPayment);
                }
                response.put("error", 0);
                response.put("message", "Order is not pending; recorded for reconciliation");
                response.set("data", null);
                return response;
            }

            finalizePaidOrder(order, "PayOS", data.getReference());

            response.put("error", 0);
            response.put("message", "Webhook delivered");
            response.set("data", null);
            return response;
        } catch (Exception e) {
            log.error("Failed to process PayOS webhook: {}", e.getMessage(), e);
            response.put("error", -1);
            response.put("message", e.getMessage());
            response.set("data", null);
            return response;
        }
    }

    @Override
    @Transactional
    public void completeOrderPayment(Long payosOrderCode, String paymentMethod, String transactionRef) {
        Order order = orderRepository.findByPayosOrderCodeForUpdate(payosOrderCode)
                .orElseThrow(() -> new EntityNotFoundException("Order not found for PayOS order code: " + payosOrderCode));

        if (order.getStatus() == OrderStatus.paid) {
            throw new IllegalArgumentException("Order is already paid");
        }
        if (order.getStatus() != OrderStatus.pending) {
            throw new IllegalStateException("Only pending orders can be confirmed (current status: " + order.getStatus() + ")");
        }

        finalizePaidOrder(order, paymentMethod, transactionRef);
    }

    @Override
    @Transactional
    public boolean verifyCheckinToken(String token) {
        if (!qrCodeUtil.verifySignedToken(token)) {
            return false;
        }
        OrderTicket ot = orderTicketRepository.findByCheckinTokenWithDetails(token).orElse(null);
        if (ot == null || ot.getOrder().getStatus() != OrderStatus.paid) {
            return false;
        }
        UserRole role = SecurityUtils.getCurrentUserRole();
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (role != UserRole.admin && !ot.getTicket().getEvent().getOrganizer().getId().equals(currentUserId)) {
            throw new SecurityException("You do not have permission to check in tickets for this event.");
        }
        return orderTicketRepository.markCheckedIn(token, LocalDateTime.now()) == 1;
    }

    private void finalizePaidOrder(Order order, String paymentMethod, String transactionRef) {
        order.setStatus(OrderStatus.paid);
        orderRepository.save(order);

        if (!paymentRepository.existsByOrderIdAndPaymentStatus(order.getOrderId(), PaymentStatus.success)) {
            Payment payment = new Payment();
            payment.setOrderId(order.getOrderId());
            payment.setPaymentMethod(paymentMethod);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setAmountPaid(order.getTotalAmount());
            payment.setPaymentStatus(PaymentStatus.success);
            payment.setTransactionReference(transactionRef);
            paymentRepository.save(payment);
        }

        List<OrderTicket> orderTickets = orderTicketRepository.findByOrderWithTicketAndEvent(order);
        List<OrderPaidEvent.TicketEmailItem> emailItems = new ArrayList<>(orderTickets.size());

        for (OrderTicket orderTicket : orderTickets) {
            String token = qrCodeUtil.generateSignedToken(
                    order.getOrderId(),
                    orderTicket.getOrderTicketId(),
                    orderTicket.getTicket().getTicketId(),
                    order.getUser().getId(),
                    orderTicket.getTicket().getEvent().getEventId(),
                    orderTicket.getQuantity()
            );
            try {
                byte[] qrPng = qrCodeUtil.generateQRCodeImage(token);
                orderTicket.setCheckinToken(token);
                orderTicket.setQrCode(qrPng);
                orderTicketRepository.save(orderTicket);

                emailItems.add(new OrderPaidEvent.TicketEmailItem(
                        orderTicket.getOrderTicketId(),
                        orderTicket.getTicket().getEvent().getEventName(),
                        orderTicket.getTicket().getTicketType(),
                        orderTicket.getQuantity(),
                        orderTicket.getUnitPrice(),
                        qrPng
                ));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate QR code for orderTicket " + orderTicket.getOrderTicketId(), e);
            }
        }

        // Dispatched asynchronously ONLY after this DB transaction commits
        if (!emailItems.isEmpty() && order.getUser() != null && order.getUser().getEmail() != null) {
            eventPublisher.publishEvent(new OrderPaidEvent(
                    order.getOrderId(),
                    order.getUser().getEmail(),
                    order.getTotalAmount(),
                    emailItems
            ));
        }
    }
}
