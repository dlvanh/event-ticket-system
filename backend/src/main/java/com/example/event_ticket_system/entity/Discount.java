package com.example.event_ticket_system.entity;

import com.example.event_ticket_system.enums.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "discounts")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Integer discountId;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "value", nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "event_id", nullable = false)
    private Event event;

    @Column(name = "max_usage")
    private Integer maxUsage;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Discount other)) return false;
        return discountId != null && Objects.equals(discountId, other.discountId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
