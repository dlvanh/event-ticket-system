package com.example.event_ticket_system.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "wards")
public class Ward {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @Column(name = "ward_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "district_id", referencedColumnName = "district_id", nullable = false)
    private District district;

    @Column(name="name", nullable = false)
    private String name;
}
