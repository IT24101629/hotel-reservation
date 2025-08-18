package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.dto.BookingRequestDTO;
import com.hotelreservationsystem.hotelreservationsystem.dto.BookingResponseDTO;
import com.hotelreservationsystem.hotelreservationsystem.model.BookingStatus;
import com.hotelreservationsystem.hotelreservationsystem.service.BookingService;
import com.hotelreservationsystem.hotelreservationsystem.service.CustomerService;
import com.hotelreservationsystem.hotelreservationsystem.service.UserService;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.model.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CustomerService customerService;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createBooking(@Valid @RequestBody BookingRequestDTO bookingRequest, 
                                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Get the authenticated user
            String username = authentication.getName();
            User user = userService.findByEmail(username);
            
            if (user == null) {
                response.put("success", false);
                response.put("error", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Get or create customer for this user
            Customer customer = customerService.getOrCreateCustomerProfile(user);
            
            // Set the customer ID from authenticated user
            bookingRequest.setCustomerId(customer.getCustomerId());
            
            BookingResponseDTO booking = bookingService.createBooking(bookingRequest);
            response.put("success", true);
            response.put("message", "Booking created successfully");
            response.put("bookingId", booking.getBookingId());
            response.put("bookingReference", booking.getBookingReference());
            response.put("booking", booking);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDTO> getBooking(@PathVariable Long id) {
        try {
            BookingResponseDTO booking = bookingService.getBookingById(id);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/reference/{reference}")
    public ResponseEntity<BookingResponseDTO> getBookingByReference(@PathVariable String reference) {
        try {
            BookingResponseDTO booking = bookingService.getBookingByReference(reference);
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponseDTO>> getAllBookings() {
        List<BookingResponseDTO> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<BookingResponseDTO>> getBookingsByCustomer(@PathVariable Long customerId) {
        List<BookingResponseDTO> bookings = bookingService.getBookingsByCustomer(customerId);
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long id,
                                                              @RequestParam(required = false) String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            bookingService.cancelBooking(id, reason != null ? reason : "Cancelled by customer");
            response.put("success", true);
            response.put("message", "Booking cancelled successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/availability/check")
    public ResponseEntity<Map<String, Object>> checkRoomAvailability(@RequestParam Long roomId,
                                                                      @RequestParam String checkIn,
                                                                      @RequestParam String checkOut) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean isAvailable = bookingService.isRoomAvailable(roomId, 
                java.time.LocalDate.parse(checkIn), 
                java.time.LocalDate.parse(checkOut));
            
            response.put("available", isAvailable);
            response.put("roomId", roomId);
            response.put("checkIn", checkIn);
            response.put("checkOut", checkOut);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}