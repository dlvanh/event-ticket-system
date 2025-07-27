package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.Entity.Event;
import com.example.event_ticket_system.Entity.Review;
import com.example.event_ticket_system.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByEvent(Event event);

    List<Review> findByUser(User user);
}
