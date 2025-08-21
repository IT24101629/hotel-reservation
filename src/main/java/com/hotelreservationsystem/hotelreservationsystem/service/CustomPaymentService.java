package com.hotelreservationsystem.hotelreservationsystem.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.BookingRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CustomPaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Process custom payment - simulates payment gateway
     */
    public Map<String, Object> processCustomPayment(String bookingReference, String cardNumber, 
                                                   String expiryDate, String cvv, String cardholderName, 
                                                   BigDecimal amount) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Validate card details (basic validation for demo)
            if (!isValidCard(cardNumber, expiryDate, cvv, cardholderName)) {
                result.put("success", false);
                result.put("error", "Invalid card details");
                return result;
            }
            
            // Find booking
            Booking booking = bookingRepository.findByBookingReference(bookingReference)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingReference));
            
            // Create payment record
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
            payment.setPaymentReference(UUID.randomUUID().toString());
            payment.setAmount(amount);
            payment.setCurrency("LKR");
            payment.setPaymentDate(LocalDateTime.now());
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            
            // Update booking status
            booking.setPaymentStatus(PaymentStatus.COMPLETED);
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            
            // Save payment and booking
            paymentRepository.save(payment);
            bookingRepository.save(booking);
            
            // Generate QR code for booking
            String qrCodeData = generateQRCodeData(booking);
            String qrCodeBase64 = generateQRCodeImage(qrCodeData);
            
            // Send confirmation email with QR code
            emailService.sendBookingConfirmationWithQR(booking, booking.getCustomer().getUser().getEmail(), qrCodeBase64);
            
            result.put("success", true);
            result.put("paymentReference", payment.getPaymentReference());
            result.put("bookingReference", bookingReference);
            result.put("amount", amount);
            result.put("qrCode", qrCodeBase64);
            
            System.out.println("=== CUSTOM PAYMENT SUCCESSFUL ===");
            System.out.println("Booking: " + bookingReference);
            System.out.println("Payment Reference: " + payment.getPaymentReference());
            System.out.println("Amount: " + amount);
            
        } catch (Exception e) {
            System.err.println("Custom payment processing error: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Basic card validation (for demo purposes)
     */
    private boolean isValidCard(String cardNumber, String expiryDate, String cvv, String cardholderName) {
        // Basic validation rules
        if (cardNumber == null || cardNumber.replaceAll("\\s", "").length() < 13) {
            return false;
        }
        
        if (expiryDate == null || !expiryDate.matches("\\d{2}/\\d{2}")) {
            return false;
        }
        
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            return false;
        }
        
        if (cardholderName == null || cardholderName.trim().length() < 2) {
            return false;
        }
        
        // Simulate payment gateway validation
        String cleanCardNumber = cardNumber.replaceAll("\\s", "");
        
        // Demo: Reject specific test cards to simulate failures
        if (cleanCardNumber.equals("4000000000000002") || 
            cleanCardNumber.equals("4000000000000119")) {
            return false; // Simulate declined cards
        }
        
        return true; // Accept all other cards for demo
    }
    
    /**
     * Generate QR code data for booking
     */
    private String generateQRCodeData(Booking booking) {
        StringBuilder qrData = new StringBuilder();
        qrData.append("HOTEL BOOKING CONFIRMATION\n");
        qrData.append("Booking ID: ").append(booking.getBookingReference()).append("\n");
        qrData.append("Customer: ").append(booking.getCustomer().getUser().getFirstName())
              .append(" ").append(booking.getCustomer().getUser().getLastName()).append("\n");
        qrData.append("Room: ").append(booking.getRoom().getRoomNumber()).append("\n");
        qrData.append("Check-in: ").append(booking.getCheckInDate()).append("\n");
        qrData.append("Check-out: ").append(booking.getCheckOutDate()).append("\n");
        qrData.append("Amount: LKR ").append(booking.getTotalAmount()).append("\n");
        qrData.append("Status: CONFIRMED");
        
        return qrData.toString();
    }
    
    /**
     * Generate QR code image as Base64 string
     */
    private String generateQRCodeImage(String qrCodeData) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 300, 300);
            
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "PNG", outputStream);
            
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
        } catch (WriterException | IOException e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get payment status for a booking
     */
    public Map<String, Object> getPaymentStatus(String bookingReference) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Booking booking = bookingRepository.findByBookingReference(bookingReference)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            result.put("success", true);
            result.put("bookingReference", bookingReference);
            result.put("paymentStatus", booking.getPaymentStatus().toString());
            result.put("bookingStatus", booking.getBookingStatus().toString());
            result.put("amount", booking.getTotalAmount());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate booking for payment
     */
    public Map<String, Object> validateBookingForPayment(String bookingReference) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Booking booking = bookingRepository.findByBookingReference(bookingReference)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            if (booking.getPaymentStatus() == PaymentStatus.COMPLETED) {
                result.put("success", false);
                result.put("error", "This booking has already been paid");
                return result;
            }
            
            result.put("success", true);
            result.put("bookingId", booking.getBookingId());
            result.put("bookingReference", bookingReference);
            result.put("customerName", booking.getCustomer().getUser().getFirstName() + " " + 
                                     booking.getCustomer().getUser().getLastName());
            result.put("customerEmail", booking.getCustomer().getUser().getEmail());
            result.put("roomNumber", booking.getRoom().getRoomNumber());
            result.put("roomType", booking.getRoom().getRoomType().getTypeName());
            result.put("checkInDate", booking.getCheckInDate().toString());
            result.put("checkOutDate", booking.getCheckOutDate().toString());
            result.put("numberOfNights", booking.getNumberOfNights());
            result.put("numberOfGuests", booking.getNumberOfGuests());
            result.put("amount", booking.getTotalAmount());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}