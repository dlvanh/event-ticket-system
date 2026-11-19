package com.example.event_ticket_system.entity;

import com.example.event_ticket_system.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", referencedColumnName = "discount_id")
    private Discount discount;

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDateTime orderDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.pending;

    @Column(name = "payos_order_code", unique = true)
    private Long payosOrderCode;

    @Size(max = 255)
    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return orderId != null && Objects.equals(orderId, other.orderId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
