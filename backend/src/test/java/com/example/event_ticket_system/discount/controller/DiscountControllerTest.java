package com.example.event_ticket_system.discount.controller;

import com.example.event_ticket_system.controller.DiscountController;
import com.example.event_ticket_system.dto.response.DiscountResponseDto;
import com.example.event_ticket_system.enums.DiscountType;
import com.example.event_ticket_system.exception.GlobalExceptionHandler;
import com.example.event_ticket_system.service.DiscountService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DiscountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private DiscountController discountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(discountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getDiscountByCode_ShouldReturnDiscountDtoWithoutSensitiveFields_WhenCodeExists() throws Exception {
        String code = "DISCOUNT10";

        DiscountResponseDto dto = DiscountResponseDto.builder()
                .discountId(1)
                .code(code)
                .description("Giảm 10%")
                .discountType(DiscountType.percentage)
                .value(new BigDecimal("10.00"))
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusDays(10))
                .maxUsage(100)
                .eventId(42)
                .eventName("Summer Concert")
                .build();

        when(discountService.getDiscountByCode(code)).thenReturn(dto);

        mockMvc.perform(get("/api/discounts/{code}", code)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Discount fetched successfully"))
                .andExpect(jsonPath("$.data.code").value("DISCOUNT10"))
                .andExpect(jsonPath("$.data.description").value("Giảm 10%"))
                .andExpect(jsonPath("$.data.discountType").value("percentage"))
                .andExpect(jsonPath("$.data.value").value(10.0))
                .andExpect(jsonPath("$.data.eventId").value(42))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("organizer"))));
    }

    @Test
    void getDiscountByCode_ShouldReturn404_WhenCodeNotFound() throws Exception {
        String code = "INVALID";
        when(discountService.getDiscountByCode(code))
                .thenThrow(new EntityNotFoundException("Discount not found with code: " + code));

        mockMvc.perform(get("/api/discounts/{code}", code))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Discount not found with code: " + code));
    }

    @Test
    void getDiscountByCode_ShouldReturn500_WhenUnexpectedError() throws Exception {
        String code = "ERROR";
        when(discountService.getDiscountByCode(code))
                .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/discounts/{code}", code))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
