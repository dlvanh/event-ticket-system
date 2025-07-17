package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order,Integer> {
    Order findByPayosOrderCode(Long payosOrderCode);
}
