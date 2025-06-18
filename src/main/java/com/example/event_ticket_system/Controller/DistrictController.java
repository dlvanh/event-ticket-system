package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Service.DistrictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/districts")
public class DistrictController {
    @Autowired
    private DistrictService districtService;

    @GetMapping("/{provinceId}")
    public ResponseEntity<?> getDistrictsByProvinceId(@PathVariable Integer provinceId) {
        try {
            return APIResponse.responseBuilder(
                    districtService.getDistrictsByProvinceId(provinceId),
                    "Districts fetched successfully",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred while fetching districts");
        }
    }

}
