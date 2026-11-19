package com.example.event_ticket_system.discount.service;

import com.example.event_ticket_system.Entity.Discount;
import com.example.event_ticket_system.Enums.DiscountType;
import com.example.event_ticket_system.Repository.DiscountRepository;
import com.example.event_ticket_system.Service.Impl.DiscountServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        discount = new Discount(
                1,
                "DISCOUNT10",
                "Giảm 10%",
                DiscountType.percentage,
                10.0,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                null,
                100
        );
    }

    @Test
    void getDiscountByCode_ShouldReturnDiscount_WhenCodeExists() {
        // Arrange
        when(discountRepository.findByCode("DISCOUNT10"))
                .thenReturn(Optional.of(discount));

        // Act
        Discount result = discountService.getDiscountByCode("DISCOUNT10");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("DISCOUNT10");
        assertThat(result.getValue()).isEqualTo(10.0);
    }

    @Test
    void getDiscountByCode_ShouldThrowException_WhenCodeNotFound() {
        // Arrange
        when(discountRepository.findByCode("INVALID"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> discountService.getDiscountByCode("INVALID"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Discount not found with code: INVALID");
    }
}
