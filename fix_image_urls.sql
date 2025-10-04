-- Update all room image URLs to use the working path
UPDATE rooms SET image_url = '/images/room-default.jpg' WHERE image_url LIKE '/images/rooms/%';
