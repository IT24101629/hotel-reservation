-- Database updates needed for admin integration
-- Run these SQL commands to add admin functionality support

-- 1. Create notifications table for admin notification system
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `message` text NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_read` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_notifications_user` (`user_id`),
  KEY `idx_notifications_read` (`is_read`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Update booking_status enum to include 'APPROVED' status needed by admin code
-- Note: This requires recreating the enum constraint
ALTER TABLE `bookings` MODIFY COLUMN `booking_status`
ENUM('PENDING','PENDING_PAYMENT','CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','COMPLETED','NO_SHOW','APPROVED')
DEFAULT 'PENDING';

-- 3. Add any indexes for better performance on admin queries
CREATE INDEX `idx_bookings_status_date` ON `bookings` (`booking_status`, `created_at`);
CREATE INDEX `idx_rooms_status_type` ON `rooms` (`status`, `type_id`);