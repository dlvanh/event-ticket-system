package com.example.event_ticket_system.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;
}
