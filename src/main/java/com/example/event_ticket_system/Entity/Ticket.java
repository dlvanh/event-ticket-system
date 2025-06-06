package com.example.event_ticket_system.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Integer ticketId;

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "event_id", nullable = false)
    private Event event;

    @Column(name="ticket_type", nullable = false)
    private String ticketType;

    @Column(name="quantity_total", nullable = false)
    private Integer quantityTotal;

    @Column(name="quantity_sold", nullable = false)
    private Integer quantitySold;

    @Column(name="sale_start")
    private java.time.LocalDateTime saleStart;

    @Column(name="sale_end")
    private java.time.LocalDateTime saleEnd;
}
