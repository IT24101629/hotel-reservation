-- --------------------------------------------------------
-- Promotion Management System Database Updates
-- Hotel Reservation System
-- --------------------------------------------------------

-- Create promotions table
CREATE TABLE IF NOT EXISTS `promotions` (
  `promotion_id` bigint NOT NULL AUTO_INCREMENT,
  `promo_code` varchar(20) NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text,
  `discount_type` enum('PERCENTAGE','FIXED_AMOUNT') NOT NULL,
  `discount_value` decimal(10,2) NOT NULL,
  `minimum_booking_amount` decimal(10,2) DEFAULT '0.00',
  `maximum_discount` decimal(10,2) DEFAULT NULL,
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `usage_limit` int DEFAULT NULL,
  `usage_count` int DEFAULT '0',
  `is_active` tinyint(1) DEFAULT '1',
  `image_path` varchar(500) DEFAULT NULL,
  `terms_conditions` text,
  `applicable_room_types` json DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`promotion_id`),
  UNIQUE KEY `promo_code` (`promo_code`),
  KEY `idx_promo_code` (`promo_code`),
  KEY `idx_promotion_dates` (`start_date`, `end_date`),
  KEY `idx_promotion_active` (`is_active`),
  KEY `idx_created_by` (`created_by`),
  CONSTRAINT `promotions_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Add promo_code column to bookings table if not exists
ALTER TABLE `bookings`
ADD COLUMN IF NOT EXISTS `promo_code` varchar(20) DEFAULT NULL,
ADD COLUMN IF NOT EXISTS `discount_amount` decimal(10,2) DEFAULT '0.00',
ADD COLUMN IF NOT EXISTS `original_amount` decimal(10,2) DEFAULT NULL;

-- Add index for promo_code in bookings
CREATE INDEX IF NOT EXISTS `idx_bookings_promo_code` ON `bookings` (`promo_code`);

-- Create promotion_usage table to track individual usage
CREATE TABLE IF NOT EXISTS `promotion_usage` (
  `usage_id` bigint NOT NULL AUTO_INCREMENT,
  `promotion_id` bigint NOT NULL,
  `booking_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `discount_applied` decimal(10,2) NOT NULL,
  `used_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`usage_id`),
  KEY `idx_promotion_usage_promotion` (`promotion_id`),
  KEY `idx_promotion_usage_booking` (`booking_id`),
  KEY `idx_promotion_usage_customer` (`customer_id`),
  CONSTRAINT `promotion_usage_ibfk_1` FOREIGN KEY (`promotion_id`) REFERENCES `promotions` (`promotion_id`) ON DELETE CASCADE,
  CONSTRAINT `promotion_usage_ibfk_2` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`booking_id`) ON DELETE CASCADE,
  CONSTRAINT `promotion_usage_ibfk_3` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`customer_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Insert sample promotions data
INSERT INTO `promotions` (`promo_code`, `title`, `description`, `discount_type`, `discount_value`, `minimum_booking_amount`, `maximum_discount`, `start_date`, `end_date`, `usage_limit`, `created_by`, `terms_conditions`) VALUES
('WELCOME10', 'Welcome New Customer', 'Get 10% off on your first booking', 'PERCENTAGE', 10.00, 5000.00, 5000.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 1000, 1, 'Valid for first-time customers only. Cannot be combined with other offers.'),
('SUMMER25', 'Summer Special', 'Save 25% on all bookings this summer', 'PERCENTAGE', 25.00, 10000.00, 10000.00, '2025-06-01 00:00:00', '2025-08-31 23:59:59', 500, 1, 'Valid for bookings made during summer season. Minimum 2 nights stay required.'),
('FLAT2000', 'Flat Discount', 'Get LKR 2000 off on bookings above LKR 15000', 'FIXED_AMOUNT', 2000.00, 15000.00, 2000.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 200, 1, 'Valid on bookings above LKR 15000. Cannot be combined with percentage discounts.'),
('WEEKEND15', 'Weekend Getaway', '15% off on weekend bookings', 'PERCENTAGE', 15.00, 8000.00, 7500.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 300, 1, 'Valid for Friday to Sunday bookings only.');

