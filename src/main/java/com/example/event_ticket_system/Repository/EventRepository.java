package com.example.event_ticket_system.Repository;

import com.example.event_ticket_system.DTO.response.RecommendEventsResponseDto;
import com.example.event_ticket_system.Entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer>, JpaSpecificationExecutor<Event> {
    @Query("SELECT e FROM Event e " +
            "WHERE e.category IN :genres " +
            "AND e.startTime >= :now " +
            "AND e.status = com.example.event_ticket_system.Enums.EventStatus.upcoming " +
            "AND e.approvalStatus = com.example.event_ticket_system.Enums.ApprovalStatus.approved")
    List<Event> findUpcomingEventsByGenres(@Param("genres") List<String> genres,
                                           @Param("now") LocalDateTime now);

    // Lấy tất cả sự kiện có start_time sau hiện tại
    @Query("SELECT e FROM Event e " +
            "WHERE e.startTime >= :now " +
            "AND e.status = com.example.event_ticket_system.Enums.EventStatus.upcoming " +
            "AND e.approvalStatus = com.example.event_ticket_system.Enums.ApprovalStatus.approved")
    List<Event> findAllUpcomingEvents(@Param("now") LocalDateTime now);
}
