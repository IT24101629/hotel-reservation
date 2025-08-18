package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.dto.BookingRequestDTO;
import com.hotelreservationsystem.hotelreservationsystem.dto.BookingResponseDTO;
import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.BookingRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.CustomerRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private EmailService emailService;
    
    private static final BigDecimal DEFAULT_ROOM_PRICE = new BigDecimal("100.00");
    
    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        validateBookingRequest(request);
        
        // Get customer and room entities
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found"));
        
        if (!isRoomAvailable(request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate())) {
            throw new RuntimeException("Room is not available for the selected dates");
        }
        
        Booking booking = new Booking();
        booking.setBookingReference(generateBookingReference());
        booking.setCustomer(customer);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setCustomerNotes(request.getCustomerNotes());
        
        calculateBookingDetails(booking);
        
        booking = bookingRepository.save(booking);
        
        try {
            emailService.sendBookingConfirmation(booking, "customer@example.com");
        } catch (Exception e) {
            System.err.println("Failed to send confirmation email: " + e.getMessage());
        }
        
        return convertToResponseDTO(booking);
    }
    
    public List<BookingResponseDTO> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    public BookingResponseDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
        return convertToResponseDTO(booking);
    }
    
    public BookingResponseDTO getBookingByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new RuntimeException("Booking not found with reference: " + reference));
        return convertToResponseDTO(booking);
    }
    
    public List<BookingResponseDTO> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomer_CustomerId(customerId)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<BookingResponseDTO> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByBookingStatus(status)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<BookingResponseDTO> getTodaysCheckIns() {
        return bookingRepository.findTodaysCheckIns(LocalDate.now())
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<BookingResponseDTO> getTodaysCheckOuts() {
        return bookingRepository.findTodaysCheckOuts(LocalDate.now())
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }
    
    public BookingResponseDTO updateBooking(Long id, BookingRequestDTO request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
        
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Cannot update a cancelled booking");
        }
        
        validateBookingRequest(request);
        
        if (!request.getRoomId().equals(booking.getRoom().getRoomId()) || 
            !request.getCheckInDate().equals(booking.getCheckInDate()) ||
            !request.getCheckOutDate().equals(booking.getCheckOutDate())) {
            
            if (!isRoomAvailable(request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate())) {
                throw new RuntimeException("Room is not available for the updated dates");
            }
        }
        
        // Update room if changed
        if (!request.getRoomId().equals(booking.getRoom().getRoomId())) {
            Room newRoom = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found"));
            booking.setRoom(newRoom);
        }
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setCustomerNotes(request.getCustomerNotes());
        
        calculateBookingDetails(booking);
        
        booking = bookingRepository.save(booking);
        return convertToResponseDTO(booking);
    }
    
    public BookingResponseDTO updateBookingStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
        
        booking.setBookingStatus(status);
        booking = bookingRepository.save(booking);
        
        return convertToResponseDTO(booking);
    }
    
    public BookingResponseDTO checkInGuest(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
        
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed bookings can be checked in");
        }
        
        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Check-in date has not arrived yet");
        }
        
        booking.setBookingStatus(BookingStatus.CHECKED_IN);
        booking = bookingRepository.save(booking);
        
        return convertToResponseDTO(booking);
    }
    
    public BookingResponseDTO checkOutGuest(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
        
        if (booking.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Only checked-in guests can be checked out");
        }
        
        booking.setBookingStatus(BookingStatus.CHECKED_OUT);
        booking.setPaymentStatus(PaymentStatus.COMPLETED);
        booking = bookingRepository.save(booking);
        
        return convertToResponseDTO(booking);
    }
    
    public void cancelBooking(Long id, String reason) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
        
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }
        
        if (booking.getBookingStatus() == BookingStatus.CHECKED_OUT) {
            throw new RuntimeException("Cannot cancel a completed booking");
        }
        
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        
        bookingRepository.save(booking);
        
        try {
            emailService.sendBookingCancellation(booking, "customer@example.com");
        } catch (Exception e) {
            System.err.println("Failed to send cancellation email: " + e.getMessage());
        }
    }
    
    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Long conflictingBookings = bookingRepository.countConflictingBookings(roomId, checkIn, checkOut);
        return conflictingBookings == 0;
    }
    
    private void validateBookingRequest(BookingRequestDTO request) {
        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Check-in date cannot be in the past");
        }
        
        if (request.getCheckOutDate().isBefore(request.getCheckInDate())) {
            throw new RuntimeException("Check-out date must be after check-in date");
        }
        
        if (request.getCheckOutDate().equals(request.getCheckInDate())) {
            throw new RuntimeException("Check-out date must be at least one day after check-in date");
        }
        
        long daysBetween = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        if (daysBetween > 30) {
            throw new RuntimeException("Maximum stay duration is 30 days");
        }
    }
    
    private void calculateBookingDetails(Booking booking) {
        long numberOfNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        booking.setNumberOfNights((int) numberOfNights);
        
        // Use the room's actual price
        BigDecimal roomPrice = booking.getRoom() != null ? booking.getRoom().getPricePerNight() : DEFAULT_ROOM_PRICE;
        booking.setRoomPricePerNight(roomPrice);
        
        BigDecimal totalAmount = roomPrice.multiply(new BigDecimal(numberOfNights));
        booking.setTotalAmount(totalAmount);
    }
    
    private String generateBookingReference() {
        String prefix = "BK";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomSuffix = String.format("%04d", new Random().nextInt(10000));
        return prefix + timestamp.substring(timestamp.length() - 6) + randomSuffix;
    }
    
    private BookingResponseDTO convertToResponseDTO(Booking booking) {
        BookingResponseDTO dto = new BookingResponseDTO();
        dto.setBookingId(booking.getBookingId());
        dto.setBookingReference(booking.getBookingReference());
        
        // Customer information
        if (booking.getCustomer() != null) {
            dto.setCustomerId(booking.getCustomer().getCustomerId());
            dto.setCustomerName(booking.getCustomer().getFullName());
            dto.setCustomerEmail(booking.getCustomer().getUser().getEmail());
        } else {
            dto.setCustomerId(null);
            dto.setCustomerName("Unknown Customer");
            dto.setCustomerEmail("unknown@example.com");
        }
        
        // Room information
        if (booking.getRoom() != null) {
            dto.setRoomId(booking.getRoom().getRoomId());
            dto.setRoomNumber(booking.getRoom().getRoomNumber());
            dto.setRoomType(booking.getRoom().getRoomType().getTypeName());
        } else {
            dto.setRoomId(null);
            dto.setRoomNumber("Unknown Room");
            dto.setRoomType("Unknown Type");
        }
        dto.setCheckInDate(booking.getCheckInDate());
        dto.setCheckOutDate(booking.getCheckOutDate());
        dto.setNumberOfGuests(booking.getNumberOfGuests());
        dto.setNumberOfNights(booking.getNumberOfNights());
        dto.setRoomPricePerNight(booking.getRoomPricePerNight());
        dto.setTotalAmount(booking.getTotalAmount());
        dto.setBookingStatus(booking.getBookingStatus());
        dto.setPaymentStatus(booking.getPaymentStatus());
        dto.setSpecialRequests(booking.getSpecialRequests());
        dto.setCustomerNotes(booking.getCustomerNotes());
        dto.setAdminNotes(booking.getAdminNotes());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        return dto;
    }
}