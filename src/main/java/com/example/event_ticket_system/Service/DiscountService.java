package com.example.event_ticket_system.Service;

import com.example.event_ticket_system.Entity.Discount;

public interface DiscountService {
    Discount getDiscountByCode(String code);
}
