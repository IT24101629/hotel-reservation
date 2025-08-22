package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.dto.BookingResponseDTO;
import com.hotelreservationsystem.hotelreservationsystem.model.BookingStatus;
import com.hotelreservationsystem.hotelreservationsystem.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/receptionist")
@PreAuthorize("hasRole('STAFF')")
public class ReceptionistController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        System.out.println("🏨 Receptionist dashboard accessed");
        return "receptionist-dashboard-standalone";
    }

    @PostMapping("/booking/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchBooking(@RequestParam String bookingId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🔍 Searching for booking: " + bookingId);
            BookingResponseDTO booking;
            
            // Try to determine if it's a booking ID (number) or booking reference (BK...)
            if (bookingId.matches("\\d+")) {
                // It's a numeric booking ID
                Long id = Long.parseLong(bookingId);
                booking = bookingService.getBookingById(id);
                System.out.println("✅ Booking found by ID: " + booking.getBookingReference());
            } else if (bookingId.matches("BK\\d+")) {
                // It's a booking reference (starts with BK)
                booking = bookingService.getBookingByReference(bookingId);
                System.out.println("✅ Booking found by reference: " + booking.getBookingReference());
            } else {
                System.err.println("❌ Invalid booking format: " + bookingId);
                response.put("success", false);
                response.put("error", "Invalid booking format. Use booking ID (number) or booking reference (BK...)");
                return ResponseEntity.badRequest().body(response);
            }
            
            response.put("success", true);
            response.put("booking", booking);
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Invalid booking ID format: " + bookingId);
            response.put("success", false);
            response.put("error", "Invalid booking ID format");
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            System.err.println("❌ Booking not found: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Booking not found");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/booking/{id}/checkin")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkInBooking(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🏨 Processing check-in for booking ID: " + id);
            
            BookingResponseDTO booking = bookingService.getBookingById(id);
            
            if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
                response.put("success", false);
                response.put("error", "Only confirmed bookings can be checked in");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update booking status to checked in
            bookingService.updateBookingStatus(id, BookingStatus.CHECKED_IN);
            
            response.put("success", true);
            response.put("message", "Guest checked in successfully");
            System.out.println("✅ Check-in completed for booking: " + booking.getBookingReference());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Check-in failed: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Check-in failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/booking/{id}/checkout")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkOutBooking(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🏨 Processing check-out for booking ID: " + id);
            
            BookingResponseDTO booking = bookingService.getBookingById(id);
            
            if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
                response.put("success", false);
                response.put("error", "Only checked-in guests can be checked out");
                return ResponseEntity.badRequest().body(response);
            }
            
            bookingService.updateBookingStatus(id, BookingStatus.CHECKED_OUT);
            
            response.put("success", true);
            response.put("message", "Guest checked out successfully");
            System.out.println("✅ Check-out completed for booking: " + booking.getBookingReference());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Check-out failed: " + e.getMessage());
            response.put("success", false);
            response.put("error", "Check-out failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

}