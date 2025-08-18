-- =====================================================
-- HOTEL RESERVATION SYSTEM - COMPLETE DATABASE SCHEMA
-- Database: hotel_reservation_db
-- =====================================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS hotel_reservation_db;
USE hotel_reservation_db;

-- Drop tables if they exist (for clean setup)
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS rooms;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS room_types;

-- =====================================================
-- 1. ROOM TYPES TABLE
-- =====================================================
CREATE TABLE room_types (
    type_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    base_price DECIMAL(10,2) NOT NULL,
    max_occupancy INT NOT NULL,
    amenities JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. ROOMS TABLE
-- =====================================================
CREATE TABLE rooms (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL UNIQUE,
    type_id BIGINT NOT NULL,
    floor_number INT NOT NULL,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'OUT_OF_ORDER') DEFAULT 'AVAILABLE',
    price_per_night DECIMAL(10,2) NOT NULL,
    description TEXT,
    amenities JSON,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (type_id) REFERENCES room_types(type_id) ON DELETE RESTRICT
);

-- =====================================================
-- 3. USERS TABLE (for authentication)
-- =====================================================
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN', 'STAFF') DEFAULT 'CUSTOMER',
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    verification_token VARCHAR(255),
    reset_password_token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =====================================================
-- 4. CUSTOMERS TABLE (detailed customer information)
-- =====================================================
CREATE TABLE customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    phone_number VARCHAR(20),
    address TEXT,
    city VARCHAR(50),
    country VARCHAR(50),
    postal_code VARCHAR(20),
    date_of_birth DATE,
    id_number VARCHAR(50),
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    preferences JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =====================================================
-- 5. BOOKINGS TABLE (updated version)
-- =====================================================
CREATE TABLE bookings (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_reference VARCHAR(20) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    number_of_guests INT NOT NULL CHECK (number_of_guests >= 1 AND number_of_guests <= 10),
    number_of_nights INT NOT NULL,
    room_price_per_night DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    booking_status ENUM('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'COMPLETED', 'NO_SHOW') DEFAULT 'PENDING',
    payment_status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'PARTIAL_REFUND') DEFAULT 'PENDING',
    special_requests TEXT,
    customer_notes TEXT,
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    cancellation_reason TEXT,
    qr_code_path VARCHAR(500),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT,
    INDEX idx_booking_dates (check_in_date, check_out_date),
    INDEX idx_booking_status (booking_status),
    INDEX idx_customer_bookings (customer_id),
    INDEX idx_room_bookings (room_id)
);

-- =====================================================
-- 6. PAYMENTS TABLE
-- =====================================================
CREATE TABLE payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    payment_method ENUM('PAYHERE', 'CREDIT_CARD', 'DEBIT_CARD', 'CASH', 'BANK_TRANSFER') NOT NULL,
    payment_provider VARCHAR(50), -- PayHere, Visa, etc.
    transaction_id VARCHAR(100) UNIQUE,
    payhere_payment_id VARCHAR(100),
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'LKR',
    payment_status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
    payment_date TIMESTAMP NULL,
    gateway_response JSON,
    failure_reason TEXT,
    refund_amount DECIMAL(10,2) DEFAULT 0.00,
    refund_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE RESTRICT,
    INDEX idx_payment_status (payment_status),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_payment_date (payment_date)
);

-- =====================================================
-- INSERT SAMPLE DATA
-- =====================================================

-- Insert Room Types
INSERT INTO room_types (type_name, description, base_price, max_occupancy, amenities) VALUES
('Standard Single', 'Comfortable single room with essential amenities', 8500.00, 1, '["WiFi", "AC", "TV", "Private Bathroom"]'),
('Standard Double', 'Spacious double room perfect for couples', 12000.00, 2, '["WiFi", "AC", "TV", "Private Bathroom", "Mini Fridge"]'),
('Deluxe Room', 'Luxurious room with premium amenities', 18000.00, 3, '["WiFi", "AC", "TV", "Private Bathroom", "Mini Fridge", "Balcony", "Room Service"]'),
('Family Suite', 'Large suite perfect for families', 25000.00, 4, '["WiFi", "AC", "TV", "Private Bathroom", "Mini Fridge", "Balcony", "Room Service", "Sofa Bed"]'),
('Presidential Suite', 'Ultimate luxury accommodation', 45000.00, 6, '["WiFi", "AC", "TV", "Private Bathroom", "Mini Fridge", "Balcony", "Room Service", "Jacuzzi", "Butler Service"]');

