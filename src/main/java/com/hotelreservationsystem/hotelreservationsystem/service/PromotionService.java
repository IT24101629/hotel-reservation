package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.PromotionRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.PromotionUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;

    private static final String UPLOAD_DIR = "uploads/promotions/";
    private static final String WEB_UPLOAD_DIR = "/images/promotions/";

    /**
     * Create a new promotion
     */
    public Promotion createPromotion(Promotion promotion, MultipartFile image) {
        log.info("Creating new promotion with code: {}", promotion.getPromoCode());

        // Validate promo code uniqueness
        if (promotionRepository.existsByPromoCodeExcludingId(promotion.getPromoCode(), null)) {
            throw new IllegalArgumentException("Promotion code already exists: " + promotion.getPromoCode());
        }

        // Handle image upload
        if (image != null && !image.isEmpty()) {
            String imagePath = savePromotionImage(image);
            promotion.setImagePath(imagePath);
        }

        Promotion savedPromotion = promotionRepository.save(promotion);
        log.info("Created promotion with ID: {}", savedPromotion.getPromotionId());
        return savedPromotion;
    }

    /**
     * Update an existing promotion
     */
    public Promotion updatePromotion(Long promotionId, Promotion promotionDetails, MultipartFile image) {
        log.info("Updating promotion with ID: {}", promotionId);

        Promotion existingPromotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + promotionId));

        // Validate promo code uniqueness (excluding current promotion)
        if (!existingPromotion.getPromoCode().equals(promotionDetails.getPromoCode()) &&
            promotionRepository.existsByPromoCodeExcludingId(promotionDetails.getPromoCode(), promotionId)) {
            throw new IllegalArgumentException("Promotion code already exists: " + promotionDetails.getPromoCode());
        }

        // Update promotion details
        existingPromotion.setPromoCode(promotionDetails.getPromoCode());
        existingPromotion.setTitle(promotionDetails.getTitle());
        existingPromotion.setDescription(promotionDetails.getDescription());
        existingPromotion.setDiscountType(promotionDetails.getDiscountType());
        existingPromotion.setDiscountValue(promotionDetails.getDiscountValue());
        existingPromotion.setMinimumBookingAmount(promotionDetails.getMinimumBookingAmount());
        existingPromotion.setMaximumDiscount(promotionDetails.getMaximumDiscount());
        existingPromotion.setStartDate(promotionDetails.getStartDate());
        existingPromotion.setEndDate(promotionDetails.getEndDate());
        existingPromotion.setUsageLimit(promotionDetails.getUsageLimit());
        existingPromotion.setIsActive(promotionDetails.getIsActive());
        existingPromotion.setTermsConditions(promotionDetails.getTermsConditions());
        existingPromotion.setApplicableRoomTypes(promotionDetails.getApplicableRoomTypes());

        // Handle image upload
        if (image != null && !image.isEmpty()) {
            // Delete old image if exists
            if (existingPromotion.getImagePath() != null) {
                deletePromotionImage(existingPromotion.getImagePath());
            }
            String imagePath = savePromotionImage(image);
            existingPromotion.setImagePath(imagePath);
        }

        Promotion updatedPromotion = promotionRepository.save(existingPromotion);
        log.info("Updated promotion with ID: {}", updatedPromotion.getPromotionId());
        return updatedPromotion;
    }

    /**
     * Get promotion by ID
     */
    @Transactional(readOnly = true)
    public Optional<Promotion> getPromotionById(Long promotionId) {
        return promotionRepository.findById(promotionId);
    }

    /**
     * Get all promotions with pagination and search
     */
    @Transactional(readOnly = true)
    public Page<Promotion> getAllPromotions(String searchTerm, Pageable pageable) {
        return promotionRepository.findPromotionsWithSearch(searchTerm, pageable);
    }

    /**
     * Get all active promotions
     */
    @Transactional(readOnly = true)
    public List<Promotion> getAllActivePromotions() {
        return promotionRepository.findAllActivePromotions();
    }

    /**
     * Get all currently valid promotions
     */
    @Transactional(readOnly = true)
    public List<Promotion> getAllCurrentlyValidPromotions() {
        return promotionRepository.findAllCurrentlyValidPromotions(LocalDateTime.now());
    }

    /**
     * Delete promotion
     */
    public void deletePromotion(Long promotionId) {
        log.info("Deleting promotion with ID: {}", promotionId);

        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + promotionId));

        // Delete associated image
        if (promotion.getImagePath() != null) {
            deletePromotionImage(promotion.getImagePath());
        }

        promotionRepository.deleteById(promotionId);
        log.info("Deleted promotion with ID: {}", promotionId);
    }

    /**
     * Validate promo code for booking
     */
    @Transactional(readOnly = true)
    public PromoValidationResult validatePromoCode(String promoCode, BigDecimal bookingAmount, Long customerId) {
        log.info("Validating promo code: {} for customer: {} with booking amount: {}", promoCode, customerId, bookingAmount);

        Optional<Promotion> promotionOpt = promotionRepository.findValidPromotionByPromoCode(promoCode, LocalDateTime.now());

        if (promotionOpt.isEmpty()) {
            return PromoValidationResult.builder()
                    .valid(false)
                    .errorMessage("Invalid or expired promo code")
                    .build();
        }

        Promotion promotion = promotionOpt.get();

        // Check if promotion is exhausted
        if (promotion.isUsageExhausted()) {
            return PromoValidationResult.builder()
                    .valid(false)
                    .errorMessage("This promo code has reached its usage limit")
                    .build();
        }

        // Check minimum booking amount
        if (promotion.getMinimumBookingAmount() != null &&
            bookingAmount.compareTo(promotion.getMinimumBookingAmount()) < 0) {
            return PromoValidationResult.builder()
                    .valid(false)
                    .errorMessage("Minimum booking amount of LKR " + promotion.getMinimumBookingAmount() + " required")
                    .build();
        }

        // Check for welcome promotion usage
        if (promotion.getPromoCode().startsWith("WELCOME")) {
            Long usageCount = promotionUsageRepository.countUsageByCustomerAndPromoCode(customerId, promoCode);
            if (usageCount > 0) {
                return PromoValidationResult.builder()
                        .valid(false)
                        .errorMessage("This welcome offer can only be used once per customer")
                        .build();
            }
        }

        // Calculate discount
        BigDecimal discountAmount = promotion.calculateDiscount(bookingAmount);

        return PromoValidationResult.builder()
                .valid(true)
                .promotion(promotion)
                .discountAmount(discountAmount)
                .build();
    }

    /**
     * Apply promo code to booking
     */
    public PromotionUsage applyPromoCode(String promoCode, Booking booking, Customer customer) {
        log.info("Applying promo code: {} to booking: {}", promoCode, booking.getBookingId());

        PromoValidationResult validationResult = validatePromoCode(promoCode, booking.getTotalAmount(), customer.getCustomerId());

        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(validationResult.getErrorMessage());
        }

        Promotion promotion = validationResult.getPromotion();
        BigDecimal discountAmount = validationResult.getDiscountAmount();

        // Update booking with promo code and discount
        booking.setPromoCode(promoCode);
        booking.setOriginalAmount(booking.getTotalAmount());
        booking.setDiscountAmount(discountAmount);
        booking.setTotalAmount(booking.getTotalAmount().subtract(discountAmount));

        // Create promotion usage record
        PromotionUsage promotionUsage = PromotionUsage.builder()
                .promotion(promotion)
                .booking(booking)
                .customer(customer)
                .discountApplied(discountAmount)
                .build();

        PromotionUsage savedUsage = promotionUsageRepository.save(promotionUsage);

        // Increment promotion usage count
        promotionRepository.incrementUsageCount(promotion.getPromotionId());

        log.info("Applied promo code: {} with discount: {} to booking: {}", promoCode, discountAmount, booking.getBookingId());
        return savedUsage;
    }

    /**
     * Get promotion statistics
     */
    @Transactional(readOnly = true)
    public PromotionStatistics getPromotionStatistics(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + promotionId));

        BigDecimal totalDiscountGiven = promotionUsageRepository.getTotalDiscountByPromotion(promotionId);
        List<PromotionUsage> usageHistory = promotionUsageRepository.findByPromotionPromotionId(promotionId);

        return PromotionStatistics.builder()
                .promotion(promotion)
                .totalUsageCount(promotion.getUsageCount())
                .totalDiscountGiven(totalDiscountGiven)
                .usageHistory(usageHistory)
                .build();
    }

    /**
     * Save promotion image
     */
    private String savePromotionImage(MultipartFile image) {
        try {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = image.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            // Save file
            Path filePath = uploadPath.resolve(filename);
            Files.copy(image.getInputStream(), filePath);

            log.info("Saved promotion image: {}", filename);
            return WEB_UPLOAD_DIR + filename;

        } catch (IOException e) {
            log.error("Error saving promotion image", e);
            throw new RuntimeException("Failed to save promotion image", e);
        }
    }

    /**
     * Delete promotion image
     */
    private void deletePromotionImage(String imagePath) {
        try {
            if (imagePath.startsWith(WEB_UPLOAD_DIR)) {
                String filename = imagePath.substring(WEB_UPLOAD_DIR.length());
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                Files.deleteIfExists(filePath);
                log.info("Deleted promotion image: {}", filename);
            }
        } catch (IOException e) {
            log.error("Error deleting promotion image: {}", imagePath, e);
        }
    }

    /**
     * Toggle promotion status
     */
    public Promotion togglePromotionStatus(Long promotionId) {
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with ID: " + promotionId));

        promotion.setIsActive(!promotion.getIsActive());
        return promotionRepository.save(promotion);
    }

    /**
     * Get promotions expiring soon
     */
    @Transactional(readOnly = true)
    public List<Promotion> getPromotionsExpiringSoon(int days) {
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(days);
        return promotionRepository.findPromotionsExpiringSoon(LocalDateTime.now(), expiryDate);
    }
}