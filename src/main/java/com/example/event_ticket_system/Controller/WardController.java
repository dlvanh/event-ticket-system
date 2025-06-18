package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.response.APIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wards")
public class WardController {
    @Autowired
    private com.example.event_ticket_system.Service.WardService wardService;

    @GetMapping("/{districtId}")
    public ResponseEntity<?> getWardsByDistrictId(@PathVariable Integer districtId) {
        try {
            return APIResponse.responseBuilder(
                    wardService.getWardsByDistrictId(districtId),
                    "Wards fetched successfully",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred while fetching wards");
        }
    }
}