-- Insert Rooms
INSERT INTO rooms (room_number, type_id, floor_number, status, price_per_night, description, image_url) VALUES
-- Floor 1 - Standard Rooms
('101', 1, 1, 'AVAILABLE', 8500.00, 'Cozy single room on the ground floor', '/images/rooms/standard-single.jpg'),
('102', 2, 1, 'AVAILABLE', 12000.00, 'Comfortable double room with garden view', '/images/rooms/standard-double.jpg'),
('103', 2, 1, 'AVAILABLE', 12000.00, 'Double room near reception', '/images/rooms/standard-double.jpg'),
('104', 1, 1, 'AVAILABLE', 8500.00, 'Single room with courtyard view', '/images/rooms/standard-single.jpg'),

-- Floor 2 - Deluxe Rooms
('201', 3, 2, 'AVAILABLE', 18000.00, 'Deluxe room with city view', '/images/rooms/deluxe.jpg'),
('202', 3, 2, 'AVAILABLE', 18000.00, 'Deluxe room with balcony', '/images/rooms/deluxe.jpg'),
('203', 4, 2, 'AVAILABLE', 25000.00, 'Family suite with separate living area', '/images/rooms/family-suite.jpg'),
('204', 3, 2, 'MAINTENANCE', 18000.00, 'Deluxe room under maintenance', '/images/rooms/deluxe.jpg'),

-- Floor 3 - Premium Rooms
('301', 4, 3, 'AVAILABLE', 25000.00, 'Spacious family suite with ocean view', '/images/rooms/family-suite.jpg'),
('302', 5, 3, 'AVAILABLE', 45000.00, 'Presidential suite with panoramic views', '/images/rooms/presidential.jpg'),
('303', 3, 3, 'AVAILABLE', 18000.00, 'Deluxe room on top floor', '/images/rooms/deluxe.jpg'),
('304', 4, 3, 'AVAILABLE', 25000.00, 'Family suite with mountain view', '/images/rooms/family-suite.jpg'),

-- Floor 4 - More rooms
('401', 2, 4, 'AVAILABLE', 12000.00, 'Double room with excellent view', '/images/rooms/standard-double.jpg'),
('402', 2, 4, 'AVAILABLE', 12000.00, 'Corner double room', '/images/rooms/standard-double.jpg'),
('403', 1, 4, 'AVAILABLE', 8500.00, 'Single room on top floor', '/images/rooms/standard-single.jpg'),
('404', 3, 4, 'AVAILABLE', 18000.00, 'Deluxe room with premium location', '/images/rooms/deluxe.jpg');

-- Insert Sample Admin User
INSERT INTO users (username, email, password_hash, first_name, last_name, role, is_active, email_verified) VALUES
('admin', 'admin@hotel.com', '$2a$10$rOmSYGJUdVZKhWgfVwPLR.XhJLJhd3MKbYYoRi4CQUZ8XKUfgLJvy', 'Hotel', 'Administrator', 'ADMIN', TRUE, TRUE);

-- Insert Sample Customer User
INSERT INTO users (username, email, password_hash, first_name, last_name, role, is_active, email_verified) VALUES
('john_doe', 'john.doe@gmail.com', '$2a$10$rOmSYGJUdVZKhWgfVwPLR.XhJLJhd3MKbYYoRi4CQUZ8XKUfgLJvy', 'John', 'Doe', 'CUSTOMER', TRUE, TRUE);

