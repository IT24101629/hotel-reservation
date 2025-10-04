package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.BookingRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.CustomerRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.ReviewRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final RoomRepository roomRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;

    /**
     * Create a new review
     */
    public Review createReview(Review review, Long customerId, Long roomId, Long bookingId) {
        log.info("Creating review for room: {} by customer: {}", roomId, customerId);

        // Validate customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));

        // Validate room
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        // Check if customer already reviewed this room
        Optional<Review> existingReview = reviewRepository.findByRoomAndCustomer(room, customer);
        if (existingReview.isPresent()) {
            throw new IllegalArgumentException("You have already reviewed this room. You can edit your existing review instead.");
        }

        // Validate rating
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
        }

        // Set customer and room
        review.setCustomer(customer);
        review.setRoom(room);

        // Check if review is from a verified booking
        if (bookingId != null) {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null && booking.getCustomer().equals(customer)) {
                review.setBooking(booking);
                review.setIsVerifiedStay(true);
            }
        }

        // Set default values
        if (review.getIsApproved() == null) {
            review.setIsApproved(true); // Auto-approve by default
        }
        if (review.getIsVerifiedStay() == null) {
            review.setIsVerifiedStay(false);
        }

        Review savedReview = reviewRepository.save(review);
        log.info("Created review with ID: {}", savedReview.getReviewId());
        return savedReview;
    }

    /**
     * Update an existing review
     */
    public Review updateReview(Long reviewId, Review reviewDetails, Long customerId) {
        log.info("Updating review with ID: {} by customer: {}", reviewId, customerId);

        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        // Verify ownership
        if (!existingReview.getCustomer().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only edit your own reviews");
        }

        // Check if review can still be edited (within 24 hours)
        if (!existingReview.canEdit()) {
            throw new IllegalArgumentException("Reviews can only be edited within 24 hours of creation");
        }

        // Validate rating
        if (reviewDetails.getRating() != null) {
            if (reviewDetails.getRating() < 1 || reviewDetails.getRating() > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5 stars");
            }
            existingReview.setRating(reviewDetails.getRating());
        }

        // Update fields
        if (reviewDetails.getTitle() != null) {
            existingReview.setTitle(reviewDetails.getTitle());
        }
        if (reviewDetails.getComment() != null) {
            existingReview.setComment(reviewDetails.getComment());
        }

        Review updatedReview = reviewRepository.save(existingReview);
        log.info("Updated review with ID: {}", updatedReview.getReviewId());
        return updatedReview;
    }

    /**
     * Delete a review
     */
    public void deleteReview(Long reviewId, Long customerId) {
        log.info("Deleting review with ID: {} by customer: {}", reviewId, customerId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        // Verify ownership
        if (!review.getCustomer().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        reviewRepository.deleteById(reviewId);
        log.info("Deleted review with ID: {}", reviewId);
    }

    /**
     * Get review by ID
     */
    @Transactional(readOnly = true)
    public Optional<Review> getReviewById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }

    /**
     * Get all reviews by customer
     */
    @Transactional(readOnly = true)
    public List<Review> getReviewsByCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        return reviewRepository.findByCustomerOrderByCreatedAtDesc(customer);
    }

    /**
     * Get all reviews by customer with pagination
     */
    @Transactional(readOnly = true)
    public Page<Review> getReviewsByCustomerPaginated(Long customerId, Pageable pageable) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found with ID: " + customerId));
        return reviewRepository.findByCustomerOrderByCreatedAtDesc(customer, pageable);
    }

    /**
     * Get all approved reviews for a room
     */
    @Transactional(readOnly = true)
    public List<Review> getApprovedReviewsByRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));
        return reviewRepository.findByRoomAndIsApprovedTrueOrderByCreatedAtDesc(room);
    }

    /**
     * Get all reviews for a room (admin)
     */
    @Transactional(readOnly = true)
    public List<Review> getAllReviewsByRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));
        return reviewRepository.findByRoomOrderByCreatedAtDesc(room);
    }

    /**
     * Get room statistics including reviews
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoomReviewStatistics(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        Map<String, Object> stats = new HashMap<>();

        // Average rating
        Double avgRating = reviewRepository.calculateAverageRatingByRoom(room);
        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        // Total reviews
        Long totalReviews = reviewRepository.countApprovedReviewsByRoom(room);
        stats.put("totalReviews", totalReviews);

        // Rating distribution
        List<Object[]> distribution = reviewRepository.getRatingDistributionByRoom(room);
        Map<Integer, Long> ratingDist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDist.put(i, 0L);
        }
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingDist.put(rating, count);
        }
        stats.put("ratingDistribution", ratingDist);

        return stats;
    }

    /**
     * Admin: Approve a review
     */
    public Review approveReview(Long reviewId) {
        log.info("Approving review with ID: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        review.setIsApproved(true);
        Review savedReview = reviewRepository.save(review);

        log.info("Approved review with ID: {}", reviewId);
        return savedReview;
    }

    /**
     * Admin: Reject/Unapprove a review
     */
    public Review rejectReview(Long reviewId) {
        log.info("Rejecting review with ID: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        review.setIsApproved(false);
        Review savedReview = reviewRepository.save(review);

        log.info("Rejected review with ID: {}", reviewId);
        return savedReview;
    }

    /**
     * Admin: Add response to a review
     */
    public Review addAdminResponse(Long reviewId, String response) {
        log.info("Adding admin response to review with ID: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found with ID: " + reviewId));

        review.setAdminResponse(response);
        review.setRespondedAt(LocalDateTime.now());
        Review savedReview = reviewRepository.save(review);

        log.info("Added admin response to review with ID: {}", reviewId);
        return savedReview;
    }

    /**
     * Admin: Delete any review
     */
    public void adminDeleteReview(Long reviewId) {
        log.info("Admin deleting review with ID: {}", reviewId);

        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("Review not found with ID: " + reviewId);
        }

        reviewRepository.deleteById(reviewId);
        log.info("Admin deleted review with ID: {}", reviewId);
    }

    /**
     * Admin: Search and filter reviews
     */
    @Transactional(readOnly = true)
    public Page<Review> searchReviews(String searchTerm, Integer rating, Boolean approved, Pageable pageable) {
        return reviewRepository.searchReviews(searchTerm, rating, approved, pageable);
    }

    /**
     * Get all pending reviews for admin
     */
    @Transactional(readOnly = true)
    public List<Review> getPendingReviews() {
        return reviewRepository.findByIsApprovedFalseOrderByCreatedAtDesc();
    }

    /**
     * Get recent reviews
     */
    @Transactional(readOnly = true)
    public List<Review> getRecentReviews(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return reviewRepository.findRecentReviews(since);
    }

    /**
     * Check if customer can review a specific room
     */
    @Transactional(readOnly = true)
    public boolean canCustomerReviewRoom(Long customerId, Long roomId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        Room room = roomRepository.findById(roomId).orElse(null);

        if (customer == null || room == null) {
            return false;
        }

        // Check if already reviewed
        Optional<Review> existingReview = reviewRepository.findByRoomAndCustomer(room, customer);
        return existingReview.isEmpty();
    }

    /**
     * Get customer's review for a specific room (if exists)
     */
    @Transactional(readOnly = true)
    public Optional<Review> getCustomerReviewForRoom(Long customerId, Long roomId) {
        Customer customer = customerRepository.findById(customerId).orElse(null);
        Room room = roomRepository.findById(roomId).orElse(null);

        if (customer == null || room == null) {
            return Optional.empty();
        }

        return reviewRepository.findByRoomAndCustomer(room, customer);
    }
}
