package com.example.event_ticket_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "event_id", nullable = false)
    private Event event;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "review_date", nullable = false, updatable = false)
    private LocalDateTime reviewDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review other)) return false;
        return reviewId != null && Objects.equals(reviewId, other.reviewId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
