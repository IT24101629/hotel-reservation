#!/usr/bin/env python3
"""
Hotel Booking Chatbot - Dataset Processor
Downloads and processes the Hugging Face hotel booking assistant dataset
"""

import json
import re
import pandas as pd
from datetime import datetime, timedelta
import random
from typing import List, Dict, Tuple
import requests
import os

class HotelChatbotDataProcessor:
    def __init__(self):
        self.raw_conversations = []
        self.processed_conversations = []
        self.training_data = []
        
    def download_dataset(self):
        """Download the dataset from Hugging Face"""
        print("📥 Downloading hotel booking assistant dataset...")
        
        # For now, we'll create sample data based on the dataset pattern
        # In production, you would use: dataset = load_dataset("M-A-E/hotel-booking-assistant-raw-chats")
        
        sample_conversations = [
            {
                "conversation_id": "conv_001",
                "messages": [
                    {"role": "user", "content": "Hi, I need a hotel in Berlin for 2 nights"},
                    {"role": "assistant", "content": "Hello! I'd be happy to help you find a hotel in Berlin. Could you please tell me your check-in and check-out dates, and how many guests will be staying?"},
                    {"role": "user", "content": "Check-in December 25th, check-out December 27th, for 2 guests"},
                    {"role": "assistant", "content": "Perfect! For 2 guests from December 25-27 in Berlin, I have several great options:\n\n1. **Berlin Grand Hotel** - €120/night\n   - 4.5★ rating\n   - City center location\n   - Free WiFi, Breakfast included\n\n2. **Modern Berlin Suite** - €95/night\n   - 4.2★ rating\n   - Near Brandenburg Gate\n   - Free WiFi, Gym access\n\nWould you like more details about either of these options?"},
                    {"role": "user", "content": "Tell me more about the Berlin Grand Hotel"},
                    {"role": "assistant", "content": "**Berlin Grand Hotel** is an excellent choice!\n\n📍 **Location**: City center, 2 minutes walk to main shopping street\n⭐ **Rating**: 4.5/5 (based on 1,247 reviews)\n💰 **Price**: €120 per night (€240 total for 2 nights)\n\n🏨 **Amenities**:\n- Spacious double room with city view\n- Free high-speed WiFi\n- Complimentary breakfast buffet\n- 24-hour room service\n- Fitness center and spa\n- Airport shuttle service\n\nWould you like to book this hotel?"},
                    {"role": "user", "content": "Yes, I want to book it"},
                    {"role": "assistant", "content": "Excellent choice! I'll help you book the Berlin Grand Hotel.\n\n📋 **Booking Summary**:\n- Hotel: Berlin Grand Hotel\n- Dates: Dec 25-27, 2024 (2 nights)\n- Guests: 2 people\n- Room: Double room with city view\n- Total: €240\n\nTo complete your booking, I'll need:\n1. Your full name\n2. Email address\n3. Phone number\n\nShall we proceed?"}
                ]
            },
            # Add more sample conversations...
        ]
        
        self.raw_conversations = sample_conversations
        print(f"✅ Downloaded {len(self.raw_conversations)} conversations")
        
    def clean_and_process_data(self):
        """Clean and process the raw conversation data"""
        print("🧹 Cleaning and processing dataset...")
        
        for conv in self.raw_conversations:
            processed_conv = {
                "conversation_id": conv["conversation_id"],
                "messages": [],
                "booking_info": {
                    "location": None,
                    "check_in": None,
                    "check_out": None,
                    "guests": None,
                    "budget": None,
                    "room_type": None
                }
            }
            
            for i, msg in enumerate(conv["messages"]):
                cleaned_content = self.clean_message(msg["content"])
                
                # Extract entities from messages
                entities = self.extract_entities(cleaned_content)
                
                # Classify intent
                intent = self.classify_intent(cleaned_content, msg["role"])
                
                processed_msg = {
                    "role": msg["role"],
                    "content": cleaned_content,
                    "intent": intent,
                    "entities": entities,
                    "timestamp": i
                }
                
                processed_conv["messages"].append(processed_msg)
                
                # Update booking info based on extracted entities
                self.update_booking_info(processed_conv["booking_info"], entities)
            
            self.processed_conversations.append(processed_conv)
        
        print(f"✅ Processed {len(self.processed_conversations)} conversations")
    
    def clean_message(self, message: str) -> str:
        """Clean individual message content"""
        # Remove extra whitespace
        message = re.sub(r'\s+', ' ', message.strip())
        
        # Normalize currency symbols
        message = re.sub(r'€(\d+)', r'EUR \1', message)
        message = re.sub(r'\$(\d+)', r'USD \1', message)
        
        # Normalize date formats
        message = re.sub(r'(\d{1,2})(st|nd|rd|th)', r'\1', message)
        
        return message
    
    def extract_entities(self, message: str) -> Dict:
        """Extract entities from message content"""
        entities = {}
        
        # Extract dates
        date_patterns = [
            r'(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})',
            r'(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2})',
            r'Dec\s+(\d{1,2})',
            r'(\d{1,2})\s*(st|nd|rd|th)?\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)'
        ]
        
        for pattern in date_patterns:
            dates = re.findall(pattern, message, re.IGNORECASE)
            if dates:
                entities['dates'] = dates
                break
        
        # Extract guest count
        guest_match = re.search(r'(\d+)\s*(guest|people|person)', message, re.IGNORECASE)
        if guest_match:
            entities['guests'] = int(guest_match.group(1))
        
        # Extract budget/price
        price_match = re.search(r'(EUR|USD|\$|€)\s*(\d+)', message, re.IGNORECASE)
        if price_match:
            entities['price'] = {
                'currency': price_match.group(1),
                'amount': int(price_match.group(2))
            }
        
        # Extract locations
        locations = ['Berlin', 'Munich', 'Frankfurt', 'Rome', 'Barcelona', 'Paris', 'London']
        for location in locations:
            if location.lower() in message.lower():
                entities['location'] = location
                break
        
        # Extract room types
        room_types = ['single', 'double', 'suite', 'deluxe', 'standard', 'family']
        for room_type in room_types:
            if room_type.lower() in message.lower():
                entities['room_type'] = room_type
                break
                
        return entities
    
    def classify_intent(self, message: str, role: str) -> str:
        """Classify the intent of a message"""
        if role == "user":
            # User intents
            if any(word in message.lower() for word in ['hello', 'hi', 'hey']):
                return 'greeting'
            elif any(word in message.lower() for word in ['book', 'reserve', 'want to book']):
                return 'booking_request'
            elif any(word in message.lower() for word in ['need', 'looking for', 'find']):
                return 'hotel_search'
            elif any(word in message.lower() for word in ['tell me more', 'details', 'about']):
                return 'request_details'
            elif any(word in message.lower() for word in ['yes', 'sure', 'ok', 'proceed']):
                return 'confirmation'
            elif 'check-in' in message.lower() or 'check-out' in message.lower():
                return 'provide_dates'
            else:
                return 'general_inquiry'
        else:
            # Assistant intents
            if 'hello' in message.lower() or 'welcome' in message.lower():
                return 'greeting_response'
            elif 'would you like' in message.lower():
                return 'offer_assistance'
            elif 'booking summary' in message.lower():
                return 'booking_summary'
            elif any(word in message.lower() for word in ['hotel', 'options', 'available']):
                return 'hotel_suggestion'
            elif 'need' in message.lower() and any(word in message.lower() for word in ['name', 'email', 'phone']):
                return 'request_booking_details'
            else:
                return 'information_response'
    
    def update_booking_info(self, booking_info: Dict, entities: Dict):
        """Update booking information based on extracted entities"""
        if 'location' in entities:
            booking_info['location'] = entities['location']
        if 'guests' in entities:
            booking_info['guests'] = entities['guests']
        if 'price' in entities:
            booking_info['budget'] = entities['price']['amount']
        if 'room_type' in entities:
            booking_info['room_type'] = entities['room_type']
        if 'dates' in entities and len(entities['dates']) >= 2:
            booking_info['check_in'] = entities['dates'][0]
            booking_info['check_out'] = entities['dates'][1]
    
    def create_training_data(self):
        """Create training data in the required format"""
        print("🎯 Creating training data...")
        
        for conv in self.processed_conversations:
            conversation_context = []
            
            for i, msg in enumerate(conv["messages"]):
                if msg["role"] == "user" and i + 1 < len(conv["messages"]):
                    # Create training pair: user message -> bot response
                    user_msg = msg["content"]
                    bot_response = conv["messages"][i + 1]["content"]
                    
                    training_example = {
                        "input": user_msg,
                        "output": bot_response,
                        "context": " ".join([m["content"] for m in conversation_context[-3:]]),  # Last 3 messages
                        "intent": msg["intent"],
                        "entities": msg["entities"],
                        "conversation_id": conv["conversation_id"]
                    }
                    
                    self.training_data.append(training_example)
                
                conversation_context.append(msg)
        
        print(f"✅ Created {len(self.training_data)} training examples")
    
    def save_processed_data(self):
        """Save processed data to files"""
        print("💾 Saving processed data...")
        
        # Save training data
        with open('hotel_chatbot_training_data.json', 'w', encoding='utf-8') as f:
            json.dump(self.training_data, f, indent=2, ensure_ascii=False)
        
        # Save processed conversations
        with open('processed_conversations.json', 'w', encoding='utf-8') as f:
            json.dump(self.processed_conversations, f, indent=2, ensure_ascii=False)
        
        # Create CSV for easier analysis
        df = pd.DataFrame(self.training_data)
        df.to_csv('hotel_chatbot_training_data.csv', index=False)
        
        print("✅ Data saved successfully!")
        print(f"📊 Training examples: {len(self.training_data)}")
        print(f"📂 Files created:")
        print("  - hotel_chatbot_training_data.json")
        print("  - processed_conversations.json") 
        print("  - hotel_chatbot_training_data.csv")

def main():
    """Main processing pipeline"""
    print("🤖 Hotel Booking Chatbot - Data Processor")
    print("=" * 50)
    
    processor = HotelChatbotDataProcessor()
    
    try:
        # Step 1: Download dataset
        processor.download_dataset()
        
        # Step 2: Clean and process
        processor.clean_and_process_data()
        
        # Step 3: Create training data
        processor.create_training_data()
        
        # Step 4: Save processed data
        processor.save_processed_data()
        
        print("\n✅ Dataset processing completed successfully!")
        print("\nNext steps:")
        print("1. Run the model training script")
        print("2. Start the chatbot service")
        print("3. Integrate with Spring Boot application")
        
    except Exception as e:
        print(f"❌ Error during processing: {e}")
        raise

if __name__ == "__main__":
    main()