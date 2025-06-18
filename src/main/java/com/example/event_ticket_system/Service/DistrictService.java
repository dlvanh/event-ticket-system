package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.Entity.District;

import java.util.List;

public interface DistrictService {
    List<District> getDistrictsByProvinceId(Integer provinceId);
}
