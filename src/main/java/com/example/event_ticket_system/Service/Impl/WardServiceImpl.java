package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.Entity.Ward;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WardServiceImpl implements com.example.event_ticket_system.Service.WardService {

    @Autowired
    private com.example.event_ticket_system.Repository.WardRepository wardRepository;

    @Override
    public List<Ward> getWardsByDistrictId(Integer districtId) {
        return wardRepository.findByDistrictId(districtId);
    }
}
