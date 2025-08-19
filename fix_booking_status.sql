-- Fix booking_status ENUM to include PENDING_PAYMENT
USE hotel_reservation_db;

ALTER TABLE bookings 
MODIFY COLUMN booking_status ENUM(
    'PENDING', 
    'PENDING_PAYMENT', 
    'CONFIRMED', 
    'CHECKED_IN', 
    'CHECKED_OUT', 
    'CANCELLED', 
    'COMPLETED', 
    'NO_SHOW'
) DEFAULT 'PENDING';

-- Verify the change
DESCRIBE bookings;