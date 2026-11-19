package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.entity.Review;
import com.example.event_ticket_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByEvent(Event event);

    List<Review> findByUser(User user);

    boolean existsByEventEventIdAndUserId(Integer eventId, Integer userId);

    @Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.event = :event ORDER BY r.reviewDate DESC")
    List<Review> findByEventWithUser(@Param("event") Event event);

    @Query("SELECT r FROM Review r JOIN FETCH r.event WHERE r.user = :user ORDER BY r.reviewDate DESC")
    List<Review> findByUserWithEvent(@Param("user") User user);
}