-- Create stored procedure to validate promo codes
DELIMITER //
CREATE PROCEDURE `ValidatePromoCode`(
    IN p_promo_code VARCHAR(20),
    IN p_booking_amount DECIMAL(10,2),
    IN p_customer_id BIGINT,
    IN p_room_type_id BIGINT,
    OUT p_is_valid BOOLEAN,
    OUT p_discount_amount DECIMAL(10,2),
    OUT p_error_message VARCHAR(255)
)
BEGIN
    DECLARE v_promotion_id BIGINT DEFAULT NULL;
    DECLARE v_discount_type ENUM('PERCENTAGE','FIXED_AMOUNT');
    DECLARE v_discount_value DECIMAL(10,2);
    DECLARE v_minimum_amount DECIMAL(10,2);
    DECLARE v_maximum_discount DECIMAL(10,2);
    DECLARE v_usage_limit INT;
    DECLARE v_usage_count INT;
    DECLARE v_start_date DATETIME;
    DECLARE v_end_date DATETIME;
    DECLARE v_is_active BOOLEAN;
    DECLARE v_applicable_room_types JSON;
    DECLARE v_customer_usage_count INT DEFAULT 0;

    SET p_is_valid = FALSE;
    SET p_discount_amount = 0.00;
    SET p_error_message = '';

    -- Get promotion details
    SELECT
        promotion_id, discount_type, discount_value, minimum_booking_amount,
        maximum_discount, usage_limit, usage_count, start_date, end_date,
        is_active, applicable_room_types
    INTO
        v_promotion_id, v_discount_type, v_discount_value, v_minimum_amount,
        v_maximum_discount, v_usage_limit, v_usage_count, v_start_date, v_end_date,
        v_is_active, v_applicable_room_types
    FROM promotions
    WHERE promo_code = p_promo_code;

    -- Check if promotion exists
    IF v_promotion_id IS NULL THEN
        SET p_error_message = 'Invalid promo code';
        LEAVE proc;
    END IF;

    -- Check if promotion is active
    IF NOT v_is_active THEN
        SET p_error_message = 'This promo code is currently inactive';
        LEAVE proc;
    END IF;

    -- Check date validity
    IF NOW() < v_start_date THEN
        SET p_error_message = 'This promo code is not yet active';
        LEAVE proc;
    END IF;

    IF NOW() > v_end_date THEN
        SET p_error_message = 'This promo code has expired';
        LEAVE proc;
    END IF;

    -- Check minimum booking amount
    IF p_booking_amount < v_minimum_amount THEN
        SET p_error_message = CONCAT('Minimum booking amount of LKR ', v_minimum_amount, ' required');
        LEAVE proc;
    END IF;

    -- Check usage limit
    IF v_usage_limit IS NOT NULL AND v_usage_count >= v_usage_limit THEN
        SET p_error_message = 'This promo code has reached its usage limit';
        LEAVE proc;
    END IF;

    -- Check customer-specific usage (assuming one-time use per customer for WELCOME10)
    IF p_promo_code = 'WELCOME10' THEN
        SELECT COUNT(*) INTO v_customer_usage_count
        FROM promotion_usage pu
        JOIN promotions p ON pu.promotion_id = p.promotion_id
        WHERE p.promo_code = 'WELCOME10' AND pu.customer_id = p_customer_id;

        IF v_customer_usage_count > 0 THEN
            SET p_error_message = 'This welcome offer can only be used once per customer';
            LEAVE proc;
        END IF;
    END IF;

    -- Calculate discount
    IF v_discount_type = 'PERCENTAGE' THEN
        SET p_discount_amount = p_booking_amount * (v_discount_value / 100);
        IF v_maximum_discount IS NOT NULL AND p_discount_amount > v_maximum_discount THEN
            SET p_discount_amount = v_maximum_discount;
        END IF;
    ELSE
        SET p_discount_amount = v_discount_value;
    END IF;

    -- Ensure discount doesn't exceed booking amount
    IF p_discount_amount > p_booking_amount THEN
        SET p_discount_amount = p_booking_amount;
    END IF;

    SET p_is_valid = TRUE;

    proc: BEGIN END;
END//
DELIMITER ;

-- Create view for active promotions
CREATE OR REPLACE VIEW `active_promotions_view` AS
SELECT
    promotion_id,
    promo_code,
    title,
    description,
    discount_type,
    discount_value,
    minimum_booking_amount,
    maximum_discount,
    start_date,
    end_date,
    usage_limit,
    usage_count,
    image_path,
    terms_conditions,
    (usage_limit - usage_count) as remaining_uses
FROM promotions
WHERE is_active = 1
AND start_date <= NOW()
AND end_date >= NOW()
ORDER BY created_at DESC;