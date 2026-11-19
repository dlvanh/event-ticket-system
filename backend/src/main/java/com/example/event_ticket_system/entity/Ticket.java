package com.example.event_ticket_system.entity;

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
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Integer ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "event_id", nullable = false)
    private Event event;

    @Column(name = "price", nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "ticket_type", nullable = false)
    private String ticketType;

    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;

    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold = 0;

    @Column(name = "sale_start")
    private LocalDateTime saleStart;

    @Column(name = "sale_end")
    private LocalDateTime saleEnd;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket other)) return false;
        return ticketId != null && Objects.equals(ticketId, other.ticketId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
