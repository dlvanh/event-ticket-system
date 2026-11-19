package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.request.CancellationReasonRequest;
import com.example.event_ticket_system.dto.request.ConfirmWebhookRequestBody;
import com.example.event_ticket_system.dto.request.OrderRequestDto;
import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.PaymentLinkData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_customer')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(@Valid @RequestBody OrderRequestDto orderRequestDto) {
        CheckoutResponseData checkoutData = orderService.createOrder(orderRequestDto);
        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("orderId", checkoutData.getOrderCode());
        responseData.put("checkoutUrl", checkoutData.getCheckoutUrl());
        responseData.put("paymentLinkId", checkoutData.getPaymentLinkId());
        return ApiResponse.created(responseData, "Order and payment link successfully created");
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startAmount", required = false) BigDecimal startAmount,
            @RequestParam(value = "endAmount", required = false) BigDecimal endAmount,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "startTime", required = false) LocalDateTime startTime,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "endTime", required = false) LocalDateTime endTime,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        Map<String, Object> ordersData = orderService.getListOrders(status, startAmount, endAmount, startTime, endTime, page, size);
        return ApiResponse.ok(ordersData, "Orders retrieved successfully");
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<PaymentLinkData>> getOrderById(@PathVariable("orderId") long orderId) {
        PaymentLinkData order = orderService.getPaymentLinkInformation(orderId);
        return ApiResponse.ok(order, "Order retrieved successfully");
    }

    @PutMapping("/{orderCode}")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<PaymentLinkData>> cancelOrder(
            @PathVariable("orderCode") long orderCode,
            @RequestBody(required = false) CancellationReasonRequest request) {
        String reason = request != null ? request.getCancellationReason() : null;
        PaymentLinkData payosOrder = orderService.cancelOrder(orderCode, reason);
        return ApiResponse.ok(payosOrder, "Order cancelled successfully");
    }

    @PostMapping("/confirm-webhook")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<String>> confirmWebhook(@RequestBody ConfirmWebhookRequestBody requestBody) {
        String webhookUrl = orderService.confirmWebhook(requestBody.getWebhookUrl());
        return ApiResponse.ok(webhookUrl, "Webhook confirmed successfully");
    }

    @PostMapping("/confirm-order/{orderCode}")
    @PreAuthorize("hasAuthority('ROLE_admin')")
    public ResponseEntity<ApiResponse<Void>> confirmPayment(@PathVariable("orderCode") long payosOrderCode) {
        orderService.confirmPayment(payosOrderCode);
        return ApiResponse.ok(null, "Payment confirmed successfully");
    }
}
