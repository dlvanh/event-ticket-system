package com.example.event_ticket_system.service;

import com.example.event_ticket_system.entity.District;

import java.util.List;

public interface DistrictService {
    List<District> getDistrictsByProvinceId(Integer provinceId);
}
