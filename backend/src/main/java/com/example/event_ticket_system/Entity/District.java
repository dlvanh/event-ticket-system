package com.example.event_ticket_system.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "districts")
public class District {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    @jakarta.persistence.Column(name = "district_id")
    private Integer id;

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "province_id", referencedColumnName = "province_id", nullable = false)
    private Province province;

    @jakarta.persistence.Column(name = "name", nullable = false)
    private String name;
}
