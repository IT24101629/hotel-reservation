package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.BookingRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Create a new payment record
     */
    public Payment createPayment(Booking booking, PaymentMethod paymentMethod, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(amount);
        payment.setCurrency("LKR");
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());
        
        return paymentRepository.save(payment);
    }

    /**
     * Update payment status
     */
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        payment.setPaymentStatus(status);
        if (PaymentStatus.COMPLETED.equals(status)) {
            payment.setCompletedAt(LocalDateTime.now());
        }
        
        return paymentRepository.save(payment);
    }

    /**
     * Get all payments for a booking
     */
    public List<Payment> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByBooking_BookingId(bookingId);
    }

    /**
     * Get payment by reference
     */
    public Payment getPaymentByReference(String paymentReference) {
        return paymentRepository.findByTransactionId(paymentReference)
                .orElseThrow(() -> new RuntimeException("Payment not found with reference: " + paymentReference));
    }
}