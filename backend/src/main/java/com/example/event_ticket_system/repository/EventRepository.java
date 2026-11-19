package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer>, JpaSpecificationExecutor<Event> {

    @Query("SELECT e FROM Event e " +
           "JOIN FETCH e.organizer " +
           "JOIN FETCH e.ward w " +
           "JOIN FETCH w.district d " +
           "JOIN FETCH d.province " +
           "WHERE e.eventId = :eventId")
    Optional<Event> findByIdWithDetails(@Param("eventId") Integer eventId);

    @Query("SELECT e FROM Event e " +
           "JOIN FETCH e.ward w " +
           "JOIN FETCH w.district d " +
           "JOIN FETCH d.province " +
           "WHERE e.category IN :genres " +
           "AND e.startTime >= :now " +
           "AND e.status = com.example.event_ticket_system.enums.EventStatus.upcoming " +
           "AND e.approvalStatus = com.example.event_ticket_system.enums.ApprovalStatus.approved")
    List<Event> findUpcomingEventsByGenres(@Param("genres") List<String> genres,
                                           @Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e " +
           "JOIN FETCH e.ward w " +
           "JOIN FETCH w.district d " +
           "JOIN FETCH d.province " +
           "WHERE e.startTime >= :now " +
           "AND e.status = com.example.event_ticket_system.enums.EventStatus.upcoming " +
           "AND e.approvalStatus = com.example.event_ticket_system.enums.ApprovalStatus.approved")
    List<Event> findAllUpcomingEvents(@Param("now") LocalDateTime now);
}
