package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Service.ProvinceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provinces")
public class ProvinceController {
    @Autowired
    private ProvinceService provinceService;

    @GetMapping()
    public ResponseEntity<?> getAllProvinces() {
        try {
            return APIResponse.responseBuilder(
                    provinceService.getAllProvinces(),
                    "Provinces fetched successfully",
                    HttpStatus.OK
            );
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred while fetching provinces");
        }
    }
}
