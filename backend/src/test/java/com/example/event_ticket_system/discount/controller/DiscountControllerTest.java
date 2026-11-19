package com.example.event_ticket_system.discount.controller;

import com.example.event_ticket_system.Controller.DiscountController;
import com.example.event_ticket_system.Entity.Discount;
import com.example.event_ticket_system.Enums.DiscountType;
import com.example.event_ticket_system.Service.DiscountService;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class DiscountControllerTest {
    private MockMvc mockMvc;

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private DiscountController discountController;

    @BeforeEach
    public void setUp() {
        // Initialize MockMvc with the controller
        mockMvc = MockMvcBuilders.standaloneSetup(discountController).build();
    }

    @Test
    void getDiscountByCode_ShouldReturnDiscount_WhenCodeExists() throws Exception {
        String code = "DISCOUNT10";

        Discount discount = new Discount(
                1,
                code,
                "Giảm 10%",
                DiscountType.percentage,
                10.0,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                null, // event = null cho đơn giản, nếu cần có thể mock Event
                100
        );

        when(discountService.getDiscountByCode(code)).thenReturn(discount);

        mockMvc.perform(get("/api/discounts/{code}", code)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Discount fetched successfully"))
                .andExpect(jsonPath("$.data.code").value("DISCOUNT10"))
                .andExpect(jsonPath("$.data.description").value("Giảm 10%"))
                .andExpect(jsonPath("$.data.discountType").value("percentage"))
                .andExpect(jsonPath("$.data.value").value(10.0));
    }

    @Test
    void getDiscountByCode_ShouldReturn404_WhenCodeNotFound() throws Exception {
        String code = "INVALID";
        when(discountService.getDiscountByCode(code))
                .thenThrow(new EntityNotFoundException("Not found"));

        mockMvc.perform(get("/api/discounts/{code}", code))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Discount not found with code: " + code));
    }

    @Test
    void getDiscountByCode_ShouldReturn500_WhenUnexpectedError() throws Exception {
        String code = "ERROR";
        when(discountService.getDiscountByCode(code))
                .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/discounts/{code}", code))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("An unexpected error occurred while fetching the discount"));
    }
}
