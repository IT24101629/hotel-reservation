package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    /**
     * Find promotion by promo code
     */
    Optional<Promotion> findByPromoCode(String promoCode);

    /**
     * Find all active promotions
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true")
    List<Promotion> findAllActivePromotions();

    /**
     * Find all currently valid promotions (active and within date range)
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findAllCurrentlyValidPromotions(@Param("now") LocalDateTime now);

    /**
     * Find active promotion by promo code
     */
    @Query("SELECT p FROM Promotion p WHERE p.promoCode = :promoCode AND p.isActive = true")
    Optional<Promotion> findActivePromotionByPromoCode(@Param("promoCode") String promoCode);

    /**
     * Find valid promotion by promo code (active and within date range)
     */
    @Query("SELECT p FROM Promotion p WHERE p.promoCode = :promoCode AND p.isActive = true " +
           "AND p.startDate <= :now AND p.endDate >= :now")
    Optional<Promotion> findValidPromotionByPromoCode(@Param("promoCode") String promoCode,
                                                     @Param("now") LocalDateTime now);

    /**
     * Find promotions by date range
     */
    @Query("SELECT p FROM Promotion p WHERE p.startDate <= :endDate AND p.endDate >= :startDate")
    List<Promotion> findPromotionsByDateRange(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    /**
     * Find promotions created by user
     */
    @Query("SELECT p FROM Promotion p WHERE p.createdBy.userId = :userId ORDER BY p.createdAt DESC")
    List<Promotion> findPromotionsByCreatedBy(@Param("userId") Long userId);

    /**
     * Find expired promotions
     */
    @Query("SELECT p FROM Promotion p WHERE p.endDate < :now")
    List<Promotion> findExpiredPromotions(@Param("now") LocalDateTime now);

    /**
     * Find promotions expiring soon
     */
    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.endDate BETWEEN :now AND :expiryDate")
    List<Promotion> findPromotionsExpiringSoon(@Param("now") LocalDateTime now,
                                              @Param("expiryDate") LocalDateTime expiryDate);

    /**
     * Find promotions with usage limit reached
     */
    @Query("SELECT p FROM Promotion p WHERE p.usageLimit IS NOT NULL AND p.usageCount >= p.usageLimit")
    List<Promotion> findPromotionsWithUsageLimitReached();

    /**
     * Increment usage count
     */
    @Modifying
    @Query("UPDATE Promotion p SET p.usageCount = p.usageCount + 1 WHERE p.promotionId = :promotionId")
    void incrementUsageCount(@Param("promotionId") Long promotionId);

    /**
     * Get promotions with pagination and search
     */
    @Query("SELECT p FROM Promotion p WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.promoCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Promotion> findPromotionsWithSearch(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count active promotions
     */
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.isActive = true")
    Long countActivePromotions();

    /**
     * Count active promotions (simple method name)
     */
    long countByIsActiveTrue();

    /**
     * Count currently valid promotions
     */
    @Query("SELECT COUNT(p) FROM Promotion p WHERE p.isActive = true AND p.startDate <= :now AND p.endDate >= :now")
    Long countCurrentlyValidPromotions(@Param("now") LocalDateTime now);

    /**
     * Get total discount amount given by a promotion
     */
    @Query("SELECT COALESCE(SUM(pu.discountApplied), 0) FROM PromotionUsage pu WHERE pu.promotion.promotionId = :promotionId")
    Double getTotalDiscountGivenByPromotion(@Param("promotionId") Long promotionId);

    /**
     * Check if promo code already exists (for creation/update validation)
     */
    @Query("SELECT COUNT(p) > 0 FROM Promotion p WHERE p.promoCode = :promoCode AND (:excludeId IS NULL OR p.promotionId != :excludeId)")
    boolean existsByPromoCodeExcludingId(@Param("promoCode") String promoCode, @Param("excludeId") Long excludeId);
}