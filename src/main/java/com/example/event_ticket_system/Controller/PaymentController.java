package com.example.event_ticket_system.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Enums.OrderStatus;
import com.example.event_ticket_system.Repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;


@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PayOS payOS;
    private final OrderRepository orderRepository;

    public PaymentController(PayOS payOS, OrderRepository orderRepository) {
        this.payOS = payOS;
        this.orderRepository = orderRepository;
    }

    @PostMapping(path = "/payos_transfer_handler")
    @Transactional
    public ObjectNode payosTransferHandler(@RequestBody ObjectNode body) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode response = objectMapper.createObjectNode();
        Webhook webhookBody = objectMapper.treeToValue(body, Webhook.class);

        try {
            WebhookData data = payOS.verifyPaymentWebhookData(webhookBody);
            Order order = orderRepository.findByPayosOrderCode(data.getOrderCode());
            if (order == null) {
                throw new IllegalArgumentException("Order not found for PayOS order code: " + data.getOrderCode());
            }
            // Update order status
//            order.setStatus(data.getDesc().equals("PAID") ?
//                    OrderStatus.paid :
//                    OrderStatus.cancelled);
            order.setStatus(OrderStatus.paid);
            orderRepository.save(order);

            // Restore ticket quantities if payment failed
//            if (!data.getCode().equals("PAID")) {
//                List<OrderTicket> orderTickets = orderTicketRepository.findByOrder(order);
//                for (OrderTicket orderTicket : orderTickets) {
//                    Ticket ticket = orderTicket.getTicket();
//                    ticket.setQuantitySold(ticket.getQuantitySold() - orderTicket.getQuantity());
//                    ticketRepository.save(ticket);
//                }
//            }

            response.put("error", 0);
            response.put("message", "Webhook delivered");
            response.set("data", null);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            response.put("error", -1);
            response.put("message", e.getMessage());
            response.set("data", null);
            return response;
        }
    }
}