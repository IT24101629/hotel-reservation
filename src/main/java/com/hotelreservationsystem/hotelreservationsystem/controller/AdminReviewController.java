package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.Review;
import com.hotelreservationsystem.hotelreservationsystem.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
@Slf4j
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * Show all reviews with search and filters
     */
    @GetMapping
    public String showAllReviews(@RequestParam(required = false) String search,
                                 @RequestParam(required = false) Integer rating,
                                 @RequestParam(required = false) Boolean approved,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 Model model) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Review> reviewsPage = reviewService.searchReviews(search, rating, approved, pageable);

            model.addAttribute("reviews", reviewsPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", reviewsPage.getTotalPages());
            model.addAttribute("totalItems", reviewsPage.getTotalElements());
            model.addAttribute("searchTerm", search);
            model.addAttribute("filterRating", rating);
            model.addAttribute("filterApproved", approved);

            // Get pending reviews count
            List<Review> pendingReviews = reviewService.getPendingReviews();
            model.addAttribute("pendingCount", pendingReviews.size());

            return "admin/reviews/list";
        } catch (Exception e) {
            log.error("Error loading reviews", e);
            model.addAttribute("error", "Error loading reviews: " + e.getMessage());
            return "admin/reviews/list";
        }
    }

    /**
     * Show pending reviews
     */
    @GetMapping("/pending")
    public String showPendingReviews(Model model) {
        try {
            List<Review> pendingReviews = reviewService.getPendingReviews();
            model.addAttribute("reviews", pendingReviews);
            model.addAttribute("isPendingView", true);

            return "admin/reviews/list";
        } catch (Exception e) {
            log.error("Error loading pending reviews", e);
            model.addAttribute("error", "Error loading pending reviews: " + e.getMessage());
            return "admin/reviews/list";
        }
    }

    /**
     * Show review detail
     */
    @GetMapping("/{reviewId}")
    public String showReviewDetail(@PathVariable Long reviewId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<Review> reviewOpt = reviewService.getReviewById(reviewId);
            if (reviewOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Review not found");
                return "redirect:/admin/reviews";
            }

            Review review = reviewOpt.get();
            model.addAttribute("review", review);

            // Get room statistics
            Map<String, Object> roomStats = reviewService.getRoomReviewStatistics(review.getRoom().getRoomId());
            model.addAttribute("roomStats", roomStats);

            return "admin/reviews/detail";
        } catch (Exception e) {
            log.error("Error loading review detail", e);
            redirectAttributes.addFlashAttribute("error", "Error loading review: " + e.getMessage());
            return "redirect:/admin/reviews";
        }
    }

    /**
     * Approve a review
     */
    @PostMapping("/approve/{reviewId}")
    @ResponseBody
    public ResponseEntity<?> approveReview(@PathVariable Long reviewId) {
        try {
            reviewService.approveReview(reviewId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review approved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving review", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Reject a review
     */
    @PostMapping("/reject/{reviewId}")
    @ResponseBody
    public ResponseEntity<?> rejectReview(@PathVariable Long reviewId) {
        try {
            reviewService.rejectReview(reviewId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Review rejected successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting review", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Add admin response to a review
     */
    @PostMapping("/respond/{reviewId}")
    public String addAdminResponse(@PathVariable Long reviewId,
                                   @RequestParam String response,
                                   RedirectAttributes redirectAttributes) {
        try {
            reviewService.addAdminResponse(reviewId, response);

            redirectAttributes.addFlashAttribute("success", "Response added successfully");
            return "redirect:/admin/reviews/" + reviewId;
        } catch (Exception e) {
            log.error("Error adding admin response", e);
            redirectAttributes.addFlashAttribute("error", "Error adding response: " + e.getMessage());
            return "redirect:/admin/reviews/" + reviewId;
        }
    }

    /**
     * Delete a review
     */
    @PostMapping("/delete/{reviewId}")
    public String deleteReview(@PathVariable Long reviewId, RedirectAttributes redirectAttributes) {
        try {
            reviewService.adminDeleteReview(reviewId);

            redirectAttributes.addFlashAttribute("success", "Review deleted successfully");
            return "redirect:/admin/reviews";
        } catch (Exception e) {
            log.error("Error deleting review", e);
            redirectAttributes.addFlashAttribute("error", "Error deleting review: " + e.getMessage());
            return "redirect:/admin/reviews";
        }
    }

    /**
     * API: Bulk approve reviews
     */
    @PostMapping("/bulk-approve")
    @ResponseBody
    public ResponseEntity<?> bulkApproveReviews(@RequestParam List<Long> reviewIds) {
        try {
            int successCount = 0;
            for (Long reviewId : reviewIds) {
                try {
                    reviewService.approveReview(reviewId);
                    successCount++;
                } catch (Exception e) {
                    log.error("Error approving review " + reviewId, e);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", successCount + " reviews approved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in bulk approve", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API: Bulk reject reviews
     */
    @PostMapping("/bulk-reject")
    @ResponseBody
    public ResponseEntity<?> bulkRejectReviews(@RequestParam List<Long> reviewIds) {
        try {
            int successCount = 0;
            for (Long reviewId : reviewIds) {
                try {
                    reviewService.rejectReview(reviewId);
                    successCount++;
                } catch (Exception e) {
                    log.error("Error rejecting review " + reviewId, e);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", successCount + " reviews rejected successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in bulk reject", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * API: Bulk delete reviews
     */
    @PostMapping("/bulk-delete")
    @ResponseBody
    public ResponseEntity<?> bulkDeleteReviews(@RequestParam List<Long> reviewIds) {
        try {
            int successCount = 0;
            for (Long reviewId : reviewIds) {
                try {
                    reviewService.adminDeleteReview(reviewId);
                    successCount++;
                } catch (Exception e) {
                    log.error("Error deleting review " + reviewId, e);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", successCount + " reviews deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in bulk delete", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get reviews by room (for viewing room-specific reviews)
     */
    @GetMapping("/room/{roomId}")
    public String getReviewsByRoom(@PathVariable Long roomId, Model model, RedirectAttributes redirectAttributes) {
        try {
            List<Review> reviews = reviewService.getAllReviewsByRoom(roomId);
            Map<String, Object> roomStats = reviewService.getRoomReviewStatistics(roomId);

            model.addAttribute("reviews", reviews);
            model.addAttribute("roomStats", roomStats);
            model.addAttribute("roomId", roomId);
            model.addAttribute("isRoomView", true);

            return "admin/reviews/list";
        } catch (Exception e) {
            log.error("Error loading room reviews", e);
            redirectAttributes.addFlashAttribute("error", "Error loading room reviews: " + e.getMessage());
            return "redirect:/admin/reviews";
        }
    }
}
