package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import com.example.event_ticket_system.DTO.response.GetOrdersResponseDto;
import com.example.event_ticket_system.Entity.*;
import com.example.event_ticket_system.Enums.DiscountType;
import com.example.event_ticket_system.Enums.OrderStatus;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Repository.*;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final DiscountRepository discountRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final PayOS payOS; // Inject PayOS
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public CheckoutResponseData createOrder(OrderRequestDto dto, HttpServletRequest request) {
        // Validate user
        User currentUser = userRepository.findById(
                        jwtUtil.extractUserId(request.getHeader("Authorization").substring(7)))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (currentUser.getRole() != UserRole.customer) {
            throw new SecurityException("Only customers can create orders");
        }
        if (dto.getTickets().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one ticket");
        }

        // Calculate total amount and prepare order tickets
        double totalAmount = 0.0;
        int totalQuantity = 0;
        List<OrderTicket> orderTickets = new ArrayList<>();

        for (OrderRequestDto.OrderTicketRequestDto ticketDto : dto.getTickets()) {
            Ticket ticket = ticketRepository.findById(ticketDto.getTicketId())
                    .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketDto.getTicketId()));
            // Validate quantity (1-5)
            if (ticketDto.getQuantity() <= 0 || ticketDto.getQuantity() > 5) {
                throw new IllegalArgumentException("Quantity must be between 1 and 5 for ticket: " + ticket.getTicketType());
            }
            // Check if ticket belongs to event
            if (!ticket.getEvent().getEventId().equals(dto.getEventId())) {
                throw new IllegalArgumentException("Ticket " + ticket.getTicketType() + " does not belong to event");
            }
            // Check sale period
            LocalDateTime now = LocalDateTime.now();
            if (ticket.getSaleStart() != null && now.isBefore(ticket.getSaleStart())) {
                throw new IllegalArgumentException("Ticket sale for " + ticket.getTicketType() + " has not started");
            }
            if (ticket.getSaleEnd() != null && now.isAfter(ticket.getSaleEnd())) {
                throw new IllegalArgumentException("Ticket sale for " + ticket.getTicketType() + " has ended");
            }
            // Check available quantity
            int available = ticket.getQuantityTotal() - ticket.getQuantitySold();
            if (ticketDto.getQuantity() > available) {
                throw new IllegalArgumentException("Not enough tickets available for: " + ticket.getTicketType());
            }

            double amount = ticket.getPrice() * ticketDto.getQuantity();
            totalAmount += amount;
            totalQuantity += ticketDto.getQuantity();

            OrderTicket orderTicket = new OrderTicket();
            orderTicket.setTicket(ticket);
            orderTicket.setQuantity(ticketDto.getQuantity());
            orderTicket.setUnitPrice(ticket.getPrice());
            orderTickets.add(orderTicket);
        }

        // Apply discount if provided
        if (dto.getDiscountCode() != null && !dto.getDiscountCode().isBlank()) {
            Discount discount = discountRepository.findByCode(dto.getDiscountCode())
                    .orElseThrow(() -> new EntityNotFoundException("Discount code not found"));
            // Check discount validity
            LocalDate today = LocalDate.now();
            if ((discount.getValidFrom() != null && today.isBefore(discount.getValidFrom())) ||
                    (discount.getValidTo() != null && today.isAfter(discount.getValidTo()))) {
                throw new IllegalArgumentException("Discount code is not valid");
            }
            // Check if discount applies to event
            if (!discount.getEvent().getEventId().equals(dto.getEventId())) {
                throw new IllegalArgumentException("Discount code does not apply to this event");
            }
            // Check if discount usage available
            if (discount.getMaxUsage() <= 0 ){
                throw new IllegalArgumentException("Discount code has reached its maximum usage limit");
            }
            if (discount.getDiscountType() == DiscountType.percentage) {
                totalAmount = totalAmount * (1 - discount.getValue() / 100.0);
            } else if (discount.getDiscountType() == DiscountType.fixed_amount) {
                totalAmount = Math.max(0, totalAmount - discount.getValue());
            }
            // Ensure total amount is not negative
            if (totalAmount < 0) {
                totalAmount = 0;
            }
            // Decrease discount usage
            discount.setMaxUsage(discount.getMaxUsage() - 1);
            discountRepository.save(discount);
        }

        // Create order
        Order order = new Order();
        order.setUser(currentUser);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.pending);
        order.setTotalAmount(totalAmount);
        // Generate PayOS order code
        String currentTimeString = String.valueOf(new Date().getTime());
        long payosOrderCode = Long.parseLong(currentTimeString.substring(currentTimeString.length() - 6));
        order.setPayosOrderCode(payosOrderCode);
        orderRepository.save(order);

        // Save order tickets and update ticket quantities
        for (OrderTicket ot : orderTickets) {
            ot.setOrder(order);
            orderTicketRepository.save(ot);
            Ticket ticket = ot.getTicket();
            ticket.setQuantitySold(ticket.getQuantitySold() + ot.getQuantity());
            ticketRepository.save(ticket);
        }

        // Create PayOS payment link
        String eventName = ticketRepository.findById(dto.getTickets().get(0).getTicketId())
                .map(ticket -> ticket.getEvent().getEventName())
                .orElse("Event Tickets");
        ItemData itemData = ItemData.builder()
                .name("Tickets for " + eventName)
                .price((int) totalAmount)
                .quantity(totalQuantity)
                .build();
        PaymentData paymentData = PaymentData.builder()
                .orderCode(payosOrderCode)
                .description("Thanh toán Eventa")
                .amount((int) totalAmount)
                .item(itemData)
                .returnUrl(dto.getReturnUrl())
                .cancelUrl(dto.getCancelUrl())
                .build();
        try {
            return payOS.createPaymentLink(paymentData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create PayOS payment link: " + e.getMessage());
        }
    }

    @Override
    public void confirmPayment(String payosOrderCode, HttpServletRequest request) {
        // Validate user
        User currentUser = userRepository.findById(
                        jwtUtil.extractUserId(request.getHeader("Authorization").substring(7)))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (currentUser.getRole() != UserRole.admin) {
            throw new SecurityException("Only admins can confirm payments");
        }

        // Find order by PayOS order code
        Order order = orderRepository.findByPayosOrderCode(Long.valueOf(payosOrderCode));
        if (order == null) {
            throw new EntityNotFoundException("Order not found for PayOS order code: " + payosOrderCode);
        }

        // Update order status based on payment status
        if (order.getStatus() == OrderStatus.paid) {
            throw new IllegalArgumentException("Order is already paid");
        }

        order.setStatus(OrderStatus.paid);
        orderRepository.save(order);
    }

    @Override
    public void cancelOrder(String orderCode, HttpServletRequest request) {
        // Validate user
        User currentUser = userRepository.findById(
                        jwtUtil.extractUserId(request.getHeader("Authorization").substring(7)))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (currentUser.getRole() != UserRole.admin) {
            throw new SecurityException("Only admins can cancel orders");
        }

        // Find order by PayOS order code
        Order order = orderRepository.findByPayosOrderCode(Long.valueOf(orderCode));
        if (order == null) {
            throw new EntityNotFoundException("Order not found for PayOS order code: " + orderCode);
        }

        // Cancel the order and restore ticket quantities
        order.setStatus(OrderStatus.cancelled);
        List<OrderTicket> orderTickets = orderTicketRepository.findByOrder(order);
        for (OrderTicket orderTicket : orderTickets) {
            Ticket ticket = orderTicket.getTicket();
            ticket.setQuantitySold(ticket.getQuantitySold() - orderTicket.getQuantity());
            ticketRepository.save(ticket);
        }
        orderRepository.save(order);
    }

    @Override
    public Map<String, Object> getListOrders(HttpServletRequest request, String status, Double startAmount, Double endAmount, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer size) {
        Integer userId = jwtUtil.extractUserId(request.getHeader("Authorization").substring(7));
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (!UserRole.admin.equals(currentUser.getRole())) {
            throw new SecurityException("You do not have permission to view all orders.");
        }
        // TODO: implement sorting direction for user
        if (page > 0) {
            page -= 1;
        }

        Pageable pageable  = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderDate"));

        Specification<Order> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (startAmount != null && endAmount != null) {
                predicates.add(criteriaBuilder.between(root.get("totalAmount"), startAmount, endAmount));
            } else {
                if (startAmount != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalAmount"), startAmount));
                }
                if (endAmount != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalAmount"), endAmount));
                }
            }

            if (startTime != null && endTime != null) {
                predicates.add(criteriaBuilder.between(root.get("orderDate"), startTime, endTime));
            } else {
                if (startTime != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("orderDate"), startTime));
                }
                if (endTime != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("orderDate"), endTime));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(specification, pageable);

        List<GetOrdersResponseDto> orderDTOs = orderPage.getContent().stream().map(order -> {
            GetOrdersResponseDto dto = new GetOrdersResponseDto();

            dto.setOrderId(order.getOrderId());
            dto.setUserId(order.getUser().getId());
            dto.setOrderDate(order.getOrderDate());
            dto.setOrderPayOSCode(order.getPayosOrderCode());
            dto.setStatus(order.getStatus());
            dto.setTotalAmount(order.getTotalAmount());
            dto.setCancellationReason(order.getCancellationReason());
            if (order.getUser() != null) {
                dto.setUserName(order.getUser().getFullName());
                dto.setUserEmail(order.getUser().getEmail());
            }
            dto.setTotalTicketsCount(orderTicketRepository.findByOrderOrderId(order.getOrderId())
                    .stream()
                    .mapToInt(OrderTicket::getQuantity)
                    .sum());

            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("listOrders", orderDTOs); // Danh sách đơn hàng
        response.put("pageSize", orderPage.getSize()); // Kích thước trang
        response.put("pageNo", orderPage.getNumber() + 1); // Số trang hiện tại (chuyển về 1-based)
        response.put("totalPages", orderPage.getTotalPages()); // Tổng số trang
        response.put("totalElements", orderPage.getTotalElements()); // Tổng số đơn hàng
        response.put("hasNext", orderPage.hasNext()); // Có trang tiếp theo không
        response.put("hasPrevious", orderPage.hasPrevious()); // Có trang trước không

        return response;
    }

    @Scheduled(fixedRate = 60000) // every 1 minute
    @Transactional
    public void autoCancelPendingOrders() throws Exception {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Order> oldPendingOrders = orderRepository.findByStatusAndOrderDateBefore(OrderStatus.pending, cutoff);
        if (oldPendingOrders.isEmpty()) {
            return;
        }
        for (Order order : oldPendingOrders) {
            order.setStatus(OrderStatus.cancelled);
            order.setCancellationReason("Order auto-cancelled due to timeout");
            payOS.cancelPaymentLink(order.getPayosOrderCode(), "Order auto-cancelled due to timeout");
            List<OrderTicket> orderTickets = orderTicketRepository.findByOrder(order);
            for (OrderTicket orderTicket : orderTickets) {
                Ticket ticket = orderTicket.getTicket();
                ticket.setQuantitySold(ticket.getQuantitySold() - orderTicket.getQuantity());
                ticketRepository.save(ticket);
            }
            orderRepository.save(order);
        }
    }
}