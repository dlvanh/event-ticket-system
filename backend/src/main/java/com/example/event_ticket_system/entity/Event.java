package com.example.event_ticket_system.entity;

import com.example.event_ticket_system.enums.ApprovalStatus;
import com.example.event_ticket_system.enums.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", referencedColumnName = "user_id", nullable = false)
    private User organizer;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id", referencedColumnName = "ward_id", nullable = false)
    private Ward ward;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.upcoming;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "approval_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus approvalStatus = ApprovalStatus.pending;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "background_url")
    private String backgroundUrl;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event other)) return false;
        return eventId != null && Objects.equals(eventId, other.eventId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