-- Insert Customer Details
INSERT INTO customers (user_id, phone_number, address, city, country, postal_code) VALUES
(2, '+94771234567', '123 Main Street, Colombo', 'Colombo', 'Sri Lanka', '00100');

-- =====================================================
-- CREATE INDEXES FOR BETTER PERFORMANCE
-- =====================================================
CREATE INDEX idx_rooms_status ON rooms(status);
CREATE INDEX idx_rooms_type ON rooms(type_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_customers_user ON customers(user_id);

-- =====================================================
-- CREATE VIEWS FOR EASIER QUERIES
-- =====================================================

-- View for available rooms with type information
CREATE VIEW available_rooms_view AS
SELECT 
    r.room_id,
    r.room_number,
    r.floor_number,
    r.price_per_night,
    r.description as room_description,
    r.image_url,
    rt.type_name,
    rt.description as type_description,
    rt.max_occupancy,
    rt.amenities
FROM rooms r
JOIN room_types rt ON r.type_id = rt.type_id
WHERE r.status = 'AVAILABLE';

-- View for booking details with customer and room information
CREATE VIEW booking_details_view AS
SELECT 
    b.booking_id,
    b.booking_reference,
    b.check_in_date,
    b.check_out_date,
    b.number_of_guests,
    b.total_amount,
    b.booking_status,
    b.payment_status,
    CONCAT(u.first_name, ' ', u.last_name) as customer_name,
    u.email as customer_email,
    c.phone_number,
    r.room_number,
    rt.type_name as room_type,
    b.created_at
FROM bookings b
JOIN customers c ON b.customer_id = c.customer_id
JOIN users u ON c.user_id = u.user_id
JOIN rooms r ON b.room_id = r.room_id
JOIN room_types rt ON r.type_id = rt.type_id;

-- =====================================================
-- STORED PROCEDURES
-- =====================================================

-- Procedure to check room availability
DELIMITER //
CREATE PROCEDURE CheckRoomAvailability(
    IN p_room_id BIGINT,
    IN p_check_in DATE,
    IN p_check_out DATE
)
BEGIN
    SELECT COUNT(*) as conflicting_bookings
    FROM bookings 
    WHERE room_id = p_room_id 
    AND ((check_in_date < p_check_out AND check_out_date > p_check_in))
    AND booking_status IN ('CONFIRMED', 'CHECKED_IN');
END //
DELIMITER ;

-- Procedure to get available rooms for date range
DELIMITER //
CREATE PROCEDURE GetAvailableRooms(
    IN p_check_in DATE,
    IN p_check_out DATE,
    IN p_guests INT
)
BEGIN
    SELECT DISTINCT
        r.room_id,
        r.room_number,
        r.floor_number,
        r.price_per_night,
        r.description,
        r.image_url,
        rt.type_name,
        rt.max_occupancy,
        rt.amenities
    FROM rooms r
    JOIN room_types rt ON r.type_id = rt.type_id
    WHERE r.status = 'AVAILABLE'
    AND rt.max_occupancy >= p_guests
    AND r.room_id NOT IN (
        SELECT DISTINCT room_id 
        FROM bookings 
        WHERE ((check_in_date < p_check_out AND check_out_date > p_check_in))
        AND booking_status IN ('CONFIRMED', 'CHECKED_IN')
    )
    ORDER BY r.floor_number, r.room_number;
END //
DELIMITER ;

-- =====================================================
-- SAMPLE QUERIES TO TEST THE SCHEMA
-- =====================================================

-- Test available rooms view
-- SELECT * FROM available_rooms_view;

-- Test room availability for specific dates
-- CALL GetAvailableRooms('2024-12-25', '2024-12-28', 2);

-- Test booking details view
-- SELECT * FROM booking_details_view;

-- Show all room types with their amenities
-- SELECT type_name, base_price, max_occupancy, JSON_EXTRACT(amenities, '$') as amenities_list FROM room_types;

COMMIT;