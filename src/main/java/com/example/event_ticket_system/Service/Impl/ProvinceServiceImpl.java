package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.Entity.Province;
import com.example.event_ticket_system.Repository.ProvinceRepository;
import com.example.event_ticket_system.Service.ProvinceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProvinceServiceImpl implements ProvinceService {
    @Override
    public List<Province> getAllProvinces() {
        return provinceRepository.findAll();
    }

    private final ProvinceRepository provinceRepository;

    public ProvinceServiceImpl(ProvinceRepository provinceRepository) {
        this.provinceRepository = provinceRepository;
    }
}
