package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Repository.OrderTicketRepository;
import com.example.event_ticket_system.Service.EmailService;
import com.example.event_ticket_system.Util.QRCodeUtil;
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

import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PayOS payOS;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final EmailService emailService;

    public PaymentController(PayOS payOS, OrderRepository orderRepository,
                             OrderTicketRepository orderTicketRepository, EmailService emailService) {
        this.payOS = payOS;
        this.orderRepository = orderRepository;
        this.orderTicketRepository = orderTicketRepository;
        this.emailService = emailService;
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

            // Cập nhật trạng thái order
            order.setStatus(OrderStatus.paid);
            orderRepository.save(order);

            // Lấy danh sách OrderTickets của order
            List<OrderTicket> orderTickets = orderTicketRepository.findByOrder(order);
            List<byte[]> qrCodes = new ArrayList<>();

            System.out.println("Tạo QR codes cho " + orderTickets.size() + " loại vé");

            // Tạo QR code riêng cho mỗi OrderTicket
            for (OrderTicket orderTicket : orderTickets) {
                String qrContent = String.format(
                        """
                            Order ID: %d
                            Order Ticket ID: %d
                            Ticket ID: %d
                            User ID: %d
                            Event ID: %d
                            Event Name: %s
                            Ticket Type: %s
                            Quantity: %d
                            Price: %.2f
                        """,
                        order.getOrderId(),
                        orderTicket.getOrderTicketId(),
                        orderTicket.getTicket().getTicketId(),
                        order.getUser().getId(),
                        orderTicket.getTicket().getEvent().getEventId(),
                        orderTicket.getTicket().getEvent().getEventName(),
                        orderTicket.getTicket().getTicketType(),
                        orderTicket.getQuantity(),
                        orderTicket.getTicket().getPrice()
                );

                // Sử dụng QR code siêu nhỏ (dưới 5KB)
                byte[] qrCode = QRCodeUtil.generateMiniQRCode(qrContent);
                System.out.println("QR code size: " + qrCode.length + " bytes");

                orderTicket.setQrCode(qrCode);
                qrCodes.add(qrCode);

                // Lưu OrderTicket với QR code
                orderTicketRepository.save(orderTicket);
            }

            // Gửi email với tất cả QR codes
            try {
                String emailText = "Cảm ơn bạn đã mua vé! Vui lòng xuất trình các mã QR này tại lối vào sự kiện.";
                emailService.sendMultipleQRCodeEmail(
                        order.getUser().getEmail(),
                        "Mã QR Vé Sự Kiện Của Bạn",
                        emailText,
                        qrCodes,
                        orderTickets
                );
                System.out.println("Email đã được gửi thành công đến: " + order.getUser().getEmail());
            } catch (Exception emailException) {
                System.err.println("Lỗi gửi email: " + emailException.getMessage());
                emailException.printStackTrace();
            }

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