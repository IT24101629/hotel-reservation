package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.dto.ChatMessageDTO;
import com.hotelreservationsystem.hotelreservationsystem.dto.ChatResponseDTO;
import com.hotelreservationsystem.hotelreservationsystem.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/chat")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @GetMapping
    public String chatPage(Model model) {
        System.out.println("🤖 Chat page accessed");
        return "chat-standalone";
    }

    @PostMapping("/message")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody ChatMessageDTO messageDTO) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("💬 Processing chat message: " + messageDTO.getMessage());
            
            // Call Python chatbot service
            ChatResponseDTO chatResponse = chatbotService.processMessage(messageDTO);
            
            response.put("success", true);
            response.put("sessionId", chatResponse.getSessionId());
            response.put("response", chatResponse.getResponse());
            response.put("intent", chatResponse.getIntent());
            response.put("bookingReady", chatResponse.isBookingReady());
            response.put("suggestedRooms", chatResponse.getSuggestedRooms());
            response.put("needsBookingDetails", chatResponse.isNeedsBookingDetails());
            
            System.out.println("✅ Chat response generated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error processing chat message: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to process message: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/book")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bookFromChat(@RequestBody Map<String, Object> bookingData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🏨 Processing booking from chat");
            
            String sessionId = (String) bookingData.get("sessionId");
            @SuppressWarnings("unchecked")
            Map<String, String> customerDetails = (Map<String, String>) bookingData.get("customerDetails");
            Integer roomId = (Integer) bookingData.get("roomId");
            
            // Process the booking through chatbot service
            Map<String, Object> bookingResult = chatbotService.completeBooking(sessionId, customerDetails, roomId);
            
            response.put("success", true);
            response.put("bookingId", bookingResult.get("bookingId"));
            response.put("message", "Booking completed successfully!");
            
            System.out.println("✅ Booking completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error completing booking: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to complete booking: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/session/{sessionId}/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSessionStatus(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> sessionStatus = chatbotService.getSessionStatus(sessionId);
            response.put("success", true);
            response.putAll(sessionStatus);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting session status: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to get session status");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/session/{sessionId}/select-room")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> selectRoom(
            @PathVariable String sessionId, 
            @RequestBody Map<String, Integer> roomSelection) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Integer roomId = roomSelection.get("roomId");
            chatbotService.selectRoom(sessionId, roomId);
            
            response.put("success", true);
            response.put("message", "Room selected successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error selecting room: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Failed to select room");
            return ResponseEntity.badRequest().body(response);
        }
    }
}