-- =====================================================
-- CHATBOT EXTENSION TO HOTEL RESERVATION SYSTEM
-- Add these tables to your existing database
-- =====================================================

USE hotel_reservation_db;

-- =====================================================
-- 1. CHAT SESSIONS TABLE
-- =====================================================
CREATE TABLE chat_sessions (
    session_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NULL, -- Can be null for anonymous users
    customer_id BIGINT NULL,
    session_status ENUM('ACTIVE', 'COMPLETED', 'ABANDONED', 'BOOKING_COMPLETED') DEFAULT 'ACTIVE',
    language VARCHAR(10) DEFAULT 'en',
    user_ip VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE SET NULL
);

-- =====================================================
-- 2. CHAT MESSAGES TABLE
-- =====================================================
CREATE TABLE chat_messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    message_type ENUM('USER', 'BOT', 'SYSTEM') NOT NULL,
    content TEXT NOT NULL,
    intent VARCHAR(100), -- booking_inquiry, room_details, availability_check, etc.
    entities JSON, -- Store extracted entities like dates, guest count, room type, etc.
    confidence_score DECIMAL(3,2), -- AI confidence in response
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_processed BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_session_timestamp (session_id, timestamp),
    INDEX idx_message_type (message_type),
    INDEX idx_intent (intent)
);

-- =====================================================
-- 3. CHAT BOOKING INTENTS TABLE
-- =====================================================
CREATE TABLE chat_booking_intents (
    intent_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    check_in_date DATE,
    check_out_date DATE,
    number_of_guests INT,
    number_of_nights INT,
    room_type_preference VARCHAR(100),
    budget_min DECIMAL(10,2),
    budget_max DECIMAL(10,2),
    special_requests TEXT,
    location_preference VARCHAR(255),
    amenity_preferences JSON, -- ["WiFi", "AC", "Balcony"]
    collected_data JSON, -- Store all collected booking data
    booking_status ENUM('COLLECTING_INFO', 'INFO_COMPLETE', 'SHOWING_OPTIONS', 'READY_TO_BOOK', 'BOOKING_COMPLETED', 'CANCELLED') DEFAULT 'COLLECTING_INFO',
    suggested_rooms JSON, -- Store room suggestions
    selected_room_id BIGINT,
    booking_id BIGINT NULL, -- Links to actual booking when completed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    FOREIGN KEY (selected_room_id) REFERENCES rooms(room_id) ON DELETE SET NULL,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE SET NULL
);

-- =====================================================
-- 4. CHATBOT RESPONSES TABLE (Pre-trained responses)
-- =====================================================
CREATE TABLE chatbot_responses (
    response_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    intent_category VARCHAR(100) NOT NULL, -- greeting, booking_inquiry, room_info, availability, etc.
    intent_subcategory VARCHAR(100),
    trigger_keywords JSON, -- Keywords that trigger this response
    response_text TEXT NOT NULL,
    response_type ENUM('TEXT', 'QUICK_REPLY', 'ROOM_SUGGESTION', 'BOOKING_FORM') DEFAULT 'TEXT',
    context_required JSON, -- What context data is needed for this response
    priority INT DEFAULT 0, -- Higher priority responses are preferred
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_intent (intent_category, intent_subcategory),
    INDEX idx_active (is_active)
);

-- =====================================================
-- 5. CHAT ANALYTICS TABLE
-- =====================================================
CREATE TABLE chat_analytics (
    analytics_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100), -- session_start, message_sent, booking_inquiry, booking_completed, etc.
    event_data JSON,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp)
);

-- =====================================================
-- INSERT SAMPLE CHATBOT RESPONSES
-- =====================================================

-- Greeting responses
INSERT INTO chatbot_responses (intent_category, trigger_keywords, response_text, response_type) VALUES
('greeting', '["hello", "hi", "hey", "good morning", "good evening"]', 
 'Hello! 🏨 Welcome to Gold Palm Hotel! I\'m here to help you find the perfect room for your stay. How can I assist you today?', 'TEXT'),

('greeting', '["start", "begin"]', 
 'Welcome to our Hotel Booking Assistant! 🌟 I can help you:\n\n• Check room availability\n• Get room details and pricing\n• Make a reservation\n• Answer questions about our hotel\n\nWhat would you like to do?', 'QUICK_REPLY');

-- Booking inquiry responses
INSERT INTO chatbot_responses (intent_category, intent_subcategory, trigger_keywords, response_text) VALUES
('booking', 'initial_inquiry', '["book", "reservation", "room", "stay", "check availability"]',
 'I\'d love to help you book a room! 🛏️ To find the perfect accommodation for you, I\'ll need a few details:\n\n1. Check-in date\n2. Check-out date\n3. Number of guests\n\nCould you please share these details?'),

('booking', 'date_inquiry', '["when", "dates", "check in", "check out"]',
 'Please provide your preferred check-in and check-out dates. You can say something like:\n• "I need a room from December 25 to December 28"\n• "Check-in: 25th Dec, Check-out: 28th Dec"'),

('booking', 'guest_inquiry', '["guests", "people", "persons", "how many"]',
 'How many guests will be staying? This helps me suggest the right room type for you.');

