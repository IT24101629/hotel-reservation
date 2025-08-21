package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.service.CustomPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/custom-payment")
public class CustomPaymentController {

    @Autowired
    private CustomPaymentService customPaymentService;

    /**
     * Process custom payment
     */
    @PostMapping("/process")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> processPayment(@Valid @RequestBody PaymentRequest request) {
        System.out.println("=== CUSTOM PAYMENT PROCESSING ===");
        System.out.println("Booking Reference: " + request.getBookingReference());
        System.out.println("Amount: " + request.getAmount());
        
        try {
            Map<String, Object> result = customPaymentService.processCustomPayment(
                request.getBookingReference(),
                request.getCardNumber(),
                request.getExpiryDate(),
                request.getCvv(),
                request.getCardholderName(),
                request.getAmount()
            );
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            System.err.println("Payment processing error: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Payment processing failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Validate booking for payment
     */
    @GetMapping("/validate/{bookingReference}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateBooking(@PathVariable String bookingReference) {
        System.out.println("Validating booking for payment: " + bookingReference);
        
        try {
            Map<String, Object> result = customPaymentService.validateBookingForPayment(bookingReference);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Validation failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get payment status
     */
    @GetMapping("/status/{bookingReference}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String bookingReference) {
        try {
            Map<String, Object> result = customPaymentService.getPaymentStatus(bookingReference);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Status check failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Payment request DTO with validation
     */
    public static class PaymentRequest {
        
        @NotBlank(message = "Booking reference is required")
        private String bookingReference;
        
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9\\s]{13,19}$", message = "Invalid card number format")
        private String cardNumber;
        
        @NotBlank(message = "Expiry date is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])/\\d{2}$", message = "Expiry date must be in MM/YY format")
        private String expiryDate;
        
        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
        private String cvv;
        
        @NotBlank(message = "Cardholder name is required")
        @Size(min = 2, max = 50, message = "Cardholder name must be between 2 and 50 characters")
        private String cardholderName;
        
        @NotNull(message = "Amount is required")
        private BigDecimal amount;
        
        // Getters and Setters
        public String getBookingReference() {
            return bookingReference;
        }
        
        public void setBookingReference(String bookingReference) {
            this.bookingReference = bookingReference;
        }
        
        public String getCardNumber() {
            return cardNumber;
        }
        
        public void setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
        }
        
        public String getExpiryDate() {
            return expiryDate;
        }
        
        public void setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
        }
        
        public String getCvv() {
            return cvv;
        }
        
        public void setCvv(String cvv) {
            this.cvv = cvv;
        }
        
        public String getCardholderName() {
            return cardholderName;
        }
        
        public void setCardholderName(String cardholderName) {
            this.cardholderName = cardholderName;
        }
        
        public BigDecimal getAmount() {
            return amount;
        }
        
        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}