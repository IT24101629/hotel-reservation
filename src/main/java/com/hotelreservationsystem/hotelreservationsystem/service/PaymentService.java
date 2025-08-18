package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.BookingRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    @Value("${payhere.merchant.id:1221688}")
    private String payhereProduction;

    @Value("${payhere.merchant.secret:MzE5NzAyMjU2MTMyODI1MTU0NDIxNzM0Mjc5OTU4MzQ4MjgzMTA1}")
    private String payhereMerchantSecret;

    public String generatePayHereHash(String orderId, String amount, String currency) {
        try {
            // PayHere hash format: MD5(merchant_id + order_id + amount + currency + md5(merchant_secret))
            String merchantSecretHash = getMD5(payhereMerchantSecret);
            String hashString = payhereProduction + orderId + amount + currency + merchantSecretHash;
            return getMD5(hashString).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PayHere hash", e);
        }
    }

    private String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating MD5 hash", e);
        }
    }

    public void processPayHereNotification(Map<String, String> params) {
        try {
            String orderId = params.get("order_id");
            String paymentId = params.get("payment_id");
            String statusCode = params.get("status_code");
            String md5sig = params.get("md5sig");

            // Verify the hash
            String localMd5sig = generateNotificationHash(params);
            if (!localMd5sig.equals(md5sig)) {
                throw new RuntimeException("Invalid payment notification hash");
            }

            // Find the booking
            Booking booking = bookingRepository.findByBookingReference(orderId)
                    .orElseThrow(() -> new RuntimeException("Booking not found: " + orderId));

            // Create or update payment record
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setPaymentMethod(PaymentMethod.PAYHERE);
            payment.setPaymentReference(paymentId);
            payment.setAmount(booking.getTotalAmount());
            payment.setCurrency("LKR");
            payment.setPaymentDate(LocalDateTime.now());

            // Update payment status based on PayHere status code
            if ("2".equals(statusCode)) {
                // Success
                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setPaymentStatus(PaymentStatus.COMPLETED);
                booking.setBookingStatus(BookingStatus.CONFIRMED);
            } else if ("0".equals(statusCode)) {
                // Pending
                payment.setPaymentStatus(PaymentStatus.PENDING);
                booking.setPaymentStatus(PaymentStatus.PENDING);
            } else {
                // Failed
                payment.setPaymentStatus(PaymentStatus.FAILED);
                booking.setPaymentStatus(PaymentStatus.FAILED);
                booking.setBookingStatus(BookingStatus.CANCELLED);
            }

            paymentRepository.save(payment);
            bookingRepository.save(booking);

            // Send email confirmation for successful payments
            if (PaymentStatus.COMPLETED.equals(payment.getPaymentStatus())) {
                emailService.sendBookingConfirmation(booking, booking.getCustomer().getUser().getEmail());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing PayHere notification", e);
        }
    }

    private String generateNotificationHash(Map<String, String> params) {
        try {
            String merchantId = params.get("merchant_id");
            String orderId = params.get("order_id");
            String paymentId = params.get("payment_id");
            String amount = params.get("payhere_amount");
            String currency = params.get("payhere_currency");
            String statusCode = params.get("status_code");

            String merchantSecretHash = getMD5(payhereMerchantSecret);
            String hashString = merchantId + orderId + amount + currency + statusCode + merchantSecretHash;
            return getMD5(hashString).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error generating notification hash", e);
        }
    }

    public void processAlternativePayment(Long bookingId, String paymentMethod) {
        try {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Create payment record
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(booking.getTotalAmount());
            payment.setCurrency("LKR");
            payment.setPaymentDate(LocalDateTime.now());

            if ("BANK_TRANSFER".equals(paymentMethod)) {
                payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
                payment.setPaymentStatus(PaymentStatus.PENDING);
                booking.setPaymentStatus(PaymentStatus.PENDING);
                booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
            } else if ("CASH".equals(paymentMethod)) {
                payment.setPaymentMethod(PaymentMethod.CASH);
                payment.setPaymentStatus(PaymentStatus.PENDING);
                booking.setPaymentStatus(PaymentStatus.PENDING);
                booking.setBookingStatus(BookingStatus.CONFIRMED);
            }

            paymentRepository.save(payment);
            bookingRepository.save(booking);

            // Send confirmation email
            emailService.sendBookingConfirmation(booking, booking.getCustomer().getUser().getEmail());

        } catch (Exception e) {
            throw new RuntimeException("Error processing alternative payment", e);
        }
    }

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

    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setPaymentStatus(status);
        if (PaymentStatus.COMPLETED.equals(status)) {
            payment.setCompletedAt(LocalDateTime.now());
        }

        return paymentRepository.save(payment);
    }

    public Payment findByBookingId(Long bookingId) {
        return paymentRepository.findByBooking_BookingId(bookingId)
                .orElse(null);
    }
}