package com.hotelreservationsystem.hotelreservationsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long promotionId;

    @NotBlank(message = "Promo code is required")
    @Size(max = 20, message = "Promo code must not exceed 20 characters")
    @Column(name = "promo_code", unique = true, nullable = false)
    private String promoCode;

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Discount type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", message = "Minimum booking amount must be non-negative")
    @Column(name = "minimum_booking_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal minimumBookingAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Maximum discount must be non-negative")
    @Column(name = "maximum_discount", precision = 10, scale = 2)
    private BigDecimal maximumDiscount;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Size(max = 500, message = "Image path must not exceed 500 characters")
    @Column(name = "image_path")
    private String imagePath;

    @Column(name = "terms_conditions", columnDefinition = "TEXT")
    private String termsConditions;

    @Column(name = "applicable_room_types", columnDefinition = "JSON")
    private String applicableRoomTypes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT
    }

    // Validation method to ensure end date is after start date
    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // Let other validations handle null checks
        }
        return endDate.isAfter(startDate);
    }

    // Helper method to check if promotion is currently active
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return isActive != null && isActive &&
               startDate != null && !now.isBefore(startDate) &&
               endDate != null && !now.isAfter(endDate);
    }

    // Helper method to check if promotion has usage limit
    public boolean hasUsageLimit() {
        return usageLimit != null && usageLimit > 0;
    }

    // Helper method to check if promotion usage is exhausted
    public boolean isUsageExhausted() {
        return hasUsageLimit() && usageCount != null && usageCount >= usageLimit;
    }

    // Helper method to get remaining uses
    public Integer getRemainingUses() {
        if (!hasUsageLimit()) {
            return null; // Unlimited
        }
        return Math.max(0, usageLimit - (usageCount != null ? usageCount : 0));
    }

    // Helper method to calculate discount amount
    public BigDecimal calculateDiscount(BigDecimal bookingAmount) {
        if (bookingAmount == null || bookingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Check minimum booking amount
        if (minimumBookingAmount != null && bookingAmount.compareTo(minimumBookingAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount;
        if (discountType == DiscountType.PERCENTAGE) {
            discountAmount = bookingAmount.multiply(discountValue).divide(new BigDecimal("100"));

            // Apply maximum discount limit if set
            if (maximumDiscount != null && discountAmount.compareTo(maximumDiscount) > 0) {
                discountAmount = maximumDiscount;
            }
        } else {
            discountAmount = discountValue;
        }

        // Ensure discount doesn't exceed booking amount
        if (discountAmount.compareTo(bookingAmount) > 0) {
            discountAmount = bookingAmount;
        }

        return discountAmount;
    }
}