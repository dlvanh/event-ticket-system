package com.example.event_ticket_system.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_tickets")
@NoArgsConstructor
@AllArgsConstructor
@Data
@IdClass(OrderTicketsIds.class)
public class OrderTicket {
    @Id
    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    private Order order;

    @Id
    @ManyToOne
    @JoinColumn(name = "ticket_id", referencedColumnName = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name="unit_price", nullable = false)
    private Double unitPrice;
}