-- Room information responses
INSERT INTO chatbot_responses (intent_category, intent_subcategory, trigger_keywords, response_text) VALUES
('room_info', 'types', '["room types", "what rooms", "available rooms"]',
 'We offer several room types:\n\n🛏️ **Standard Single** - LKR 8,500/night (1 guest)\n🛏️ **Standard Double** - LKR 12,000/night (2 guests)\n🏨 **Deluxe Room** - LKR 18,000/night (3 guests)\n👨‍👩‍👧‍👦 **Family Suite** - LKR 25,000/night (4 guests)\n👑 **Presidential Suite** - LKR 45,000/night (6 guests)\n\nWould you like more details about any specific room type?'),

('room_info', 'amenities', '["amenities", "facilities", "what includes"]',
 'All our rooms include:\n• Free WiFi 📶\n• Air Conditioning ❄️\n• LED TV 📺\n• Private Bathroom 🚿\n\nPremium rooms also feature:\n• Mini Fridge 🧊\n• Balcony 🌅\n• Room Service 🛎️\n• Jacuzzi (Presidential Suite) 🛁');

-- Non-hotel related responses
INSERT INTO chatbot_responses (intent_category, trigger_keywords, response_text) VALUES
('out_of_scope', '["weather", "restaurant", "flight", "transport", "directions"]',
 'I apologize, but I can only assist with hotel booking and room-related inquiries. 😔\n\nFor other matters, our customer care team will contact you soon, or you can call our hotline immediately: **011-4545678**\n\nIs there anything about our hotel rooms or booking process I can help you with?');

-- Emergency/complaint responses
INSERT INTO chatbot_responses (intent_category, trigger_keywords, response_text) VALUES
('emergency', '["complaint", "problem", "issue", "emergency", "urgent", "help"]',
 'I understand you need immediate assistance! 🚨\n\nFor urgent matters, please call our hotline: **011-4545678**\n\nOur customer care team will also contact you as soon as possible.\n\nIf this is a booking-related question, I\'m here to help!');

-- =====================================================
-- CREATE INDEXES FOR BETTER PERFORMANCE
-- =====================================================
CREATE INDEX idx_chat_sessions_status ON chat_sessions(session_status);
CREATE INDEX idx_chat_sessions_created ON chat_sessions(created_at);
CREATE INDEX idx_booking_intents_status ON chat_booking_intents(booking_status);
CREATE INDEX idx_booking_intents_dates ON chat_booking_intents(check_in_date, check_out_date);

-- =====================================================
-- CREATE VIEWS FOR CHAT ANALYTICS
-- =====================================================

-- View for active chat sessions with booking progress
CREATE VIEW active_chat_sessions_view AS
SELECT 
    cs.session_id,
    cs.session_status,
    cs.created_at as session_start,
    cbi.booking_status,
    cbi.check_in_date,
    cbi.check_out_date,
    cbi.number_of_guests,
    COUNT(cm.message_id) as message_count,
    MAX(cm.timestamp) as last_message_time
FROM chat_sessions cs
LEFT JOIN chat_booking_intents cbi ON cs.session_id = cbi.session_id
LEFT JOIN chat_messages cm ON cs.session_id = cm.session_id
WHERE cs.session_status = 'ACTIVE'
GROUP BY cs.session_id;

-- View for chat conversion rates
CREATE VIEW chat_conversion_stats AS
SELECT 
    DATE(cs.created_at) as date,
    COUNT(*) as total_sessions,
    COUNT(CASE WHEN cs.session_status = 'BOOKING_COMPLETED' THEN 1 END) as completed_bookings,
    ROUND((COUNT(CASE WHEN cs.session_status = 'BOOKING_COMPLETED' THEN 1 END) * 100.0 / COUNT(*)), 2) as conversion_rate
FROM chat_sessions cs
GROUP BY DATE(cs.created_at)
ORDER BY date DESC;

-- =====================================================
-- STORED PROCEDURES FOR CHATBOT
-- =====================================================

-- Procedure to get chat session with booking intent
DELIMITER //
CREATE PROCEDURE GetChatSessionDetails(
    IN p_session_id VARCHAR(255)
)
BEGIN
    SELECT 
        cs.*,
        cbi.booking_status,
        cbi.check_in_date,
        cbi.check_out_date,
        cbi.number_of_guests,
        cbi.collected_data,
        cbi.suggested_rooms
    FROM chat_sessions cs
    LEFT JOIN chat_booking_intents cbi ON cs.session_id = cbi.session_id
    WHERE cs.session_id = p_session_id;
END //
DELIMITER ;

-- Procedure to find suitable rooms for chat booking
DELIMITER //
CREATE PROCEDURE FindRoomsForChatBooking(
    IN p_check_in DATE,
    IN p_check_out DATE,
    IN p_guests INT,
    IN p_budget_max DECIMAL(10,2)
)
BEGIN
    SELECT 
        r.room_id,
        r.room_number,
        r.price_per_night,
        r.description,
        r.image_url,
        rt.type_name,
        rt.max_occupancy,
        rt.amenities,
        (DATEDIFF(p_check_out, p_check_in) * r.price_per_night) as total_cost
    FROM rooms r
    JOIN room_types rt ON r.type_id = rt.type_id
    WHERE r.status = 'AVAILABLE'
    AND rt.max_occupancy >= p_guests
    AND (p_budget_max IS NULL OR r.price_per_night <= p_budget_max)
    AND r.room_id NOT IN (
        SELECT DISTINCT room_id 
        FROM bookings 
        WHERE ((check_in_date < p_check_out AND check_out_date > p_check_in))
        AND booking_status IN ('CONFIRMED', 'CHECKED_IN')
    )
    ORDER BY r.price_per_night ASC, r.room_number ASC;
END //
DELIMITER ;

COMMIT;