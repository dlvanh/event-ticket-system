package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.Discount;
import com.example.event_ticket_system.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Integer> {
    Optional<Discount> findByCode(String code);

    List<Discount> findByEvent(Event event);
}
