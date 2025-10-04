package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.Promotion;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.repository.UserRepository;
import com.hotelreservationsystem.hotelreservationsystem.service.PromotionService;
import com.hotelreservationsystem.hotelreservationsystem.service.PromoValidationResult;
import com.hotelreservationsystem.hotelreservationsystem.service.PromotionStatistics;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final PromotionService promotionService;
    private final UserRepository userRepository;

    /**
     * Display promotions list with pagination and search
     */
    @GetMapping
    public String listPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Model model) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Promotion> promotionsPage = promotionService.getAllPromotions(search.trim().isEmpty() ? null : search, pageable);
        List<Promotion> expiringSoonPromotions = promotionService.getPromotionsExpiringSoon(7); // Next 7 days

        model.addAttribute("promotionsPage", promotionsPage);
        model.addAttribute("expiringSoonPromotions", expiringSoonPromotions);
        model.addAttribute("search", search);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        // Statistics
        model.addAttribute("activePromotionsCount", promotionService.getAllActivePromotions().size());
        model.addAttribute("validPromotionsCount", promotionService.getAllCurrentlyValidPromotions().size());

        return "admin/promotions/list";
    }

    /**
     * Show create promotion form
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("promotion", new Promotion());
        model.addAttribute("discountTypes", Promotion.DiscountType.values());
        return "admin/promotions/create";
    }

    /**
     * Handle create promotion form submission
     */
    @PostMapping("/create")
    public String createPromotion(
            @Valid @ModelAttribute("promotion") Promotion promotion,
            BindingResult bindingResult,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("discountTypes", Promotion.DiscountType.values());
            return "admin/promotions/create";
        }

        try {
            // Get current admin user from Spring Security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            Optional<User> adminUser = userRepository.findByUsername(username);

            if (adminUser.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Unable to identify admin user");
                return "redirect:/admin/promotions";
            }

            promotion.setCreatedBy(adminUser.get());

            // Validate dates
            if (promotion.getStartDate().isAfter(promotion.getEndDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date must be after start date");
                model.addAttribute("discountTypes", Promotion.DiscountType.values());
                return "admin/promotions/create";
            }

            // Create promotion
            Promotion savedPromotion = promotionService.createPromotion(promotion, image);

            log.info("Admin {} created promotion: {}", username, savedPromotion.getPromoCode());
            redirectAttributes.addFlashAttribute("success", "Promotion '" + savedPromotion.getTitle() + "' created successfully!");

        } catch (Exception e) {
            log.error("Error creating promotion", e);
            redirectAttributes.addFlashAttribute("error", "Error creating promotion: " + e.getMessage());
        }

        return "redirect:/admin/promotions";
    }

    /**
     * Show edit promotion form
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);

        if (promotionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Promotion not found");
            return "redirect:/admin/promotions";
        }

        model.addAttribute("promotion", promotionOpt.get());
        model.addAttribute("discountTypes", Promotion.DiscountType.values());
        return "admin/promotions/edit";
    }

    /**
     * Handle edit promotion form submission
     */
    @PostMapping("/edit/{id}")
    public String editPromotion(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("promotion") Promotion promotion,
            BindingResult bindingResult,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("discountTypes", Promotion.DiscountType.values());
            return "admin/promotions/edit";
        }

        try {
            // Validate dates
            if (promotion.getStartDate().isAfter(promotion.getEndDate())) {
                bindingResult.rejectValue("endDate", "error.endDate", "End date must be after start date");
                model.addAttribute("discountTypes", Promotion.DiscountType.values());
                return "admin/promotions/edit";
            }

            Promotion updatedPromotion = promotionService.updatePromotion(id, promotion, image);

            log.info("Updated promotion: {}", updatedPromotion.getPromoCode());
            redirectAttributes.addFlashAttribute("success", "Promotion '" + updatedPromotion.getTitle() + "' updated successfully!");

        } catch (Exception e) {
            log.error("Error updating promotion", e);
            redirectAttributes.addFlashAttribute("error", "Error updating promotion: " + e.getMessage());
        }

        return "redirect:/admin/promotions";
    }

    /**
     * View promotion details
     */
    @GetMapping("/view/{id}")
    public String viewPromotion(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);

        if (promotionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Promotion not found");
            return "redirect:/admin/promotions";
        }

        Promotion promotion = promotionOpt.get();
        model.addAttribute("promotion", promotion);

        // Get promotion statistics
        try {
            model.addAttribute("promotionStats", promotionService.getPromotionStatistics(id));
        } catch (Exception e) {
            log.error("Error getting promotion statistics", e);
            model.addAttribute("statsError", "Unable to load promotion statistics");
        }

        return "admin/promotions/view";
    }

    /**
     * Toggle promotion status (activate/deactivate)
     */
    @PostMapping("/toggle-status/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> togglePromotionStatus(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            Promotion updatedPromotion = promotionService.togglePromotionStatus(id);
            response.put("success", true);
            response.put("newStatus", updatedPromotion.getIsActive());
            response.put("message", updatedPromotion.getIsActive() ? "Promotion activated" : "Promotion deactivated");

            log.info("Toggled promotion status: {} - {}", id, updatedPromotion.getIsActive());

        } catch (Exception e) {
            log.error("Error toggling promotion status", e);
            response.put("success", false);
            response.put("message", "Error updating promotion status: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Delete promotion
     */
    @PostMapping("/delete/{id}")
    public String deletePromotion(
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        try {
            Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
            if (promotionOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Promotion not found");
                return "redirect:/admin/promotions";
            }

            Promotion promotion = promotionOpt.get();
            promotionService.deletePromotion(id);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            log.info("Admin {} deleted promotion: {}", username, promotion.getPromoCode());

            redirectAttributes.addFlashAttribute("success", "Promotion '" + promotion.getTitle() + "' deleted successfully!");

        } catch (Exception e) {
            log.error("Error deleting promotion", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting promotion: " + e.getMessage());
        }

        return "redirect:/admin/promotions";
    }

    /**
     * Validate promo code (AJAX endpoint)
     */
    @PostMapping("/validate-promo-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validatePromoCode(
            @RequestParam("promoCode") String promoCode,
            @RequestParam("bookingAmount") BigDecimal bookingAmount,
            @RequestParam("customerId") Long customerId) {

        Map<String, Object> response = new HashMap<>();

        try {
            var validationResult = promotionService.validatePromoCode(promoCode, bookingAmount, customerId);

            if (validationResult.isValid()) {
                response.put("valid", true);
                response.put("discountAmount", validationResult.getDiscountAmount());
                response.put("newTotal", bookingAmount.subtract(validationResult.getDiscountAmount()));
                response.put("promotion", validationResult.getPromotion());
            } else {
                response.put("valid", false);
                response.put("message", validationResult.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Error validating promo code", e);
            response.put("valid", false);
            response.put("message", "Error validating promo code");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get promotion statistics (AJAX endpoint)
     */
    @GetMapping("/stats/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPromotionStats(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();

        try {
            var stats = promotionService.getPromotionStatistics(id);
            response.put("success", true);
            response.put("stats", stats);

        } catch (Exception e) {
            log.error("Error getting promotion statistics", e);
            response.put("success", false);
            response.put("message", "Error loading statistics");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Export promotions data
     */
    @GetMapping("/export")
    public String exportPromotions(Model model) {
        // For now, just redirect to list. In future, implement CSV/Excel export
        return "redirect:/admin/promotions";
    }

    /**
     * Bulk operations on promotions
     */
    @PostMapping("/bulk-action")
    public String bulkAction(
            @RequestParam("action") String action,
            @RequestParam("promotionIds") List<Long> promotionIds,
            RedirectAttributes redirectAttributes) {

        if (promotionIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No promotions selected");
            return "redirect:/admin/promotions";
        }

        try {
            int successCount = 0;
            switch (action.toLowerCase()) {
                case "activate":
                    for (Long id : promotionIds) {
                        Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
                        if (promotionOpt.isPresent() && !promotionOpt.get().getIsActive()) {
                            promotionService.togglePromotionStatus(id);
                            successCount++;
                        }
                    }
                    redirectAttributes.addFlashAttribute("success", successCount + " promotion(s) activated");
                    break;

                case "deactivate":
                    for (Long id : promotionIds) {
                        Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
                        if (promotionOpt.isPresent() && promotionOpt.get().getIsActive()) {
                            promotionService.togglePromotionStatus(id);
                            successCount++;
                        }
                    }
                    redirectAttributes.addFlashAttribute("success", successCount + " promotion(s) deactivated");
                    break;

                case "delete":
                    for (Long id : promotionIds) {
                        promotionService.deletePromotion(id);
                        successCount++;
                    }
                    redirectAttributes.addFlashAttribute("success", successCount + " promotion(s) deleted");
                    break;

                default:
                    redirectAttributes.addFlashAttribute("error", "Unknown bulk action: " + action);
                    break;
            }

        } catch (Exception e) {
            log.error("Error performing bulk action", e);
            redirectAttributes.addFlashAttribute("error", "Error performing bulk action: " + e.getMessage());
        }

        return "redirect:/admin/promotions";
    }
}