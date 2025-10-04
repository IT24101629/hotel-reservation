package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.Booking;
import com.hotelreservationsystem.hotelreservationsystem.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    // Find bookings by customer ID
    List<Booking> findByCustomer_CustomerId(Long customerId);
    
    // Find bookings by room ID
    List<Booking> findByRoom_RoomId(Long roomId);
    
    // Find bookings by status
    List<Booking> findByBookingStatus(BookingStatus status);
    
    // Find booking by reference number
    Optional<Booking> findByBookingReference(String bookingReference);
    
    // Find bookings by date range
    @Query("SELECT b FROM Booking b WHERE b.checkInDate >= :startDate AND b.checkOutDate <= :endDate")
    List<Booking> findBookingsByDateRange(@Param("startDate") LocalDate startDate, 
                                         @Param("endDate") LocalDate endDate);
    
    // Check room availability - count conflicting bookings
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.room.roomId = :roomId AND " +
           "((b.checkInDate < :checkOut AND b.checkOutDate > :checkIn)) AND " +
           "b.bookingStatus IN ('CONFIRMED', 'CHECKED_IN')")
    Long countConflictingBookings(@Param("roomId") Long roomId,
                                 @Param("checkIn") LocalDate checkIn,
                                 @Param("checkOut") LocalDate checkOut);
    
    // Find bookings for today's check-ins
    @Query("SELECT b FROM Booking b WHERE b.checkInDate = :today AND b.bookingStatus = 'CONFIRMED'")
    List<Booking> findTodaysCheckIns(@Param("today") LocalDate today);
    
    // Find bookings for today's check-outs
    @Query("SELECT b FROM Booking b WHERE b.checkOutDate = :today AND b.bookingStatus = 'CHECKED_IN'")
    List<Booking> findTodaysCheckOuts(@Param("today") LocalDate today);
    
    // Find overdue check-outs
    @Query("SELECT b FROM Booking b WHERE b.checkOutDate < :today AND b.bookingStatus = 'CHECKED_IN'")
    List<Booking> findOverdueCheckOuts(@Param("today") LocalDate today);
    
    // Find bookings by customer and status
    List<Booking> findByCustomer_CustomerIdAndBookingStatus(Long customerId, BookingStatus status);
    
    // Find recent bookings (last 30 days)
    @Query("SELECT b FROM Booking b WHERE b.createdAt >= :thirtyDaysAgo ORDER BY b.createdAt DESC")
    List<Booking> findRecentBookings(@Param("thirtyDaysAgo") LocalDate thirtyDaysAgo);
    
    // Count total bookings by status
    Long countByBookingStatus(BookingStatus status);
    
    // Find bookings by check-in date
    List<Booking> findByCheckInDate(LocalDate checkInDate);
    
    // Find bookings by check-out date
    List<Booking> findByCheckOutDate(LocalDate checkOutDate);

    // Find top 5 recent bookings for dashboard (ordered by createdAt)
    List<Booking> findTop5ByOrderByCreatedAtDesc();
}