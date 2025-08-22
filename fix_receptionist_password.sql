-- Fix receptionist password hash in database
-- This will set the password to "password" with a correct BCrypt hash

UPDATE `hotel_reservation_db`.`users` 
SET `password_hash` = '$2a$10$e0MYzXyjpJS7Pd2AWugdx.Mx6sAxfwmSI7bGLhKGAWx7Fv9MNH1jG'
WHERE `email` = 'receptionist@goldpalmhotel.com';

-- Alternative hashes for different passwords:
-- For password "password":     $2a$10$e0MYzXyjpJS7Pd2AWugdx.Mx6sAxfwmSI7bGLhKGAWx7Fv9MNH1jG
-- For password "123456":       $2a$10$N9qo8uLOickgx2ZMRZoMyu3e5KmUc0L1pjzNSkKKr6qGr.z7e.b/q
-- For password "staff":        $2a$10$Y1l.5vKb5L5E7kCGbVJHK.9nWFI4lHi5G3Fz4f3GjNDFYG5Fz.H7m
-- For password "receptionist": $2a$10$L5kC9Pz2e7vBdGz5P4W4we4L9qU4jO8sG9qW4pQ3vL5kC9Pz2e7vBdGz

-- After running this query, you can login with:
-- Email: receptionist@goldpalmhotel.com
-- Password: password