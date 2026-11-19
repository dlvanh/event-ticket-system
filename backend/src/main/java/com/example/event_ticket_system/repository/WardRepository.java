package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WardRepository extends JpaRepository<Ward, Integer> {
    List<Ward> findByDistrictId(Integer districtId);
}
