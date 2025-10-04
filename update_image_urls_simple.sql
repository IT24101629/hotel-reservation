USE `hotel_reservation_db`;

-- Update all room image URLs to use the working path
UPDATE rooms SET image_url = '/images/room-default.jpg';

-- Verify the updates
SELECT room_id, room_number, image_url FROM rooms;
