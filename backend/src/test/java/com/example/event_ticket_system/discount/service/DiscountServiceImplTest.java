package com.example.event_ticket_system.discount.service;

import com.example.event_ticket_system.dto.response.DiscountResponseDto;
import com.example.event_ticket_system.entity.Discount;
import com.example.event_ticket_system.entity.Event;
import com.example.event_ticket_system.enums.DiscountType;
import com.example.event_ticket_system.repository.DiscountRepository;
import com.example.event_ticket_system.service.impl.DiscountServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceImplTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountServiceImpl discountService;

    private Discount discount;

    @BeforeEach
    void setUp() {
        Event event = new Event();
        event.setEventId(7);
        event.setEventName("Rock Night");

        discount = new Discount(
                1,
                "DISCOUNT10",
                "Giảm 10%",
                DiscountType.percentage,
                new BigDecimal("10.00"),
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                event,
                100
        );
    }

    @Test
    void getDiscountByCode_ShouldReturnDiscountDto_WhenCodeExists() {
        when(discountRepository.findByCodeWithEvent("DISCOUNT10"))
                .thenReturn(Optional.of(discount));

        DiscountResponseDto result = discountService.getDiscountByCode("DISCOUNT10");

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("DISCOUNT10");
        assertThat(result.getValue()).isEqualByComparingTo("10.00");
        assertThat(result.getEventId()).isEqualTo(7);
        assertThat(result.getEventName()).isEqualTo("Rock Night");
    }

    @Test
    void getDiscountByCode_ShouldThrowException_WhenCodeNotFound() {
        when(discountRepository.findByCodeWithEvent("INVALID"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountService.getDiscountByCode("INVALID"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Discount not found with code: INVALID");
    }
}
