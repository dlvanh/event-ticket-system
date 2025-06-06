package com.example.event_ticket_system.Entity;

import com.example.event_ticket_system.Enums.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "discounts")
public class Discount {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Integer discountId;

    @Column(name="code", nullable = false, unique = true)
    private String code;

    @Column(name="description")
    private String description;

    @Column(name="discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name="value", nullable = false)
    private Double value;

    @Column(name="valid_from")
    private java.time.LocalDate validFrom;

    @Column(name="valid_to")
    private java.time.LocalDate validTo;

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "event_id", nullable = false)
    private Event event;

    @Column(name="max_usage")
    private Integer maxUsage;
}
