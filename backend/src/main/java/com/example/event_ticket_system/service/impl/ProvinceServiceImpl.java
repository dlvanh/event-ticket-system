package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.entity.Province;
import com.example.event_ticket_system.repository.ProvinceRepository;
import com.example.event_ticket_system.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceRepository provinceRepository;

    @Override
    public List<Province> getAllProvinces() {
        return provinceRepository.findAll();
    }
}
