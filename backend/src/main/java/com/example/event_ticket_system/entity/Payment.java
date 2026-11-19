package com.example.event_ticket_system.entity;

import com.example.event_ticket_system.enums.PaymentStatus;
import jakarta.persistence.*;
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
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_date", nullable = false, updatable = false)
    private LocalDateTime paymentDate;

    @Column(name = "amount_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.success;

    @Column(name = "transaction_reference", length = 128)
    private String transactionReference;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment other)) return false;
        return paymentId != null && Objects.equals(paymentId, other.paymentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
