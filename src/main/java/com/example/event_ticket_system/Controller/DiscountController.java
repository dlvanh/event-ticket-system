package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Service.DiscountService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {
    @Autowired
    private DiscountService discountService;

    @GetMapping("/{code}")
    public ResponseEntity<?> getDiscountByCode(@PathVariable String code) {
        try {
            return APIResponse.responseBuilder(
                    discountService.getDiscountByCode(code),
                    "Discount fetched successfully",
                    HttpStatus.OK
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Discount not found with code: " + code);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred while fetching the discount");
        }
    }
}
