package com.example.event_ticket_system.service.impl;

import com.example.event_ticket_system.dto.response.DiscountResponseDto;
import com.example.event_ticket_system.entity.Discount;
import com.example.event_ticket_system.repository.DiscountRepository;
import com.example.event_ticket_system.service.DiscountService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepository;

    @Override
    public DiscountResponseDto getDiscountByCode(String code) {
        Discount discount = discountRepository.findByCodeWithEvent(code)
                .orElseThrow(() -> new EntityNotFoundException("Discount not found with code: " + code));

        return DiscountResponseDto.builder()
                .discountId(discount.getDiscountId())
                .code(discount.getCode())
                .description(discount.getDescription())
                .discountType(discount.getDiscountType())
                .value(discount.getValue())
                .validFrom(discount.getValidFrom())
                .validTo(discount.getValidTo())
                .maxUsage(discount.getMaxUsage())
                .eventId(discount.getEvent() != null ? discount.getEvent().getEventId() : null)
                .eventName(discount.getEvent() != null ? discount.getEvent().getEventName() : null)
                .build();
    }
}
