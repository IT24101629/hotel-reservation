#!/usr/bin/env python3
"""
Hotel Booking Chatbot Service
FastAPI service that handles chat interactions and integrates with the hotel booking system
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Optional, Any
import json
import re
import uuid
import mysql.connector
from datetime import datetime, timedelta
import requests
import os
from dataclasses import dataclass
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Hotel Booking Chatbot API", version="1.0.0")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Database configuration
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',  # Update with your MySQL credentials
    'password': 'password',  # Update with your MySQL password
    'database': 'hotel_reservation_db',
    'charset': 'utf8mb4'
}

# Spring Boot API base URL
SPRING_BOOT_API = "http://localhost:8080/api"

# Pydantic models
class ChatMessage(BaseModel):
    session_id: Optional[str] = None
    message: str
    user_id: Optional[int] = None

class ChatResponse(BaseModel):
    session_id: str
    response: str
    intent: str
    entities: Dict[str, Any]
    booking_status: Optional[str] = None
    suggested_rooms: Optional[List[Dict]] = None
    booking_ready: bool = False
    needs_booking_details: bool = False

class BookingRequest(BaseModel):
    session_id: str
    customer_details: Dict[str, str]

@dataclass
class BookingIntent:
    session_id: str
    check_in_date: Optional[str] = None
    check_out_date: Optional[str] = None
    number_of_guests: Optional[int] = None
    room_type_preference: Optional[str] = None
    budget_max: Optional[float] = None
    special_requests: Optional[str] = None
    booking_status: str = "COLLECTING_INFO"
    suggested_rooms: Optional[List[Dict]] = None
    selected_room_id: Optional[int] = None

class HotelChatbot:
    def __init__(self):
        self.load_response_templates()
        self.conversation_memory = {}  # In-memory storage for demo
        
    def load_response_templates(self):
        """Load predefined response templates"""
        self.templates = {
            'greeting': [
                "Hello! 🏨 Welcome to Gold Palm Hotel! I'm here to help you find the perfect room for your stay. How can I assist you today?",
                "Hi there! 🌟 I'm your hotel booking assistant. I can help you check availability, get room details, or make a reservation. What would you like to do?",
            ],
            'booking_inquiry': [
                "I'd love to help you book a room! 🛏️ To find the perfect accommodation for you, I'll need a few details:\n\n1. Check-in date\n2. Check-out date\n3. Number of guests\n\nCould you please share these details?",
            ],
            'date_request': [
                "Please provide your preferred check-in and check-out dates. You can say something like:\n• 'I need a room from December 25 to December 28'\n• 'Check-in: 25th Dec, Check-out: 28th Dec'",
            ],
            'guest_count_request': [
                "How many guests will be staying? This helps me suggest the right room type for you.",
            ],
            'room_suggestions': [
                "Here are our available rooms for your dates:\n\n{room_list}\n\nWould you like more details about any of these rooms?",
            ],
            'out_of_scope': [
                "I apologize, but I can only assist with hotel booking and room-related inquiries. 😔\n\nFor other matters, our customer care team will contact you soon, or you can call our hotline immediately: **011-4545678**\n\nIs there anything about our hotel rooms or booking process I can help you with?",
            ],
            'emergency': [
                "I understand you need immediate assistance! 🚨\n\nFor urgent matters, please call our hotline: **011-4545678**\n\nOur customer care team will also contact you as soon as possible.\n\nIf this is a booking-related question, I'm here to help!",
            ]
        }
    
    def get_db_connection(self):
        """Get database connection"""
        try:
            return mysql.connector.connect(**DB_CONFIG)
        except mysql.connector.Error as e:
            logger.error(f"Database connection error: {e}")
            raise HTTPException(status_code=500, detail="Database connection failed")
    
    def create_chat_session(self, user_id: Optional[int] = None) -> str:
        """Create a new chat session"""
        session_id = str(uuid.uuid4())
        
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "INSERT INTO chat_sessions (session_id, user_id, session_status, user_ip) VALUES (%s, %s, %s, %s)",
                (session_id, user_id, 'ACTIVE', '127.0.0.1')
            )
            
            # Create initial booking intent
            cursor.execute(
                "INSERT INTO chat_booking_intents (session_id, booking_status) VALUES (%s, %s)",
                (session_id, 'COLLECTING_INFO')
            )
            
            conn.commit()
            cursor.close()
            conn.close()
            
            logger.info(f"Created new chat session: {session_id}")
            return session_id
            
        except Exception as e:
            logger.error(f"Error creating chat session: {e}")
            raise HTTPException(status_code=500, detail="Failed to create chat session")
    
    def save_message(self, session_id: str, message_type: str, content: str, intent: str, entities: Dict):
        """Save message to database"""
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "INSERT INTO chat_messages (session_id, message_type, content, intent, entities) VALUES (%s, %s, %s, %s, %s)",
                (session_id, message_type, content, intent, json.dumps(entities))
            )
            
            conn.commit()
            cursor.close()
            conn.close()
            
        except Exception as e:
            logger.error(f"Error saving message: {e}")
    
    def extract_entities(self, message: str) -> Dict[str, Any]:
        """Extract entities from user message"""
        entities = {}
        
        # Extract dates
        date_patterns = [
            r'(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})',
            r'(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2})',
            r'(Dec|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov)\s+(\d{1,2})',
            r'(\d{1,2})\s*(st|nd|rd|th)?\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)'
        ]
        
        dates_found = []
        for pattern in date_patterns:
            matches = re.findall(pattern, message, re.IGNORECASE)
            if matches:
                dates_found.extend(matches)
        
        if dates_found:
            entities['dates'] = dates_found
        
        # Extract guest count
        guest_patterns = [
            r'(\d+)\s*(guest|guests|people|person|persons)',
            r'for\s+(\d+)',
            r'(\d+)\s*(adult|adults)'
        ]
        
        for pattern in guest_patterns:
            match = re.search(pattern, message, re.IGNORECASE)
            if match:
                entities['guests'] = int(match.group(1))
                break
        
        # Extract room types
        room_types = {
            'single': ['single', 'one person', '1 person'],
            'double': ['double', 'two people', '2 people', 'couple'],
            'deluxe': ['deluxe', 'luxury', 'premium'],
            'family': ['family', 'family room', 'large room'],
            'suite': ['suite', 'presidential', 'executive']
        }
        
        message_lower = message.lower()
        for room_type, keywords in room_types.items():
            if any(keyword in message_lower for keyword in keywords):
                entities['room_type'] = room_type
                break
        
        # Extract budget
        budget_match = re.search(r'(under|below|less than|max|maximum)\s*(LKR|Rs\.?)\s*(\d+)', message, re.IGNORECASE)
        if budget_match:
            entities['budget_max'] = int(budget_match.group(3))
        
        return entities
    
    def classify_intent(self, message: str) -> str:
        """Classify the intent of user message"""
        message_lower = message.lower()
        
        # Greeting
        if any(word in message_lower for word in ['hello', 'hi', 'hey', 'good morning', 'good evening']):
            return 'greeting'
        
        # Booking request
        if any(word in message_lower for word in ['book', 'reservation', 'reserve', 'want to book', 'need a room']):
            return 'booking_request'
        
        # Room inquiry
        if any(word in message_lower for word in ['room', 'rooms', 'available', 'availability']):
            return 'room_inquiry'
        
        # Date/time related
        if any(word in message_lower for word in ['check-in', 'check in', 'check-out', 'check out', 'dates']):
            return 'date_inquiry'
        
        # Guest count
        if any(word in message_lower for word in ['guest', 'people', 'person', 'how many']):
            return 'guest_inquiry'
        
        # Request details
        if any(word in message_lower for word in ['details', 'more info', 'tell me more', 'about']):
            return 'request_details'
        
        # Confirmation
        if any(word in message_lower for word in ['yes', 'sure', 'ok', 'okay', 'proceed', 'confirm']):
            return 'confirmation'
        
        # Out of scope
        if any(word in message_lower for word in ['weather', 'restaurant', 'flight', 'transport', 'directions']):
            return 'out_of_scope'
        
        # Emergency
        if any(word in message_lower for word in ['complaint', 'problem', 'issue', 'emergency', 'urgent', 'help']):
            return 'emergency'
        
        return 'general_inquiry'
    
    def get_booking_intent(self, session_id: str) -> BookingIntent:
        """Get current booking intent from database"""
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.execute(
                "SELECT * FROM chat_booking_intents WHERE session_id = %s ORDER BY created_at DESC LIMIT 1",
                (session_id,)
            )
            
            result = cursor.fetchone()
            cursor.close()
            conn.close()
            
            if result:
                return BookingIntent(
                    session_id=result[1],
                    check_in_date=result[2].isoformat() if result[2] else None,
                    check_out_date=result[3].isoformat() if result[3] else None,
                    number_of_guests=result[4],
                    room_type_preference=result[5],
                    budget_max=float(result[6]) if result[6] else None,
                    special_requests=result[7],
                    booking_status=result[10],
                    suggested_rooms=json.loads(result[11]) if result[11] else None,
                    selected_room_id=result[12]
                )
            else:
                # Create new booking intent
                return BookingIntent(session_id=session_id)
                
        except Exception as e:
            logger.error(f"Error getting booking intent: {e}")
            return BookingIntent(session_id=session_id)
    
    def update_booking_intent(self, booking_intent: BookingIntent, entities: Dict):
        """Update booking intent with new entities"""
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            # Update entities
            if 'dates' in entities and len(entities['dates']) >= 2:
                booking_intent.check_in_date = entities['dates'][0]
                booking_intent.check_out_date = entities['dates'][1] if len(entities['dates']) > 1 else None
            
            if 'guests' in entities:
                booking_intent.number_of_guests = entities['guests']
            
            if 'room_type' in entities:
                booking_intent.room_type_preference = entities['room_type']
            
            if 'budget_max' in entities:
                booking_intent.budget_max = entities['budget_max']
            
            # Update database
            cursor.execute("""
                UPDATE chat_booking_intents 
                SET check_in_date = %s, check_out_date = %s, number_of_guests = %s, 
                    room_type_preference = %s, budget_max = %s, updated_at = NOW()
                WHERE session_id = %s
            """, (
                booking_intent.check_in_date,
                booking_intent.check_out_date,
                booking_intent.number_of_guests,
                booking_intent.room_type_preference,
                booking_intent.budget_max,
                booking_intent.session_id
            ))
            
            conn.commit()
            cursor.close()
            conn.close()
            
        except Exception as e:
            logger.error(f"Error updating booking intent: {e}")
    
    def search_available_rooms(self, booking_intent: BookingIntent) -> List[Dict]:
        """Search for available rooms based on booking intent"""
        if not all([booking_intent.check_in_date, booking_intent.check_out_date, booking_intent.number_of_guests]):
            return []
        
        try:
            conn = self.get_db_connection()
            cursor = conn.cursor()
            
            cursor.callproc('FindRoomsForChatBooking', [
                booking_intent.check_in_date,
                booking_intent.check_out_date,
                booking_intent.number_of_guests,
                booking_intent.budget_max
            ])
            
            rooms = []
            for result in cursor.stored_results():
                rows = result.fetchall()
                for row in rows:
                    room = {
                        'room_id': row[0],
                        'room_number': row[1],
                        'price_per_night': float(row[2]),
                        'description': row[3],
                        'image_url': row[4],
                        'type_name': row[5],
                        'max_occupancy': row[6],
                        'amenities': json.loads(row[7]) if row[7] else [],
                        'total_cost': float(row[8])
                    }
                    rooms.append(room)
            
            cursor.close()
            conn.close()
            
            return rooms[:5]  # Return top 5 options
            
        except Exception as e:
            logger.error(f"Error searching rooms: {e}")
            return []
    
    def format_room_suggestions(self, rooms: List[Dict]) -> str:
        """Format room suggestions for chat response"""
        if not rooms:
            return "I'm sorry, no rooms are available for your selected dates and requirements. Would you like to try different dates?"
        
        suggestions = ""
        for i, room in enumerate(rooms, 1):
            amenities_str = ", ".join(room['amenities'][:3])  # Show first 3 amenities
            suggestions += f"{i}. **{room['type_name']} - Room {room['room_number']}**\n"
            suggestions += f"   💰 LKR {room['price_per_night']:,.0f}/night (Total: LKR {room['total_cost']:,.0f})\n"
            suggestions += f"   👥 Up to {room['max_occupancy']} guests\n"
            suggestions += f"   ✨ {amenities_str}\n\n"
        
        suggestions += "Would you like to book any of these rooms? Just let me know the room number!"
        return suggestions
    
    def generate_response(self, message: str, session_id: str) -> ChatResponse:
        """Generate chatbot response"""
        # Extract entities and classify intent
        entities = self.extract_entities(message)
        intent = self.classify_intent(message)
        
        # Get current booking intent
        booking_intent = self.get_booking_intent(session_id)
        
        # Update booking intent with new entities
        self.update_booking_intent(booking_intent, entities)
        
        # Generate response based on intent
        response_text = ""
        suggested_rooms = None
        booking_ready = False
        needs_booking_details = False
        
        if intent == 'greeting':
            response_text = self.templates['greeting'][0]
        
        elif intent in ['booking_request', 'room_inquiry']:
            if not any([booking_intent.check_in_date, booking_intent.check_out_date]):
                response_text = self.templates['booking_inquiry'][0]
            elif not booking_intent.number_of_guests:
                response_text = self.templates['guest_count_request'][0]
            else:
                # Search for rooms
                rooms = self.search_available_rooms(booking_intent)
                suggested_rooms = rooms
                response_text = self.format_room_suggestions(rooms)
                
                if rooms:
                    booking_intent.booking_status = 'SHOWING_OPTIONS'
                    booking_ready = True
        
        elif intent == 'date_inquiry':
            response_text = self.templates['date_request'][0]
        
        elif intent == 'guest_inquiry':
            response_text = self.templates['guest_count_request'][0]
        
        elif intent == 'confirmation':
            if booking_intent.booking_status == 'SHOWING_OPTIONS':
                needs_booking_details = True
                response_text = "Great! I'll help you complete the booking. To proceed, I'll need:\n\n1. Your full name\n2. Email address\n3. Phone number\n\nPlease provide these details to complete your reservation."
                booking_intent.booking_status = 'READY_TO_BOOK'
        
        elif intent == 'out_of_scope':
            response_text = self.templates['out_of_scope'][0]
        
        elif intent == 'emergency':
            response_text = self.templates['emergency'][0]
        
        else:
            # Check if we need more booking information
            if not booking_intent.check_in_date or not booking_intent.check_out_date:
                response_text = "To help you find the perfect room, I'll need your check-in and check-out dates. When would you like to stay?"
            elif not booking_intent.number_of_guests:
                response_text = "How many guests will be staying?"
            else:
                response_text = "I'm here to help with your hotel booking! Is there something specific you'd like to know about our rooms or services?"
        
        # Save messages to database
        self.save_message(session_id, 'USER', message, intent, entities)
        self.save_message(session_id, 'BOT', response_text, 'response', {})
        
        return ChatResponse(
            session_id=session_id,
            response=response_text,
            intent=intent,
            entities=entities,
            booking_status=booking_intent.booking_status,
            suggested_rooms=suggested_rooms,
            booking_ready=booking_ready,
            needs_booking_details=needs_booking_details
        )

# Initialize chatbot
chatbot = HotelChatbot()

@app.post("/chat", response_model=ChatResponse)
async def chat_endpoint(message: ChatMessage):
    """Main chat endpoint"""
    try:
        # Create session if not provided
        if not message.session_id:
            session_id = chatbot.create_chat_session(message.user_id)
        else:
            session_id = message.session_id
        
        # Generate response
        response = chatbot.generate_response(message.message, session_id)
        
        return response
        
    except Exception as e:
        logger.error(f"Error in chat endpoint: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/book")
async def book_room(booking_request: BookingRequest):
    """Complete booking from chat"""
    try:
        # Get booking intent
        booking_intent = chatbot.get_booking_intent(booking_request.session_id)
        
        if not booking_intent.selected_room_id:
            raise HTTPException(status_code=400, detail="No room selected")
        
        # Call Spring Boot booking API
        booking_data = {
            "roomId": booking_intent.selected_room_id,
            "checkInDate": booking_intent.check_in_date,
            "checkOutDate": booking_intent.check_out_date,
            "numberOfGuests": booking_intent.number_of_guests,
            "specialRequests": booking_intent.special_requests or "",
            "customerName": booking_request.customer_details.get("name"),
            "email": booking_request.customer_details.get("email"),
            "phone": booking_request.customer_details.get("phone")
        }
        
        # Make API call to Spring Boot (this would be implemented)
        # response = requests.post(f"{SPRING_BOOT_API}/bookings", json=booking_data)
        
        # For now, return success
        return {"success": True, "message": "Booking completed successfully!"}
        
    except Exception as e:
        logger.error(f"Error in booking: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {"status": "healthy", "service": "Hotel Booking Chatbot"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)