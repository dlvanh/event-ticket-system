package com.example.event_ticket_system.Service.Impl;

import com.example.event_ticket_system.Entity.Discount;
import com.example.event_ticket_system.Repository.DiscountRepository;
import com.example.event_ticket_system.Service.DiscountService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DiscountServiceImpl implements DiscountService {
    @Autowired
    private DiscountRepository discountRepository;

    @Override
    public Discount getDiscountByCode(String code) {
        return discountRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with code: " + code));
    }
}
