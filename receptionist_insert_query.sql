-- Receptionist user has been added to the database with these details:
-- This query was already executed in your database

-- Current receptionist login credentials:
-- Username: res_hotel
-- Email: receptionist@goldpalmhotel.com  
-- Password: password (BCrypt hash: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.)
-- Role: STAFF

-- The inserted record matches this structure:
INSERT INTO `hotel_reservation_db`.`users` 
(`user_id`, `username`, `email`, `password_hash`, `first_name`, `last_name`, `role`, `email_verified`) 
VALUES (6, 'res_hotel', 'receptionist@goldpalmhotel.com', 
        '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 
        'reseptionist', 'palm', 'STAFF', 1);

-- No additional insert needed - user already exists in your database!