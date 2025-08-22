# 🤖 Hotel Booking Chatbot - Complete Setup Guide

This guide will help you set up the complete hotel booking chatbot system that integrates with your existing Spring Boot application.

## 🏗️ Architecture Overview

```
Frontend (Chat UI) → Spring Boot → Python Chatbot Service → MySQL Database
                         ↓
                  Existing Booking System
```

## 📋 Prerequisites

1. **Python 3.8+** installed
2. **MySQL Database** (already configured)
3. **Spring Boot Application** (already running)
4. **Internet connection** for downloading dependencies

## 🚀 Step-by-Step Setup

### Step 1: Database Setup

1. **Run the chatbot database schema:**
```sql
-- Connect to your MySQL database and run:
SOURCE chatbot_schema.sql;
```

2. **Verify tables were created:**
```sql
SHOW TABLES LIKE 'chat_%';
-- Should show: chat_sessions, chat_messages, chat_booking_intents, chatbot_responses, chat_analytics
```

### Step 2: Python Environment Setup

1. **Create Python virtual environment:**
```bash
# Navigate to your project folder
cd C:\Users\LapMart\IdeaProjects\Hotel_Reservation_System_final

# Create virtual environment
python -m venv chatbot_env

# Activate environment (Windows)
chatbot_env\Scripts\activate

# Or on Mac/Linux:
# source chatbot_env/bin/activate
```

2. **Install required Python packages:**
```bash
pip install fastapi uvicorn mysql-connector-python requests pandas python-multipart
```

### Step 3: Configure Database Connection

1. **Update database credentials in `chatbot_service.py`:**
```python
# Line 29-35 in chatbot_service.py
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',           # Your MySQL username
    'password': 'your_password',  # Your MySQL password
    'database': 'hotel_reservation_db',
    'charset': 'utf8mb4'
}
```

### Step 4: Process Training Data

1. **Run the data processor:**
```bash
python chatbot_data_processor.py
```

This will create:
- `hotel_chatbot_training_data.json`
- `processed_conversations.json`
- `hotel_chatbot_training_data.csv`

### Step 5: Start the Python Chatbot Service

1. **Run the chatbot service:**
```bash
python chatbot_service.py
```

You should see:
```
INFO:     Started server process
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8000
```

2. **Test the service:**
Open browser and go to: `http://localhost:8000/health`
Should return: `{"status": "healthy", "service": "Hotel Booking Chatbot"}`

### Step 6: Start Your Spring Boot Application

1. **Ensure Spring Boot is running on port 8080**
2. **The chatbot integration is already added to your project**

### Step 7: Test the Complete System

1. **Open your browser and go to:** `http://localhost:8080/chat`
2. **Try these test messages:**
   - "Hello"
   - "I want to book a room"
   - "I need a room for 2 guests from December 25 to December 27"
   - "What are your room types?"
   - "Show me available rooms"

## 🎯 Features Included

### ✅ **Chatbot Capabilities:**
- **Natural Language Understanding** - Understands booking requests
- **Entity Extraction** - Extracts dates, guest count, room preferences
- **Room Availability** - Checks real-time room availability
- **Room Suggestions** - Suggests suitable rooms based on requirements
- **Booking Completion** - Completes bookings through chat
- **Out-of-scope Handling** - Redirects non-hotel questions to hotline
- **Emergency Response** - Provides immediate hotline number

### ✅ **Smart Responses:**
- **Contextual Understanding** - Remembers conversation history
- **Progressive Information Collection** - Asks for missing details step by step
- **Room Recommendations** - Suggests rooms based on budget and preferences
- **Booking Flow Management** - Guides users through complete booking process

### ✅ **Database Integration:**
- **Chat History** - Stores all conversations
- **Booking Intent Tracking** - Tracks booking progress
- **Analytics** - Conversation analytics and conversion tracking
- **Session Management** - Manages user sessions across conversations

## 🔧 Customization Options

### 1. **Update Chatbot Responses:**
Edit the responses in `chatbot_schema.sql` in the `chatbot_responses` table:

```sql
UPDATE chatbot_responses 
SET response_text = 'Your custom greeting message here' 
WHERE intent_category = 'greeting';
```

### 2. **Add New Response Categories:**
```sql
INSERT INTO chatbot_responses (intent_category, trigger_keywords, response_text) 
VALUES ('custom_intent', '["keyword1", "keyword2"]', 'Your response here');
```

### 3. **Modify Hotline Number:**
Update the hotline number in both:
- `chatbot_service.py` (line 119, 124)
- `chatbot_schema.sql` (line 97, 110)

Change `011-4545678` to your actual hotline number.

## 🛠️ Troubleshooting

### **Issue: Python service won't start**
**Solution:**
```bash
# Check if port 8000 is available
netstat -an | findstr 8000

# Or use different port:
uvicorn chatbot_service:app --host 0.0.0.0 --port 8001
# Then update PYTHON_CHATBOT_URL in ChatbotService.java
```

### **Issue: Database connection failed**
**Solution:**
1. Verify MySQL is running
2. Check database credentials in `chatbot_service.py`
3. Ensure database `hotel_reservation_db` exists
4. Run: `mysql -u root -p -e "SHOW DATABASES;"`

### **Issue: Chat UI not loading**
**Solution:**
1. Check Spring Boot is running on port 8080
2. Clear browser cache
3. Check console for JavaScript errors
4. Verify `/chat` endpoint is accessible

### **Issue: Chatbot gives fallback responses**
**Solution:**
1. Check Python service is running on port 8000
2. Check network connectivity between Spring Boot and Python service
3. Look at Python service logs for errors

## 📊 Monitoring & Analytics

### **View Chat Statistics:**
```sql
-- Daily conversation stats
SELECT * FROM chat_conversion_stats;

-- Active sessions
SELECT * FROM active_chat_sessions_view;

-- Popular intents
SELECT intent, COUNT(*) as count 
FROM chat_messages 
WHERE message_type = 'USER' 
GROUP BY intent 
ORDER BY count DESC;
```

### **Monitor Booking Conversions:**
```sql
-- Booking conversion rate
SELECT 
    COUNT(*) as total_sessions,
    COUNT(CASE WHEN session_status = 'BOOKING_COMPLETED' THEN 1 END) as completed_bookings,
    ROUND((COUNT(CASE WHEN session_status = 'BOOKING_COMPLETED' THEN 1 END) * 100.0 / COUNT(*)), 2) as conversion_rate
FROM chat_sessions
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
```

## 🎉 Success!

Your hotel booking chatbot is now fully operational! Users can:

1. **Chat naturally** about room requirements
2. **Get real-time availability** information
3. **View room suggestions** with pricing
4. **Complete bookings** directly through chat
5. **Get immediate help** for urgent matters

The system integrates seamlessly with your existing hotel booking system and provides a modern, conversational booking experience.

## 🔮 Next Steps (Optional Enhancements)

1. **Advanced AI Model**: Train custom model with your specific data
2. **Multi-language Support**: Add support for multiple languages
3. **Voice Integration**: Add voice chat capabilities
4. **Rich Media**: Add image and video support in chat
5. **Integration**: Connect with WhatsApp, Facebook Messenger
6. **Analytics Dashboard**: Build admin dashboard for chat analytics

---

**Support:** If you need help, the chatbot will direct users to call: **011-4545678**

**Development:** All components are documented and ready for further customization!