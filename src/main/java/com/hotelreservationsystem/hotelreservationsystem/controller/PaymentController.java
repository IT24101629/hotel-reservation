package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/payhere/generate-hash")
    public ResponseEntity<Map<String, Object>> generatePayHereHash(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            System.out.println("PaymentController: Received hash generation request: " + request);
            
            String orderId = (String) request.get("orderId");
            String amount = (String) request.get("amount");
            String currency = (String) request.get("currency");
            
            System.out.println("PaymentController: Hash generation params - orderId: " + orderId + ", amount: " + amount + ", currency: " + currency);
            
            if (orderId == null || orderId.trim().isEmpty()) {
                throw new RuntimeException("Order ID is required");
            }
            
            if (amount == null || amount.trim().isEmpty()) {
                throw new RuntimeException("Amount is required");
            }
            
            if (currency == null || currency.trim().isEmpty()) {
                currency = "LKR"; // Default currency
            }
            
            String hash = paymentService.generatePayHereHash(orderId, amount, currency);
            System.out.println("PaymentController: Generated hash successfully for order: " + orderId);
            
            response.put("success", true);
            response.put("hash", hash);
            response.put("orderId", orderId);
            response.put("amount", amount);
            response.put("currency", currency);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("PaymentController: Error generating hash: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/payhere/notify")
    public ResponseEntity<String> handlePayHereNotification(@RequestParam Map<String, String> params) {
        try {
            paymentService.processPayHereNotification(params);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("ERROR");
        }
    }

    @PostMapping("/alternative")
    public ResponseEntity<Map<String, Object>> processAlternativePayment(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long bookingId = Long.valueOf(request.get("bookingId").toString());
            String paymentMethod = (String) request.get("paymentMethod");
            
            paymentService.processAlternativePayment(bookingId, paymentMethod);
            
            response.put("success", true);
            response.put("message", "Payment method confirmed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}