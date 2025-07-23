package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import com.example.event_ticket_system.DTO.request.cancellationReasonBody;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Entity.Ticket;
import com.example.event_ticket_system.Entity.User;
import com.example.event_ticket_system.Enums.OrderStatus;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Repository.OrderRepository;
import com.example.event_ticket_system.Repository.OrderTicketRepository;
import com.example.event_ticket_system.Repository.TicketRepository;
import com.example.event_ticket_system.Repository.UserRepository;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentLinkData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final PayOS payOS;
    private final OrderRepository orderRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<Object> createOrder(@RequestBody @Valid OrderRequestDto orderRequestDto,
                                              BindingResult bindingResult,
                                              HttpServletRequest request) {
        try {
            // Validate input
            Map<String, String> errors = new HashMap<>();
            if (bindingResult.hasErrors()) {
                bindingResult.getFieldErrors().forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );
            }
            if (!errors.isEmpty()) {
                return APIResponse.responseBuilder(
                        errors,
                        "Validation failed",
                        HttpStatus.BAD_REQUEST
                );
            }

            // Create order and payment link
            CheckoutResponseData checkoutData = orderService.createOrder(orderRequestDto, request);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("orderId", checkoutData.getOrderCode());
            responseData.put("checkoutUrl", checkoutData.getCheckoutUrl());
            responseData.put("paymentLinkId", checkoutData.getPaymentLinkId());

            return APIResponse.responseBuilder(
                    responseData,
                    "Order and payment link successfully created",
                    HttpStatus.CREATED
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Object> getOrderById(@PathVariable("orderId") long orderId, HttpServletRequest request) {
        try {
            Integer adminId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
            User currentUser = userRepository.findById(adminId)
                    .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + adminId));

            if (!UserRole.admin.equals(currentUser.getRole())) {
                throw new SecurityException("You do not have permission to use this endpoint.");
            }
            PaymentLinkData order = payOS.getPaymentLinkInformation(orderId);
            return APIResponse.responseBuilder(
                    order,
                    "Order retrieved successfully",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PutMapping("/{orderId}")
    @Transactional
    public ResponseEntity<Object> cancelOrder(@PathVariable("orderId") int orderId,
                                              @RequestBody cancellationReasonBody cancellationReasonBody,
                                              HttpServletRequest request) {
        try {
            Integer adminId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
            User currentUser = userRepository.findById(adminId)
                    .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + adminId));

            if (!UserRole.admin.equals(currentUser.getRole())) {
                throw new SecurityException("You do not have permission to use this endpoint.");
            }
            // Cancel PayOS payment link
            PaymentLinkData payosOrder = payOS.cancelPaymentLink(orderId, cancellationReasonBody.getCancellationReason());

            // Update order status
            Order order = orderRepository.findByPayosOrderCode((long) orderId);
            if (order == null) {
                throw new EntityNotFoundException("Order not found for PayOS order code: " + orderId);
            }
            order.setStatus(OrderStatus.cancelled);
            order.setCancellationReason(cancellationReasonBody.getCancellationReason());
            orderRepository.save(order);

            // Restore ticket quantities
            List<OrderTicket> orderTickets = orderTicketRepository.findByOrder(order);
            for (OrderTicket orderTicket : orderTickets) {
                Ticket ticket = orderTicket.getTicket();
                ticket.setQuantitySold(ticket.getQuantitySold() - orderTicket.getQuantity());
                ticketRepository.save(ticket);
            }

            return APIResponse.responseBuilder(
                    payosOrder,
                    "Order cancelled successfully",
                    HttpStatus.OK
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/confirm-webhook")
    public ResponseEntity<Object> confirmWebhook(@RequestBody Map<String, String> requestBody, HttpServletRequest request) {
        try {
            Integer adminId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
            User currentUser = userRepository.findById(adminId)
                    .orElseThrow(() -> new EntityNotFoundException("Organizer not found with id: " + adminId));

            if (!UserRole.admin.equals(currentUser.getRole())) {
                throw new SecurityException("You do not have permission to use this endpoint.");
            }
            String webhookUrl = payOS.confirmWebhook(requestBody.get("webhookUrl"));
            return APIResponse.responseBuilder(
                    webhookUrl,
                    "Webhook confirmed successfully",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/confirm-order/{orderCode}")
    public ResponseEntity<Object> confirmPayment(@PathVariable("orderCode") String payosOrderCode,
                                                 HttpServletRequest request) {
        try {
            orderService.confirmPayment(payosOrderCode, request);
            return APIResponse.responseBuilder(
                    null,
                    "Payment confirmed successfully",
                    HttpStatus.OK
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @PostMapping("/cancel-order/{orderCode}")
    public ResponseEntity<Object> cancelOrder(@PathVariable("orderCode") String orderCode,
                                              HttpServletRequest request) {
        try {
            orderService.cancelOrder(orderCode, request);
            return APIResponse.responseBuilder(
                    null,
                    "Order cancelled successfully",
                    HttpStatus.OK
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
