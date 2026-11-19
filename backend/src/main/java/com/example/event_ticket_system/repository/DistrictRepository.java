package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DistrictRepository extends JpaRepository<District, Integer> {
    List<District> findByProvinceId(Integer provinceId);
}
