package com.example.event_ticket_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_tickets")
public class OrderTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_ticket_id")
    private Integer orderTicketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "checkin_token", length = 255)
    private String checkinToken;

    @Column(name = "checked_in_at")
    private java.time.LocalDateTime checkedInAt;

    @Lob
    @Column(name = "qr_code", columnDefinition = "MEDIUMBLOB")
    @Basic(fetch = FetchType.LAZY)
    private byte[] qrCode;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderTicket other)) return false;
        return orderTicketId != null && Objects.equals(orderTicketId, other.orderTicketId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
