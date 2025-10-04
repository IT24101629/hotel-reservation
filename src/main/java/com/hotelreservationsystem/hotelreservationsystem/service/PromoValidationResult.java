package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.Promotion;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromoValidationResult {
    private boolean valid;
    private String errorMessage;
    private Promotion promotion;
    private BigDecimal discountAmount;
}