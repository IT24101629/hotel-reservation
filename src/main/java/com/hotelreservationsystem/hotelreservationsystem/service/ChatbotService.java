package com.hotelreservationsystem.hotelreservationsystem.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelreservationsystem.hotelreservationsystem.dto.ChatMessageDTO;
import com.hotelreservationsystem.hotelreservationsystem.dto.ChatResponseDTO;
import com.hotelreservationsystem.hotelreservationsystem.model.BookingStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    @Autowired
    private BookingService bookingService;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String PYTHON_CHATBOT_URL = "http://localhost:8000";

    public ChatbotService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public ChatResponseDTO processMessage(ChatMessageDTO messageDTO) throws Exception {
        System.out.println("🤖 Calling Python chatbot service...");
        
        // Prepare request for Python service
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("session_id", messageDTO.getSessionId());
        requestBody.put("message", messageDTO.getMessage());
        requestBody.put("user_id", messageDTO.getUserId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            // Call Python chatbot service
            ResponseEntity<String> response = restTemplate.exchange(
                PYTHON_CHATBOT_URL + "/chat",
                HttpMethod.POST,
                request,
                String.class
            );

            // Parse response
            Map<String, Object> responseData = objectMapper.readValue(
                response.getBody(), 
                new TypeReference<Map<String, Object>>() {}
            );

            // Create ChatResponseDTO
            ChatResponseDTO chatResponse = new ChatResponseDTO();
            chatResponse.setSessionId((String) responseData.get("session_id"));
            chatResponse.setResponse((String) responseData.get("response"));
            chatResponse.setIntent((String) responseData.get("intent"));
            chatResponse.setEntities((Map<String, Object>) responseData.get("entities"));
            chatResponse.setBookingStatus((String) responseData.get("booking_status"));
            chatResponse.setBookingReady((Boolean) responseData.getOrDefault("booking_ready", false));
            chatResponse.setNeedsBookingDetails((Boolean) responseData.getOrDefault("needs_booking_details", false));
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> suggestedRooms = (List<Map<String, Object>>) responseData.get("suggested_rooms");
            chatResponse.setSuggestedRooms(suggestedRooms);

            System.out.println("✅ Python chatbot response processed");
            return chatResponse;

        } catch (Exception e) {
            System.err.println("❌ Error calling Python chatbot service: " + e.getMessage());
            
            // Fallback response
            ChatResponseDTO fallbackResponse = new ChatResponseDTO();
            fallbackResponse.setSessionId(messageDTO.getSessionId());
            fallbackResponse.setResponse(
                "I apologize, but I'm experiencing technical difficulties. 😔\n\n" +
                "For immediate assistance, please call our hotline: **011-4545678**\n\n" +
                "Our customer care team will contact you as soon as possible."
            );
            fallbackResponse.setIntent("error");
            fallbackResponse.setEntities(new HashMap<>());
            fallbackResponse.setBookingReady(false);
            fallbackResponse.setNeedsBookingDetails(false);
            
            return fallbackResponse;
        }
    }

    public Map<String, Object> completeBooking(String sessionId, Map<String, String> customerDetails, Integer roomId) throws Exception {
        System.out.println("🏨 Completing booking from chat session: " + sessionId);
        
        try {
            // Get booking intent from Python service
            Map<String, Object> sessionStatus = getSessionStatus(sessionId);
            
            // Extract booking details
            String checkInDate = (String) sessionStatus.get("check_in_date");
            String checkOutDate = (String) sessionStatus.get("check_out_date");
            Integer numberOfGuests = (Integer) sessionStatus.get("number_of_guests");
            String specialRequests = (String) sessionStatus.get("special_requests");
            
            // Create booking through existing booking service
            // Note: You'll need to adapt this to work with your existing BookingService
            // This is a simplified version
            Map<String, Object> bookingData = new HashMap<>();
            bookingData.put("roomId", roomId);
            bookingData.put("checkInDate", checkInDate);
            bookingData.put("checkOutDate", checkOutDate);
            bookingData.put("numberOfGuests", numberOfGuests);
            bookingData.put("specialRequests", specialRequests != null ? specialRequests : "");
            bookingData.put("customerName", customerDetails.get("name"));
            bookingData.put("email", customerDetails.get("email"));
            bookingData.put("phone", customerDetails.get("phone"));
            
            // Update chat session status to completed
            updateSessionStatus(sessionId, "BOOKING_COMPLETED");
            
            // Return booking confirmation
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("bookingId", "BK" + System.currentTimeMillis()); // Temporary booking ID
            result.put("message", "Booking completed successfully!");
            
            System.out.println("✅ Chat booking completed successfully");
            return result;
            
        } catch (Exception e) {
            System.err.println("❌ Error completing chat booking: " + e.getMessage());
            throw new Exception("Failed to complete booking: " + e.getMessage());
        }
    }

    public Map<String, Object> getSessionStatus(String sessionId) throws Exception {
        try {
            // Call Python service to get session status
            ResponseEntity<String> response = restTemplate.getForEntity(
                PYTHON_CHATBOT_URL + "/session/" + sessionId + "/status",
                String.class
            );
            
            return objectMapper.readValue(
                response.getBody(),
                new TypeReference<Map<String, Object>>() {}
            );
            
        } catch (Exception e) {
            System.err.println("❌ Error getting session status: " + e.getMessage());
            throw new Exception("Failed to get session status");
        }
    }

    public void selectRoom(String sessionId, Integer roomId) throws Exception {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("room_id", roomId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(
                PYTHON_CHATBOT_URL + "/session/" + sessionId + "/select-room",
                HttpMethod.POST,
                request,
                String.class
            );
            
            System.out.println("✅ Room selected successfully for session: " + sessionId);
            
        } catch (Exception e) {
            System.err.println("❌ Error selecting room: " + e.getMessage());
            throw new Exception("Failed to select room");
        }
    }

    private void updateSessionStatus(String sessionId, String status) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("status", status);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(
                PYTHON_CHATBOT_URL + "/session/" + sessionId + "/status",
                HttpMethod.PUT,
                request,
                String.class
            );
            
        } catch (Exception e) {
            System.err.println("❌ Error updating session status: " + e.getMessage());
        }
    }

    public boolean isPythonServiceHealthy() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                PYTHON_CHATBOT_URL + "/health",
                String.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}