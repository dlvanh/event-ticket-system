package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Entity.OrderTicket;
import com.example.event_ticket_system.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTicketRepository extends JpaRepository<OrderTicket, Integer> {
    List<OrderTicket> findByOrder(Order order);
    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM OrderTicket o WHERE o.ticket.event = :event")
    long sumQuantityByEvent(@Param("event") Event event);
}
