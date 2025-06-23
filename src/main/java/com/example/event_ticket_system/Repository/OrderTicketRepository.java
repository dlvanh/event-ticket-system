package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.OrderTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTicketRepository extends JpaRepository<OrderTicket, Integer> {
}
