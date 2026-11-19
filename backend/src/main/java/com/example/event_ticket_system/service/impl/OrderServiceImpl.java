package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.dto.request.OrderRequestDto;
import com.example.event_ticket_system.dto.response.GetOrdersResponseDto;
import com.example.event_ticket_system.entity.Discount;
import com.example.event_ticket_system.entity.Order;
import com.example.event_ticket_system.entity.OrderTicket;
import com.example.event_ticket_system.entity.Ticket;
import com.example.event_ticket_system.entity.User;
import com.example.event_ticket_system.enums.ApprovalStatus;
import com.example.event_ticket_system.enums.DiscountType;
import com.example.event_ticket_system.enums.EventStatus;
import com.example.event_ticket_system.enums.OrderStatus;
import com.example.event_ticket_system.enums.UserRole;
import com.example.event_ticket_system.exception.ApiException;
import com.example.event_ticket_system.repository.DiscountRepository;
import com.example.event_ticket_system.repository.OrderRepository;
import com.example.event_ticket_system.repository.OrderTicketRepository;
import com.example.event_ticket_system.repository.TicketRepository;
import com.example.event_ticket_system.repository.UserRepository;
import com.example.event_ticket_system.security.SecurityUtils;
import com.example.event_ticket_system.service.OrderService;
import com.example.event_ticket_system.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final long PAYOS_CODE_BASE = 100_000_000L;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final DiscountRepository discountRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final PayOS payOS;
    private final PaymentService paymentService;
    private final PlatformTransactionManager transactionManager;

    private record PendingOrderContext(
            Integer orderId,
            long payosOrderCode,
            String eventName,
            BigDecimal totalAmount,
            int totalQuantity
    ) {
    }

    @Override
    public CheckoutResponseData createOrder(OrderRequestDto dto) {
        Integer userId = SecurityUtils.getCurrentUserId();
        if (!SecurityUtils.hasRole(UserRole.customer)) {
            throw new SecurityException("Only customers can create orders");
        }
        if (dto.getTickets() == null || dto.getTickets().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one ticket");
        }

        // Aggregate duplicate ticketIds and sort ascending to prevent deadlocks
        TreeMap<Integer, Integer> sortedQuantities = new TreeMap<>();
        for (OrderRequestDto.OrderTicketRequestDto ticketDto : dto.getTickets()) {
            sortedQuantities.merge(ticketDto.getTicketId(), ticketDto.getQuantity(), Integer::sum);
        }

        // TX1 (short): Reserve inventory atomically & persist pending Order
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        PendingOrderContext ctx = txTemplate.execute(status -> reserveTicketsAndCreateOrder(userId, dto, sortedQuantities));
        if (ctx == null) {
            throw new IllegalStateException("Failed to create pending order");
        }

        // Free / 100%-discounted orders skip PayOS and complete immediately
        if (ctx.totalAmount().compareTo(BigDecimal.ZERO) == 0) {
            paymentService.completeOrderPayment(ctx.payosOrderCode(), "Free/Discount100", null);
            return CheckoutResponseData.builder()
                    .orderCode(ctx.payosOrderCode())
                    .amount(0)
                    .status("PAID")
                    .build();
        }

        // External HTTP call to PayOS OUTSIDE database transaction
        int payosAmount = ctx.totalAmount().setScale(0, RoundingMode.HALF_UP).intValueExact();
        ItemData itemData = ItemData.builder()
                .name("Tickets for " + ctx.eventName())
                .price(payosAmount)
                .quantity(ctx.totalQuantity())
                .build();
        PaymentData paymentData = PaymentData.builder()
                .orderCode(ctx.payosOrderCode())
                .description("Thanh toán Eventa")
                .amount(payosAmount)
                .item(itemData)
                .returnUrl(dto.getReturnUrl())
                .cancelUrl(dto.getCancelUrl())
                .build();

        try {
            return payOS.createPaymentLink(paymentData);
        } catch (Exception e) {
            log.error("PayOS createPaymentLink failed for order {}: {}. Executing compensating rollback.",
                    ctx.orderId(), e.getMessage());
            compensateFailedOrder(ctx.orderId(), "PayOS payment link creation failed");
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to create PayOS payment link: " + e.getMessage(), e);
        }
    }

    private PendingOrderContext reserveTicketsAndCreateOrder(Integer userId,
                                                             OrderRequestDto dto,
                                                             TreeMap<Integer, Integer> sortedQuantities) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        String eventName = "Event Tickets";
        List<OrderTicket> orderTickets = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Integer, Integer> entry : sortedQuantities.entrySet()) {
            Integer ticketId = entry.getKey();
            int qty = entry.getValue();

            Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                    .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

            if (qty <= 0 || qty > 5) {
                throw new IllegalArgumentException("Quantity must be between 1 and 5 for ticket: " + ticket.getTicketType());
            }
            if (!ticket.getEvent().getEventId().equals(dto.getEventId())) {
                throw new IllegalArgumentException("Ticket " + ticket.getTicketType() + " does not belong to event");
            }
            if (ticket.getEvent().getApprovalStatus() != ApprovalStatus.approved
                    || ticket.getEvent().getStatus() == EventStatus.cancelled) {
                throw new IllegalStateException("Event is not available for ticket booking");
            }
            if (ticket.getSaleStart() != null && now.isBefore(ticket.getSaleStart())) {
                throw new IllegalArgumentException("Ticket sale for " + ticket.getTicketType() + " has not started");
            }
            if (ticket.getSaleEnd() != null && now.isAfter(ticket.getSaleEnd())) {
                throw new IllegalArgumentException("Ticket sale for " + ticket.getTicketType() + " has ended");
            }

            // Atomic conditional update guarantees zero overselling under high concurrency
            int updated = ticketRepository.reserveTickets(ticketId, qty);
            if (updated == 0) {
                throw new IllegalStateException("Not enough tickets available for: " + ticket.getTicketType());
            }

            eventName = ticket.getEvent().getEventName();
            BigDecimal lineAmount = ticket.getPrice().multiply(BigDecimal.valueOf(qty));
            totalAmount = totalAmount.add(lineAmount);
            totalQuantity += qty;

            OrderTicket orderTicket = new OrderTicket();
            orderTicket.setTicket(ticket);
            orderTicket.setQuantity(qty);
            orderTicket.setUnitPrice(ticket.getPrice());
            orderTickets.add(orderTicket);
        }

        Discount appliedDiscount = null;
        if (dto.getDiscountCode() != null && !dto.getDiscountCode().isBlank()) {
            Discount discount = discountRepository.findByCodeWithEvent(dto.getDiscountCode().trim())
                    .orElseThrow(() -> new EntityNotFoundException("Discount code not found"));

            LocalDate today = LocalDate.now();
            if ((discount.getValidFrom() != null && today.isBefore(discount.getValidFrom())) ||
                    (discount.getValidTo() != null && today.isAfter(discount.getValidTo()))) {
                throw new IllegalArgumentException("Discount code is not valid");
            }
            if (!discount.getEvent().getEventId().equals(dto.getEventId())) {
                throw new IllegalArgumentException("Discount code does not apply to this event");
            }

            int updated = discountRepository.decrementMaxUsage(discount.getDiscountId());
            if (updated == 0) {
                throw new IllegalStateException("Discount code has reached its maximum usage limit");
            }

            if (discount.getDiscountType() == DiscountType.percentage) {
                BigDecimal multiplier = BigDecimal.ONE.subtract(
                        discount.getValue().divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP));
                totalAmount = totalAmount.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
            } else if (discount.getDiscountType() == DiscountType.fixed_amount) {
                totalAmount = totalAmount.subtract(discount.getValue());
            }
            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                totalAmount = BigDecimal.ZERO;
            }
            appliedDiscount = discount;
        }

        Order order = new Order();
        order.setUser(currentUser);
        order.setDiscount(appliedDiscount);
        order.setOrderDate(now);
        order.setStatus(OrderStatus.pending);
        order.setTotalAmount(totalAmount);
        order = orderRepository.saveAndFlush(order);

        // Collision-safe positive PayOS order code derived from unique DB primary key
        long payosOrderCode = generateUniquePayosOrderCode(order.getOrderId());
        order.setPayosOrderCode(payosOrderCode);
        order = orderRepository.save(order);

        for (OrderTicket ot : orderTickets) {
            ot.setOrder(order);
            orderTicketRepository.save(ot);
        }

        return new PendingOrderContext(order.getOrderId(), payosOrderCode, eventName, totalAmount, totalQuantity);
    }

    private void compensateFailedOrder(Integer orderId, String reason) {
        TransactionTemplate requiresNewTx = new TransactionTemplate(transactionManager);
        requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNewTx.executeWithoutResult(status -> {
            orderRepository.findByIdForUpdate(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.pending) {
                    cancelPendingOrderAndRestoreInventory(order, reason);
                }
            });
        });
    }

    private long generateUniquePayosOrderCode(Integer orderId) {
        int suffix = ThreadLocalRandom.current().nextInt(1_000);
        return PAYOS_CODE_BASE + (orderId.longValue() * 1_000L) + suffix;
    }

    @Override
    public void confirmPayment(long payosOrderCode) {
        requireAdmin("Only admins can confirm payments");
        paymentService.completeOrderPayment(payosOrderCode, "AdminManual", null);
    }

    @Override
    public PaymentLinkData cancelOrder(long payosOrderCode, String reason) {
        requireAdmin("Only admins can cancel orders");
        String effectiveReason = (reason == null || reason.isBlank()) ? "Cancelled by administrator" : reason.trim();

        // Validate & cancel inside DB transaction first? Wait: check order state before calling PayOS!
        // First do atomic DB state transition (locks order & verifies pending), then notify PayOS.
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.executeWithoutResult(status -> {
            Order order = orderRepository.findByPayosOrderCodeForUpdate(payosOrderCode)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found for PayOS order code: " + payosOrderCode));

            if (order.getStatus() != OrderStatus.pending) {
                throw new IllegalStateException(
                        "Only pending orders can be cancelled (current status: " + order.getStatus() + ")");
            }
            cancelPendingOrderAndRestoreInventory(order, effectiveReason);
        });

        try {
            return payOS.cancelPaymentLink(payosOrderCode, effectiveReason);
        } catch (Exception e) {
            log.warn("Local order {} cancelled, but PayOS cancelPaymentLink returned warning: {}",
                    payosOrderCode, e.getMessage());
            return null;
        }
    }

    @Override
    public PaymentLinkData getPaymentLinkInformation(long payosOrderCode) {
        requireAdmin("You do not have permission to view PayOS payment links.");
        try {
            return payOS.getPaymentLinkInformation(payosOrderCode);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to retrieve PayOS payment link: " + e.getMessage(), e);
        }
    }

    @Override
    public String confirmWebhook(String webhookUrl) {
        requireAdmin("You do not have permission to configure PayOS webhooks.");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException("webhookUrl cannot be blank");
        }
        try {
            return payOS.confirmWebhook(webhookUrl);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to confirm PayOS webhook: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getListOrders(String status,
                                             BigDecimal startAmount,
                                             BigDecimal endAmount,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime,
                                             Integer page,
                                             Integer size) {
        requireAdmin("You do not have permission to view all orders.");

        int pageIndex = (page == null || page <= 1) ? 0 : page - 1;
        int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "orderDate"));

        Specification<Order> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                try {
                    OrderStatus parsed = OrderStatus.valueOf(status.trim().toLowerCase(Locale.ROOT));
                    predicates.add(cb.equal(root.get("status"), parsed));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid order status: " + status);
                }
            }
            if (startAmount != null && endAmount != null) {
                predicates.add(cb.between(root.get("totalAmount"), startAmount, endAmount));
            } else {
                if (startAmount != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), startAmount));
                }
                if (endAmount != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), endAmount));
                }
            }
            if (startTime != null && endTime != null) {
                predicates.add(cb.between(root.get("orderDate"), startTime, endTime));
            } else {
                if (startTime != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), startTime));
                }
                if (endTime != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), endTime));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(specification, pageable);
        List<Order> orders = orderPage.getContent();

        // Batch-load order tickets to avoid N+1 queries
        Map<Integer, Integer> ticketCountByOrderId = new HashMap<>();
        if (!orders.isEmpty()) {
            List<Integer> orderIds = orders.stream().map(Order::getOrderId).toList();
            for (OrderTicket ot : orderTicketRepository.findByOrderIdsWithDetails(orderIds)) {
                ticketCountByOrderId.merge(ot.getOrder().getOrderId(), ot.getQuantity(), Integer::sum);
            }
        }

        List<GetOrdersResponseDto> orderDTOs = orders.stream().map(order -> {
            GetOrdersResponseDto dto = new GetOrdersResponseDto();
            dto.setOrderId(order.getOrderId());
            dto.setOrderDate(order.getOrderDate());
            dto.setOrderPayOSCode(order.getPayosOrderCode());
            dto.setStatus(order.getStatus());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setCancellationReason(order.getCancellationReason());
            if (order.getUser() != null) {
                dto.setUserId(order.getUser().getId());
                dto.setUserName(order.getUser().getFullName());
                dto.setUserEmail(order.getUser().getEmail());
            }
            dto.setTotalTicketsCount(ticketCountByOrderId.getOrDefault(order.getOrderId(), 0));
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("listOrders", orderDTOs);
        response.put("pageSize", orderPage.getSize());
        response.put("pageNo", orderPage.getNumber() + 1);
        response.put("totalPages", orderPage.getTotalPages());
        response.put("totalElements", orderPage.getTotalElements());
        response.put("hasNext", orderPage.hasNext());
        response.put("hasPrevious", orderPage.hasPrevious());
        return response;
    }

    @Scheduled(fixedRate = 60_000)
    public void autoCancelPendingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Order> expiredOrders = orderRepository.findByStatusAndOrderDateBefore(OrderStatus.pending, cutoff);
        if (expiredOrders.isEmpty()) {
            return;
        }

        TransactionTemplate perOrderTx = new TransactionTemplate(transactionManager);
        perOrderTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        for (Order candidate : expiredOrders) {
            Integer orderId = candidate.getOrderId();
            Long payosCode = candidate.getPayosOrderCode();
            try {
                Boolean cancelledLocally = perOrderTx.execute(status -> {
                    Order lockedOrder = orderRepository.findByIdForUpdate(orderId).orElse(null);
                    if (lockedOrder == null || lockedOrder.getStatus() != OrderStatus.pending) {
                        return false;
                    }
                    cancelPendingOrderAndRestoreInventory(lockedOrder, "Order auto-cancelled due to timeout");
                    return true;
                });

                if (Boolean.TRUE.equals(cancelledLocally) && payosCode != null) {
                    try {
                        payOS.cancelPaymentLink(payosCode, "Order auto-cancelled due to timeout");
                    } catch (Exception payosEx) {
                        log.warn("Auto-cancelled local order {}, but PayOS cancelPaymentLink failed: {}",
                                orderId, payosEx.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to auto-cancel expired order {}: {}", orderId, e.getMessage(), e);
            }
        }
    }

    private void cancelPendingOrderAndRestoreInventory(Order order, String reason) {
        order.setStatus(OrderStatus.cancelled);
        order.setCancellationReason(reason);
        orderRepository.save(order);

        List<OrderTicket> orderTickets = orderTicketRepository.findByOrder(order);
        // Sort ticketIds ascending before releasing to avoid lock inversions
        orderTickets.stream()
                .sorted(Comparator.comparing(ot -> ot.getTicket().getTicketId()))
                .forEach(ot -> ticketRepository.releaseTickets(ot.getTicket().getTicketId(), ot.getQuantity()));

        if (order.getDiscount() != null) {
            discountRepository.incrementMaxUsage(order.getDiscount().getDiscountId());
        }
    }

    private void requireAdmin(String message) {
        if (!SecurityUtils.hasRole(UserRole.admin)) {
            throw new SecurityException(message);
        }
    }
}