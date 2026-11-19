package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.entity.Province;
import com.example.event_ticket_system.service.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/provinces")
@RequiredArgsConstructor
public class ProvinceController {

    private final ProvinceService provinceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Province>>> getAllProvinces() {
        return ApiResponse.ok(provinceService.getAllProvinces(), "Provinces fetched successfully");
    }
}
