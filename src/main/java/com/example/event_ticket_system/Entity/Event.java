package com.example.event_ticket_system.Entity;

import com.example.event_ticket_system.Enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="events")
public class Event {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Integer eventId;

    @ManyToOne
    @JoinColumn(name = "organizer_id", referencedColumnName = "user_id", nullable = false)
    private User organizer;

    @Column(name="event_name", nullable = false)
    private String eventName;

    @Column(name="description")
    private String description;

    @Column(name="location", nullable = false)
    private String location;

    @Column(name="start_time", nullable = false)
    private java.time.LocalDateTime startTime;

    @Column(name="end_time", nullable = false)
    private java.time.LocalDateTime endTime;

    @Column(name="category", nullable = false)
    private String category;

    @Column(name="status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.upcoming;

    @Column(name="created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

}
