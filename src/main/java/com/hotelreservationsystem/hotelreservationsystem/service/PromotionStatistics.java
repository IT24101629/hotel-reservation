package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.Promotion;
import com.hotelreservationsystem.hotelreservationsystem.model.PromotionUsage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PromotionStatistics {
    private Promotion promotion;
    private Integer totalUsageCount;
    private BigDecimal totalDiscountGiven;
    private List<PromotionUsage> usageHistory;
}