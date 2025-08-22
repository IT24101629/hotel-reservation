package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.Room;
import com.hotelreservationsystem.hotelreservationsystem.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomApiController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchRooms(
            @RequestParam(required = false) String checkInDate,
            @RequestParam(required = false) String checkOutDate,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String priceRange) {

        List<Room> rooms;

        if (checkInDate != null && checkOutDate != null) {
            rooms = roomService.findAvailableRoomsForDates(checkInDate, checkOutDate, guests, roomType);
        } else {
            rooms = roomService.getAllAvailableRooms();
        }

        // Filter by room type if specified
        if (roomType != null && !roomType.isEmpty()) {
            rooms = rooms.stream()
                    .filter(room -> room.getRoomType().getTypeName().equalsIgnoreCase(roomType))
                    .collect(Collectors.toList());
        }

        // Filter by guest capacity if specified
        if (guests != null) {
            rooms = rooms.stream()
                    .filter(room -> room.getRoomType().getMaxOccupancy() >= guests)
                    .collect(Collectors.toList());
        }

        // Convert to response format
        List<Map<String, Object>> roomData = rooms.stream()
                .map(this::convertRoomToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(roomData);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Map<String, Object>>> getAvailableRooms() {
        List<Room> rooms = roomService.getAllAvailableRooms();
        List<Map<String, Object>> roomData = rooms.stream()
                .map(this::convertRoomToMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roomData);
    }

    @PostMapping("/available")
    public ResponseEntity<List<Map<String, Object>>> searchAvailableRooms(@RequestBody Map<String, Object> searchRequest) {
        try {
            String checkInDate = (String) searchRequest.get("checkInDate");
            String checkOutDate = (String) searchRequest.get("checkOutDate");
            Integer numberOfGuests = (Integer) searchRequest.get("numberOfGuests");
            
            System.out.println("🔍 Chat API searching rooms for: " + checkInDate + " to " + checkOutDate + " for " + numberOfGuests + " guests");
            
            List<Room> rooms;
            
            if (checkInDate != null && checkOutDate != null) {
                rooms = roomService.findAvailableRoomsForDates(checkInDate, checkOutDate, numberOfGuests, null);
            } else {
                rooms = roomService.getAllAvailableRooms();
                
                // Filter by guest capacity if specified
                if (numberOfGuests != null) {
                    final Integer guests = numberOfGuests;
                    rooms = rooms.stream()
                            .filter(room -> room.getRoomType().getMaxOccupancy() >= guests)
                            .collect(Collectors.toList());
                }
            }
            
            // Convert to response format for chatbot
            List<Map<String, Object>> roomData = rooms.stream()
                    .limit(4) // Limit to top 4 results for chat
                    .map(this::convertRoomToMap)
                    .collect(Collectors.toList());
            
            System.out.println("✅ Found " + roomData.size() + " available rooms for chat");
            return ResponseEntity.ok(roomData);
            
        } catch (Exception e) {
            System.err.println("❌ Error searching rooms for chat: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    private Map<String, Object> convertRoomToMap(Room room) {
        Map<String, Object> roomMap = new HashMap<>();
        roomMap.put("roomId", room.getRoomId());
        roomMap.put("roomNumber", room.getRoomNumber());
        roomMap.put("roomType", room.getRoomType().getTypeName());
        roomMap.put("description", room.getDescription());
        roomMap.put("pricePerNight", room.getPricePerNight());
        roomMap.put("floorNumber", room.getFloorNumber());
        roomMap.put("maxOccupancy", room.getRoomType().getMaxOccupancy());
        roomMap.put("amenities", room.getAmenities());
        roomMap.put("imageUrl", room.getImageUrl());
        roomMap.put("status", room.getStatus().toString());
        return roomMap;
    }
}