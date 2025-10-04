package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.Customer;
import com.hotelreservationsystem.hotelreservationsystem.model.Review;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.repository.CustomerRepository;
import com.hotelreservationsystem.hotelreservationsystem.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;
    private final CustomerRepository customerRepository;

    /**
     * Show all reviews by the logged-in customer
     */
    @GetMapping("/my-reviews")
    public String showMyReviews(Authentication authentication,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        try {
            Customer customer = getAuthenticatedCustomer(authentication);

            Pageable pageable = PageRequest.of(page, size);
            Page<Review> reviewsPage = reviewService.getReviewsByCustomerPaginated(customer.getCustomerId(), pageable);

            model.addAttribute("reviews", reviewsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reviewsPage.getTotalPages());
            model.addAttribute("totalItems", reviewsPage.getTotalElements());

            return "my-reviews";
        } catch (Exception e) {
            log.error("Error loading my reviews", e);
            model.addAttribute("error", "Error loading reviews: " + e.getMessage());
            return "my-reviews";
        }
    }

    /**
     * Show form to add a new review
     */
    @GetMapping("/add")
    public String showAddReviewForm(@RequestParam Long roomId,
                                    @RequestParam(required = false) Long bookingId,
                                    Authentication authentication,
                                    Model model) {
        try {
            Customer customer = getAuthenticatedCustomer(authentication);

            // Check if customer can review this room
            if (!reviewService.canCustomerReviewRoom(customer.getCustomerId(), roomId)) {
                model.addAttribute("error", "You have already reviewed this room.");
                return "redirect:/rooms/" + roomId;
            }

            Review review = new Review();
            model.addAttribute("review", review);
            model.addAttribute("roomId", roomId);
            model.addAttribute("bookingId", bookingId);

            return "review-form";
        } catch (Exception e) {
            log.error("Error showing add review form", e);
            return "redirect:/rooms/" + roomId + "?error=" + e.getMessage();
        }
    }

    /**
     * Handle new review submission
     */
    @PostMapping("/add")
    public String addReview(@ModelAttribute Review review,
                           @RequestParam Long roomId,
                           @RequestParam(required = false) Long bookingId,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            Customer customer = getAuthenticatedCustomer(authentication);

            reviewService.createReview(review, customer.getCustomerId(), roomId, bookingId);

            redirectAttributes.addFlashAttribute("success", "Review added successfully!");
            return "redirect:/reviews/my-reviews";
        } catch (Exception e) {
            log.error("Error adding review", e);
            redirectAttributes.addFlashAttribute("error", "Error adding review: " + e.getMessage());
            return "redirect:/reviews/add?roomId=" + roomId + (bookingId != null ? "&bookingId=" + bookingId : "");
        }
    }

    /**
     * Show form to edit an existing review
     */
    @GetMapping("/edit/{reviewId}")
    public String showEditReviewForm(@PathVariable Long reviewId,
                                     Authentication authentication,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        try {
            Customer customer = getAuthenticatedCustomer(authentication);

            Optional<Review> reviewOpt = reviewService.getReviewById(reviewId);
            if (reviewOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Review not found");
                return "redirect:/reviews/my-reviews";
            }

            Review review = reviewOpt.get();

            // Verify ownership
            if (!review.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                redirectAttributes.addFlashAttribute("error", "You can only edit your own reviews");
                return "redirect:/reviews/my-reviews";
            }

            // Check if can still edit
            if (!review.canEdit()) {
                redirectAttributes.addFlashAttribute("error", "Reviews can only be edited within 24 hours of creation");
                return "redirect:/reviews/my-reviews";
            }

            model.addAttribute("review", review);
            model.addAttribute("isEdit", true);

            return "review-form";
        } catch (Exception e) {
            log.error("Error showing edit review form", e);
            redirectAttributes.addFlashAttribute("error", "Error loading review: " + e.getMessage());
            return "redirect:/reviews/my-reviews";
        }
    }

    /**
     * Handle review update
     */
    @PostMapping("/edit/{reviewId}")
    public String updateReview(@PathVariable Long reviewId,
                              @ModelAttribute Review reviewDetails,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            Customer customer = getAuthenticatedCustomer(authentication);

            reviewService.updateReview(reviewId, reviewDetails, customer.getCustomerId());

            redirectAttributes.addFlashAttribute("success", "Review updated successfully!");
            return "redirect:/reviews/my-reviews";
        } catch (Exception e) {
            log.error("Error updating review", e);
            redirectAttributes.addFlashAttribute("error", "Error updating review: " + e.getMessage());
            return "redirect:/reviews/edit/" + reviewId;
        }
    }

    /**
     * Delete a review
     */
    @PostMapping("/delete/{reviewId}")
    public String deleteReview(@PathVariable Long reviewId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            Customer customer = getAuthenticatedCustomer(authentication);

            reviewService.deleteReview(reviewId, customer.getCustomerId());

            redirectAttributes.addFlashAttribute("success", "Review deleted successfully!");
            return "redirect:/reviews/my-reviews";
        } catch (Exception e) {
            log.error("Error deleting review", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting review: " + e.getMessage());
            return "redirect:/reviews/my-reviews";
        }
    }

    /**
     * API: Get reviews for a specific room
     */
    @GetMapping("/api/room/{roomId}")
    @ResponseBody
    public ResponseEntity<?> getRoomReviews(@PathVariable Long roomId) {
        try {
            List<Review> reviews = reviewService.getApprovedReviewsByRoom(roomId);
            Map<String, Object> stats = reviewService.getRoomReviewStatistics(roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("reviews", reviews);
            response.put("statistics", stats);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting room reviews", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * API: Check if customer can review a room
     */
    @GetMapping("/api/can-review")
    @ResponseBody
    public ResponseEntity<?> canReviewRoom(@RequestParam Long roomId,
                                          Authentication authentication) {
        try {
            Customer customer = getAuthenticatedCustomer(authentication);
            boolean canReview = reviewService.canCustomerReviewRoom(customer.getCustomerId(), roomId);

            Map<String, Object> response = new HashMap<>();
            response.put("canReview", canReview);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking review eligibility", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Helper method to get authenticated customer
     */
    private Customer getAuthenticatedCustomer(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        // Get username from Spring Security UserDetails
        String username = authentication.getName();

        // Find user by email (username)
        User user = customerRepository.findByUser_Email(username)
                .map(Customer::getUser)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));

        return customerRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Customer profile not found for user"));
    }
}
