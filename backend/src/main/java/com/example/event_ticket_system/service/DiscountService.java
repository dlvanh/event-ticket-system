package com.example.event_ticket_system.service;

import com.example.event_ticket_system.dto.response.DiscountResponseDto;

public interface DiscountService {
    DiscountResponseDto getDiscountByCode(String code);
}
