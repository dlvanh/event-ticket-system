package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.entity.Ward;
import com.example.event_ticket_system.service.WardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wards")
@RequiredArgsConstructor
public class WardController {

    private final WardService wardService;

    @GetMapping("/{districtId}")
    public ResponseEntity<ApiResponse<List<Ward>>> getWardsByDistrictId(@PathVariable Integer districtId) {
        return ApiResponse.ok(wardService.getWardsByDistrictId(districtId), "Wards fetched successfully");
    }
}
