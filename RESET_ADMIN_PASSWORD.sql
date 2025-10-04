-- Reset Admin Password to 'admin123'
-- Run this SQL script to fix the admin login

UPDATE users
SET password_hash = '$2a$10$NQDzE6cKJSjKNVhw6aNwGOBzOQOqYaO3J.lOQZfXGPgUKb7e6xN/S'
WHERE username = 'admin';

-- This sets the admin password to 'admin123' with BCrypt encryption
-- The hash was generated using BCrypt with strength 10

-- After running this, you can login with:
-- Username: admin
-- Password: admin123