package com.example.event_ticket_system.dto.response;

import com.example.event_ticket_system.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountResponseDto {
    private Integer discountId;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal value;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Integer maxUsage;
    private Integer eventId;
    private String eventName;
}
