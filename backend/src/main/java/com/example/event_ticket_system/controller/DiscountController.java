package com.example.event_ticket_system.controller;

import com.example.event_ticket_system.dto.response.ApiResponse;
import com.example.event_ticket_system.dto.response.DiscountResponseDto;
import com.example.event_ticket_system.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<DiscountResponseDto>> getDiscountByCode(@PathVariable String code) {
        return ApiResponse.ok(discountService.getDiscountByCode(code), "Discount fetched successfully");
    }
}
