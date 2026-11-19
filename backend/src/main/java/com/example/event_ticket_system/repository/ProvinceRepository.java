package com.example.event_ticket_system.repository;

import com.example.event_ticket_system.entity.Province;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProvinceRepository extends JpaRepository<Province, Integer> {

}
