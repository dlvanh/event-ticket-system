package com.example.event_ticket_system.Controller;

import com.example.event_ticket_system.DTO.request.OrderRequestDto;
import com.example.event_ticket_system.DTO.response.APIResponse;
import com.example.event_ticket_system.Service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping()
    public ResponseEntity<Object> createOrder(@RequestBody @Valid OrderRequestDto orderRequestDto,
                                              BindingResult bindingResult,
                                              HttpServletRequest request) {
        try {
            Map<String, String> errors = new HashMap<>();
            if (bindingResult.hasErrors()) {
                bindingResult.getFieldErrors().forEach(error ->
                        errors.put(error.getField(), error.getDefaultMessage())
                );
            }
            if (!errors.isEmpty()) {
                return APIResponse.responseBuilder(
                        errors,
                        "Validation failed",
                        HttpStatus.BAD_REQUEST
                );
            }
            Integer orderId = orderService.createOrder(orderRequestDto,request);
            return APIResponse.responseBuilder(
                    orderId,
                    "Event created successfully",
                    HttpStatus.CREATED
            );
        } catch (SecurityException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );
        } catch (EntityNotFoundException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.NOT_FOUND
            );
        } catch (IllegalArgumentException e) {
            return APIResponse.responseBuilder(
                    null,
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            return APIResponse.responseBuilder(
                    null,
                    "An unexpected error occurred: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
