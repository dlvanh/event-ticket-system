package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.entity.District;
import com.example.event_ticket_system.service.DistrictService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictService districtService;

    @GetMapping("/{provinceId}")
    public ResponseEntity<ApiResponse<List<District>>> getDistrictsByProvinceId(@PathVariable Integer provinceId) {
        return ApiResponse.ok(districtService.getDistrictsByProvinceId(provinceId), "Districts fetched successfully");
    }
}
