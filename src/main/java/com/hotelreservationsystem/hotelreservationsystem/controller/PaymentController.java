package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.dto.BookingResponseDTO;
import com.hotelreservationsystem.hotelreservationsystem.service.BookingService;
import com.hotelreservationsystem.hotelreservationsystem.service.PaymentService;
import com.hotelreservationsystem.hotelreservationsystem.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    /**
     * Get payment details for a booking
     */
    @GetMapping("/booking/{bookingId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentDetails(@PathVariable Long bookingId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            BookingResponseDTO booking = bookingService.getBookingById(bookingId);
            List<Payment> payments = paymentService.getPaymentsByBooking(bookingId);
            
            response.put("success", true);
            response.put("bookingId", booking.getBookingId());
            response.put("bookingReference", booking.getBookingReference());
            response.put("amount", booking.getTotalAmount());
            response.put("currency", "LKR");
            response.put("customerName", booking.getCustomerName());
            response.put("customerEmail", booking.getCustomerEmail());
            response.put("paymentStatus", booking.getPaymentStatus());
            response.put("payments", payments);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get payment by reference
     */
    @GetMapping("/reference/{paymentReference}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getPaymentByReference(@PathVariable String paymentReference) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Payment payment = paymentService.getPaymentByReference(paymentReference);
            
            response.put("success", true);
            response.put("payment", payment);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}