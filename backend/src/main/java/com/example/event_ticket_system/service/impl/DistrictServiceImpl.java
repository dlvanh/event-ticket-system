package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.entity.District;
import com.example.event_ticket_system.repository.DistrictRepository;
import com.example.event_ticket_system.service.DistrictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DistrictServiceImpl implements DistrictService {

    private final DistrictRepository districtRepository;

    @Override
    public List<District> getDistrictsByProvinceId(Integer provinceId) {
        return districtRepository.findByProvinceId(provinceId);
    }
}
