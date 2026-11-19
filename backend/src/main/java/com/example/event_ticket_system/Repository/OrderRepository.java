package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.Order;
import com.example.event_ticket_system.Enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order,Integer> {
    Order findByPayosOrderCode(Long payosOrderCode);
    List<Order> findByStatusAndOrderDateBefore(OrderStatus orderStatus, LocalDateTime cutoff);
    List<Order> findByUserId(Integer userId);
    Page<Order> findAll(Specification<Order> specification, Pageable pageable);
}
