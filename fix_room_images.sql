-- Fix Room Image URLs
-- This script updates the image URLs in the rooms table to match the correct static resource path

-- Update rooms to have proper image URLs based on room type
UPDATE rooms r
JOIN room_types rt ON r.type_id = rt.room_type_id
SET r.image_url = CASE
    WHEN LOWER(rt.type_name) LIKE '%standard single%' THEN '/images/rooms/standard-single.jpg'
    WHEN LOWER(rt.type_name) LIKE '%standard double%' THEN '/images/rooms/standard-double.jpg'
    WHEN LOWER(rt.type_name) LIKE '%deluxe%' THEN '/images/rooms/deluxe.jpg'
    WHEN LOWER(rt.type_name) LIKE '%family%' THEN '/images/rooms/family-suite.jpg'
    WHEN LOWER(rt.type_name) LIKE '%presidential%' THEN '/images/rooms/presidential.jpg'
    ELSE '/images/rooms/room-default.jpg'
END;

-- Verify the updates
SELECT
    r.room_id,
    r.room_number,
    rt.type_name,
    r.image_url
FROM rooms r
JOIN room_types rt ON r.type_id = rt.room_type_id
ORDER BY r.room_id;
