package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.PromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionUsageRepository extends JpaRepository<PromotionUsage, Long> {

    /**
     * Find usage by promotion
     */
    List<PromotionUsage> findByPromotionPromotionId(Long promotionId);

    /**
     * Find usage by customer
     */
    List<PromotionUsage> findByCustomerCustomerId(Long customerId);

    /**
     * Find usage by booking
     */
    List<PromotionUsage> findByBookingBookingId(Long bookingId);

    /**
     * Count usage by customer for a specific promotion
     */
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.customer.customerId = :customerId AND pu.promotion.promotionId = :promotionId")
    Long countUsageByCustomerAndPromotion(@Param("customerId") Long customerId, @Param("promotionId") Long promotionId);

    /**
     * Count usage by customer for a specific promo code
     */
    @Query("SELECT COUNT(pu) FROM PromotionUsage pu WHERE pu.customer.customerId = :customerId AND pu.promotion.promoCode = :promoCode")
    Long countUsageByCustomerAndPromoCode(@Param("customerId") Long customerId, @Param("promoCode") String promoCode);

    /**
     * Find usage within date range
     */
    @Query("SELECT pu FROM PromotionUsage pu WHERE pu.usedAt BETWEEN :startDate AND :endDate ORDER BY pu.usedAt DESC")
    List<PromotionUsage> findUsageWithinDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get total discount amount for a promotion
     */
    @Query("SELECT COALESCE(SUM(pu.discountApplied), 0) FROM PromotionUsage pu WHERE pu.promotion.promotionId = :promotionId")
    BigDecimal getTotalDiscountByPromotion(@Param("promotionId") Long promotionId);

    /**
     * Get total discount amount for a customer
     */
    @Query("SELECT COALESCE(SUM(pu.discountApplied), 0) FROM PromotionUsage pu WHERE pu.customer.customerId = :customerId")
    BigDecimal getTotalDiscountByCustomer(@Param("customerId") Long customerId);

    /**
     * Get usage statistics for a date range
     */
    @Query("SELECT COUNT(pu), COALESCE(SUM(pu.discountApplied), 0) FROM PromotionUsage pu WHERE pu.usedAt BETWEEN :startDate AND :endDate")
    Object[] getUsageStatisticsForDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find top used promotions
     */
    @Query("SELECT pu.promotion.promoCode, COUNT(pu), COALESCE(SUM(pu.discountApplied), 0) " +
           "FROM PromotionUsage pu GROUP BY pu.promotion.promoCode ORDER BY COUNT(pu) DESC")
    List<Object[]> findTopUsedPromotions();

    /**
     * Check if customer has used a welcome promotion
     */
    @Query("SELECT COUNT(pu) > 0 FROM PromotionUsage pu " +
           "WHERE pu.customer.customerId = :customerId AND pu.promotion.promoCode LIKE 'WELCOME%'")
    boolean hasCustomerUsedWelcomePromotion(@Param("customerId") Long customerId);
}