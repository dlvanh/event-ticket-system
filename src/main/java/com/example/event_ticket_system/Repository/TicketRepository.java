package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket,Integer> {
}
