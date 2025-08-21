package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.Payment;
import com.hotelreservationsystem.hotelreservationsystem.model.PaymentMethod;
import com.hotelreservationsystem.hotelreservationsystem.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Find payments by booking ID (multiple payments possible)
    List<Payment> findByBooking_BookingId(Long bookingId);
    
    // Find payment by transaction ID (payment reference)
    Optional<Payment> findByTransactionId(String transactionId);
    
    // Find payments by payment method
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);
    
    // Find payments by status
    List<Payment> findByPaymentStatus(PaymentStatus status);
    
    // Find payments by date range
    @Query("SELECT p FROM Payment p WHERE p.paymentDate >= :startDate AND p.paymentDate <= :endDate")
    List<Payment> findPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);
    
    // Find completed payments
    List<Payment> findByPaymentStatusAndCompletedAtIsNotNull(PaymentStatus status);
    
    // Count payments by status
    long countByPaymentStatus(PaymentStatus status);
    
    // Count payments by method
    long countByPaymentMethod(PaymentMethod paymentMethod);
    
    // Find recent payments
    @Query("SELECT p FROM Payment p WHERE p.paymentDate >= :date ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPayments(@Param("date") LocalDateTime date);
    
    // Sum total payments by status
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentStatus = :status")
    Double sumAmountByPaymentStatus(@Param("status") PaymentStatus status);
}