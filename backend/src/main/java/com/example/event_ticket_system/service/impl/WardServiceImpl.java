package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.entity.Ward;
import com.example.event_ticket_system.repository.WardRepository;
import com.example.event_ticket_system.service.WardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WardServiceImpl implements WardService {

    private final WardRepository wardRepository;

    @Override
    public List<Ward> getWardsByDistrictId(Integer districtId) {
        return wardRepository.findByDistrictId(districtId);
    }
}
