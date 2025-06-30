package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import com.example.event_ticket_system.Entity.*;
import com.example.event_ticket_system.Enums.DiscountType;
import com.example.event_ticket_system.Enums.OrderStatus;
import com.example.event_ticket_system.Enums.UserRole;
import com.example.event_ticket_system.Repository.*;
import com.example.event_ticket_system.Security.JwtUtil;
import com.example.event_ticket_system.Service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final DiscountRepository discountRepository;
    private final OrderTicketRepository orderTicketRepository;

    private final JwtUtil jwtUtil;

    @Override
    public Integer createOrder(OrderRequestDto dto, HttpServletRequest request) {
        User currentUser = userRepository.findById(
                        jwtUtil.extractUserId(request.getHeader("Authorization").substring(7)))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (currentUser.getRole() != UserRole.customer) {
            throw new SecurityException("Only customers can create orders");
        }
        if (dto.getTickets().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one ticket");
        }
        double totalAmount = 0.0;
        List<OrderTicket> orderTickets = new ArrayList<>();

        for (OrderRequestDto.OrderTicketRequestDto ticketDto : dto.getTickets()) {
            Ticket ticket = ticketRepository.findById(ticketDto.getTicketId())
                    .orElseThrow(() -> new EntityNotFoundException("Ticket not found"));
            // Số lượng vé không được quá 5
            if (ticketDto.getQuantity() <= 0 || ticketDto.getQuantity() > 5) {
                throw new IllegalArgumentException("Số lượng vé phải phải lớn hơn 0 và không được quá 5");
            }
            // Kiểm tra vé có thuộc sự kiện
            if (!ticket.getEvent().getEventId().equals(dto.getEventId())) {
                throw new IllegalArgumentException("Ticket không thuộc event");
            }

            // Kiểm tra còn đủ vé không
            if (ticket.getQuantityTotal() - ticket.getQuantitySold() < ticketDto.getQuantity()) {
                throw new IllegalArgumentException("Không đủ vé còn lại cho loại: " + ticket.getTicketType());
            }

            double amount = ticket.getPrice() * ticketDto.getQuantity();
            totalAmount += amount;

            OrderTicket orderTicket = new OrderTicket();
            orderTicket.setTicket(ticket);
            orderTicket.setQuantity(ticketDto.getQuantity());
            orderTicket.setUnitPrice(ticket.getPrice());
            orderTickets.add(orderTicket);
        }

        // Áp dụng mã giảm giá nếu có
        if (dto.getDiscountCode() != null && !dto.getDiscountCode().isBlank()) {
            Discount discount = discountRepository.findByCode(dto.getDiscountCode())
                    .orElseThrow(() -> new EntityNotFoundException("Mã giảm giá không tồn tại"));

            // Kiểm tra hạn dùng
            LocalDate today = LocalDate.now();
            if ((discount.getValidFrom() != null && today.isBefore(discount.getValidFrom())) ||
                    (discount.getValidTo() != null && today.isAfter(discount.getValidTo()))) {
                throw new IllegalArgumentException("Mã giảm giá hết hạn");
            }

            // Kiểm tra mã có thuộc sự kiện
            if (!discount.getEvent().getEventId().equals(dto.getEventId())) {
                throw new IllegalArgumentException("Mã giảm giá không áp dụng cho sự kiện này");
            }

            if (discount.getDiscountType() == DiscountType.percentage) {
                totalAmount = totalAmount * (1 - discount.getValue() / 100.0);
            } else if (discount.getDiscountType() == DiscountType.fixed_amount) {
                totalAmount = Math.max(0, totalAmount - discount.getValue());
            }
        }

        // Tạo Order
        Order order = new Order();
        order.setUser(currentUser);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus(OrderStatus.pending);
        order.setTotalAmount(totalAmount);
        orderRepository.save(order);

        // Lưu OrderTicket & cập nhật quantity_sold
        for (OrderTicket ot : orderTickets) {
            ot.setOrder(order);
            orderTicketRepository.save(ot);

            Ticket ticket = ot.getTicket();
            ticket.setQuantitySold(ticket.getQuantitySold() + ot.getQuantity());
            ticketRepository.save(ticket);
        }
        return order.getOrderId();
    }
}
