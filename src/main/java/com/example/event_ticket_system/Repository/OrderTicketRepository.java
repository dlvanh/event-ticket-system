package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTicketRepository extends JpaRepository<OrderTicket, Integer> {
    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM OrderTicket o WHERE o.ticket.event = :event")
    long sumQuantityByEvent(@Param("event") Event event);
    @Query("SELECT COALESCE(SUM(ot.quantity), 0) FROM OrderTicket ot WHERE ot.ticket = :ticket")
    int sumQuantityByTicket(@Param("ticket") Ticket ticket);
}
