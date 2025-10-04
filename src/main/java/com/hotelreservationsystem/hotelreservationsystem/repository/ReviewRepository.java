package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.Booking;
import com.hotelreservationsystem.hotelreservationsystem.model.Customer;
import com.hotelreservationsystem.hotelreservationsystem.model.Review;
import com.hotelreservationsystem.hotelreservationsystem.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find all reviews for a specific room, ordered by creation date
     */
    List<Review> findByRoomOrderByCreatedAtDesc(Room room);

    /**
     * Find approved reviews for a specific room
     */
    List<Review> findByRoomAndIsApprovedTrueOrderByCreatedAtDesc(Room room);

    /**
     * Find all reviews by a customer
     */
    List<Review> findByCustomerOrderByCreatedAtDesc(Customer customer);

    /**
     * Find reviews by customer with pagination
     */
    Page<Review> findByCustomerOrderByCreatedAtDesc(Customer customer, Pageable pageable);

    /**
     * Find all approved reviews
     */
    List<Review> findByIsApprovedTrueOrderByCreatedAtDesc();

    /**
     * Find all pending reviews (not approved)
     */
    List<Review> findByIsApprovedFalseOrderByCreatedAtDesc();

    /**
     * Find review by booking and customer (to check if user already reviewed)
     */
    Optional<Review> findByBookingAndCustomer(Booking booking, Customer customer);

    /**
     * Find if customer has already reviewed a specific room
     */
    Optional<Review> findByRoomAndCustomer(Room room, Customer customer);

    /**
     * Calculate average rating for a room
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.room = :room AND r.isApproved = true")
    Double calculateAverageRatingByRoom(@Param("room") Room room);

    /**
     * Count total reviews for a room
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.room = :room AND r.isApproved = true")
    Long countApprovedReviewsByRoom(@Param("room") Room room);

    /**
     * Find reviews by rating
     */
    List<Review> findByRatingAndIsApprovedTrueOrderByCreatedAtDesc(Integer rating);

    /**
     * Find verified stay reviews
     */
    List<Review> findByIsVerifiedStayTrueAndIsApprovedTrueOrderByCreatedAtDesc();

    /**
     * Search reviews with pagination and filters
     */
    @Query("SELECT r FROM Review r WHERE " +
           "(:searchTerm IS NULL OR :searchTerm = '' OR " +
           "LOWER(r.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.comment) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND (:rating IS NULL OR r.rating = :rating) " +
           "AND (:approved IS NULL OR r.isApproved = :approved) " +
           "ORDER BY r.createdAt DESC")
    Page<Review> searchReviews(
            @Param("searchTerm") String searchTerm,
            @Param("rating") Integer rating,
            @Param("approved") Boolean approved,
            Pageable pageable
    );

    /**
     * Find reviews with admin responses
     */
    @Query("SELECT r FROM Review r WHERE r.adminResponse IS NOT NULL ORDER BY r.respondedAt DESC")
    List<Review> findReviewsWithAdminResponse();

    /**
     * Count reviews by customer
     */
    Long countByCustomer(Customer customer);

    /**
     * Get recent reviews (last N days)
     */
    @Query("SELECT r FROM Review r WHERE r.createdAt >= :since AND r.isApproved = true ORDER BY r.createdAt DESC")
    List<Review> findRecentReviews(@Param("since") java.time.LocalDateTime since);

    /**
     * Count reviews by rating for a room (for rating distribution)
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.room = :room AND r.isApproved = true GROUP BY r.rating")
    List<Object[]> getRatingDistributionByRoom(@Param("room") Room room);
}
