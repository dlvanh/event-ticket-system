package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.Entity.District;
import com.example.event_ticket_system.Repository.DistrictRepository;
import com.example.event_ticket_system.Service.DistrictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DistrictServiceImpl implements DistrictService {
    @Autowired
    private DistrictRepository districtRepository;

    @Override
    public List<District> getDistrictsByProvinceId(Integer provinceId) {
        return districtRepository.findByProvinceId(provinceId);
    }
}
