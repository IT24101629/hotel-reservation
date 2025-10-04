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
            System.err.println("=== BOOKING CREATION REQUEST ===");
            System.err.println("BookingController: Received booking request: " + bookingRequest);
            
            // Get the authenticated user
            String username = authentication.getName();
            System.err.println("BookingController: Authenticated user: " + username);
            
            User user = userService.findByEmail(username);
            
            if (user == null) {
                System.err.println("BookingController: User not found for email: " + username);
                response.put("success", false);
                response.put("error", "User not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            System.err.println("BookingController: User found: " + user.getFirstName() + " " + user.getLastName());
            
            // Get or create customer for this user
            Customer customer = customerService.getOrCreateCustomerProfile(user);
            System.err.println("BookingController: Customer profile: " + customer.getCustomerId());
            
            // Set the customer ID from authenticated user
            bookingRequest.setCustomerId(customer.getCustomerId());
            
            System.err.println("BookingController: Creating booking with request: " + bookingRequest);
            BookingResponseDTO booking = bookingService.createBooking(bookingRequest);
            
            System.err.println("BookingController: Booking created successfully: " + booking.getBookingReference());
            
            response.put("success", true);
            response.put("message", "Booking created successfully");
            response.put("bookingId", booking.getBookingId());
            response.put("bookingReference", booking.getBookingReference());
            response.put("booking", booking);
            
            System.err.println("BookingController: Returning success response");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("BookingController: Error creating booking: " + e.getMessage());
            e.printStackTrace();
            
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

    @GetMapping("/{id}/test")
    public ResponseEntity<Map<String, Object>> testBookingEndpoint(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test endpoint working");
        response.put("bookingId", id);
        response.put("timestamp", java.time.LocalDateTime.now());
        System.out.println("🧪 Test endpoint called for booking ID: " + id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateBookingPost(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> updateData,
                                                              Authentication authentication) {
        return updateBookingInternal(id, updateData, authentication);
    }

    @PutMapping("/{id}/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> updateBooking(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> updateData,
                                                              Authentication authentication) {
        return updateBookingInternal(id, updateData, authentication);
    }

    private ResponseEntity<Map<String, Object>> updateBookingInternal(@PathVariable Long id,
                                                              @RequestBody Map<String, Object> updateData,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("🔄 ===== UPDATE BOOKING REQUEST =====");
            System.out.println("📝 Booking ID: " + id);
            System.out.println("📝 Update data received: " + updateData);
            System.out.println("📝 User: " + authentication.getName());
            
            // Verify user owns this booking
            String userEmail = authentication.getName();
            System.out.println("🔍 Fetching existing booking...");
            BookingResponseDTO existingBooking = bookingService.getBookingById(id);
            System.out.println("✅ Existing booking found: " + existingBooking.getBookingReference());
            
            if (!existingBooking.getCustomerEmail().equals(userEmail)) {
                System.out.println("❌ Access denied: booking belongs to " + existingBooking.getCustomerEmail() + ", user is " + userEmail);
                response.put("success", false);
                response.put("error", "You can only update your own bookings");
                return ResponseEntity.badRequest().body(response);
            }
            
            System.out.println("✅ Access granted, updating booking...");
            // Call service to update booking
            BookingResponseDTO updatedBooking = bookingService.updateBookingDetails(id, updateData);
            System.out.println("✅ Booking updated successfully");
            
            response.put("success", true);
            response.put("message", "Booking updated successfully");
            response.put("booking", updatedBooking);
            
            System.out.println("📤 Sending success response");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error updating booking: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Long id,
                                                              @RequestBody(required = false) Map<String, String> cancelData,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("❌ Cancel booking request for ID: " + id);
            
            // Verify user owns this booking
            String userEmail = authentication.getName();
            BookingResponseDTO existingBooking = bookingService.getBookingById(id);
            
            if (!existingBooking.getCustomerEmail().equals(userEmail)) {
                response.put("success", false);
                response.put("error", "You can only cancel your own bookings");
                return ResponseEntity.badRequest().body(response);
            }
            
            String reason = "Cancelled by customer";
            if (cancelData != null && cancelData.containsKey("reason")) {
                reason = cancelData.get("reason");
            }
            
            bookingService.cancelBookingWithEmail(id, reason);
            response.put("success", true);
            response.put("message", "Booking cancelled successfully and confirmation email sent");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error cancelling booking: " + e.getMessage());
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

    @PostMapping("/create-with-promo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> createBookingWithPromoCode(@Valid @RequestBody BookingRequestDTO bookingRequest,
                                                                           @RequestParam(required = false) String promoCode,
                                                                           Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.err.println("=== BOOKING CREATION WITH PROMO CODE REQUEST ===");
            System.err.println("BookingController: Received booking request with promo code: " + promoCode);

            BookingResponseDTO booking;
            if (promoCode != null && !promoCode.trim().isEmpty()) {
                booking = bookingService.createBookingWithPromoCode(bookingRequest, promoCode.trim());
            } else {
                booking = bookingService.createBooking(bookingRequest);
            }

            response.put("success", true);
            response.put("message", "Booking created successfully" + (promoCode != null ? " with promo code applied" : ""));
            response.put("booking", booking);

            System.err.println("BookingController: Booking created with reference: " + booking.getBookingReference());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("BookingController: Error creating booking with promo code: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/apply-promo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> applyPromoCode(@PathVariable Long id,
                                                              @RequestParam String promoCode,
                                                              Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("🏷️ Apply promo code request for booking ID: " + id + ", code: " + promoCode);

            // Verify user owns this booking
            String userEmail = authentication.getName();
            BookingResponseDTO existingBooking = bookingService.getBookingById(id);

            if (!existingBooking.getCustomerEmail().equals(userEmail)) {
                response.put("success", false);
                response.put("error", "You can only apply promo codes to your own bookings");
                return ResponseEntity.badRequest().body(response);
            }

            BookingResponseDTO updatedBooking = bookingService.applyPromoCodeToBooking(id, promoCode);

            response.put("success", true);
            response.put("message", "Promo code applied successfully");
            response.put("booking", updatedBooking);
            response.put("discountAmount", updatedBooking.getDiscountAmount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error applying promo code: " + e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/remove-promo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> removePromoCode(@PathVariable Long id,
                                                               Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("🗑️ Remove promo code request for booking ID: " + id);

            // Verify user owns this booking
            String userEmail = authentication.getName();
            BookingResponseDTO existingBooking = bookingService.getBookingById(id);

            if (!existingBooking.getCustomerEmail().equals(userEmail)) {
                response.put("success", false);
                response.put("error", "You can only modify your own bookings");
                return ResponseEntity.badRequest().body(response);
            }

            BookingResponseDTO updatedBooking = bookingService.removePromoCodeFromBooking(id);

            response.put("success", true);
            response.put("message", "Promo code removed successfully");
            response.put("booking", updatedBooking);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error removing promo code: " + e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/validate-promo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> validatePromoCode(@RequestParam String promoCode,
                                                                 @RequestParam Long bookingId,
                                                                 Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            System.out.println("🔍 Validate promo code request: " + promoCode + " for booking: " + bookingId);

            // Verify user owns this booking
            String userEmail = authentication.getName();
            BookingResponseDTO existingBooking = bookingService.getBookingById(bookingId);

            if (!existingBooking.getCustomerEmail().equals(userEmail)) {
                response.put("success", false);
                response.put("error", "You can only validate promo codes for your own bookings");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> validationResult = bookingService.validatePromoCode(promoCode, bookingId);
            return ResponseEntity.ok(validationResult);

        } catch (Exception e) {
            System.err.println("❌ Error validating promo code: " + e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}