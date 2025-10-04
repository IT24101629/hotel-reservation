-- Reviews System Database Updates
-- This SQL script creates the reviews table for the hotel reservation system

-- ====================================
-- Create Reviews Table
-- ====================================

CREATE TABLE IF NOT EXISTS reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    booking_id BIGINT,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(200),
    comment TEXT,
    is_verified_stay BOOLEAN DEFAULT FALSE,
    is_approved BOOLEAN DEFAULT TRUE,
    admin_response TEXT,
    responded_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_review_room FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE SET NULL,

    INDEX idx_reviews_room (room_id),
    INDEX idx_reviews_customer (customer_id),
    INDEX idx_reviews_booking (booking_id),
    INDEX idx_reviews_rating (rating),
    INDEX idx_reviews_approved (is_approved),
    INDEX idx_reviews_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================
-- Sample Data (Optional - for testing)
-- ====================================

-- Note: Uncomment the following INSERT statements if you want to add sample reviews
-- Make sure the room_id, customer_id, and booking_id values match existing records in your database

/*
INSERT INTO reviews (room_id, customer_id, booking_id, rating, title, comment, is_verified_stay, is_approved) VALUES
(1, 1, 1, 5, 'Excellent Stay!', 'The room was absolutely perfect. Clean, spacious, and the view was breathtaking. Staff was very friendly and helpful. Would definitely stay here again!', TRUE, TRUE),
(1, 2, NULL, 4, 'Great Room', 'Very comfortable room with modern amenities. The bed was super comfortable. Only minor issue was the AC was a bit noisy.', FALSE, TRUE),
(2, 3, 2, 5, 'Amazing Experience', 'This is the best hotel I have ever stayed at. Everything was perfect from check-in to check-out. Highly recommended!', TRUE, TRUE),
(2, 1, NULL, 3, 'Good but could be better', 'Room was clean and comfortable but felt a bit dated. Service was good though.', FALSE, TRUE),
(3, 2, 3, 5, 'Perfect for Families', 'Stayed here with my family and we all loved it! The suite was spacious and had everything we needed. Kids loved the amenities.', TRUE, TRUE);

-- Add admin responses to some reviews
UPDATE reviews SET admin_response = 'Thank you so much for your wonderful feedback! We are thrilled to hear you enjoyed your stay. We look forward to welcoming you back!', responded_at = NOW() WHERE review_id = 1;
UPDATE reviews SET admin_response = 'Thank you for your review and we apologize for the AC noise. We will have our maintenance team check it immediately. We hope to see you again!', responded_at = NOW() WHERE review_id = 2;
*/

-- ====================================
-- Verification Queries
-- ====================================

-- Check if table was created successfully
-- SELECT * FROM reviews;

-- Get review statistics by room
-- SELECT
--     r.room_number,
--     rt.type_name,
--     COUNT(rev.review_id) as total_reviews,
--     AVG(rev.rating) as average_rating,
--     SUM(CASE WHEN rev.rating = 5 THEN 1 ELSE 0 END) as five_stars,
--     SUM(CASE WHEN rev.rating = 4 THEN 1 ELSE 0 END) as four_stars,
--     SUM(CASE WHEN rev.rating = 3 THEN 1 ELSE 0 END) as three_stars,
--     SUM(CASE WHEN rev.rating = 2 THEN 1 ELSE 0 END) as two_stars,
--     SUM(CASE WHEN rev.rating = 1 THEN 1 ELSE 0 END) as one_star
-- FROM rooms r
-- LEFT JOIN room_types rt ON r.room_type_id = rt.room_type_id
-- LEFT JOIN reviews rev ON r.room_id = rev.room_id AND rev.is_approved = TRUE
-- GROUP BY r.room_id, r.room_number, rt.type_name
-- ORDER BY average_rating DESC;

-- Get pending reviews
-- SELECT
--     rev.review_id,
--     rev.rating,
--     rev.title,
--     rev.comment,
--     r.room_number,
--     rt.type_name,
--     CONCAT(u.first_name, ' ', u.last_name) as customer_name,
--     rev.created_at
-- FROM reviews rev
-- JOIN rooms r ON rev.room_id = r.room_id
-- JOIN room_types rt ON r.room_type_id = rt.room_type_id
-- JOIN customers c ON rev.customer_id = c.customer_id
-- JOIN users u ON c.user_id = u.user_id
-- WHERE rev.is_approved = FALSE
-- ORDER BY rev.created_at DESC;

-- ====================================
-- Notes
-- ====================================
-- 1. The reviews table is linked to rooms, customers, and bookings
-- 2. Rating is constrained between 1-5 stars
-- 3. Reviews are auto-approved by default (is_approved = TRUE)
-- 4. Verified stay badges are shown for reviews linked to bookings
-- 5. Admins can respond to reviews via admin_response field
-- 6. ON DELETE CASCADE ensures reviews are deleted if room or customer is deleted
-- 7. booking_id uses ON DELETE SET NULL to preserve reviews if booking is deleted
-- 8. Indexes are added for performance on common queries
