package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.dto.BookingRequestDTO;
import com.hotelreservationsystem.hotelreservationsystem.dto.BookingResponseDTO;
import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.BookingRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PromotionService promotionService;

    private static final BigDecimal DEFAULT_ROOM_PRICE = new BigDecimal("100.00");

    public BookingResponseDTO createBooking(BookingRequestDTO request) {
        validateBookingRequest(request);

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User must be authenticated to create a booking");
        }

        String userEmail = authentication.getName();
        User user = userService.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Get or create customer profile
        Customer customer = customerService.getOrCreateCustomerProfile(user);

        // Get room
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
        
        // Set initial booking and payment status
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);

        booking = bookingRepository.save(booking);

        // Note: Email confirmation will be sent after successful payment

        return convertToResponseDTO(booking);
    }

    public BookingResponseDTO createBookingForUser(BookingRequestDTO request, String userEmail) {
        validateBookingRequest(request);

        User user = userService.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found with email: " + userEmail);
        }

        // Get or create customer profile
        Customer customer = customerService.getOrCreateCustomerProfile(user);

        // Get room
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
        
        // Set initial booking and payment status
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);

        booking = bookingRepository.save(booking);

        // Note: Email confirmation will be sent after successful payment

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

    /**
     * Get booking entity by ID for payment processing
     */
    public Booking getBookingEntityById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
    }

    public List<BookingResponseDTO> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomer_CustomerId(customerId)
                .stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<BookingResponseDTO> getBookingsByUser(String userEmail) {
        User user = userService.findByEmail(userEmail);
        if (user == null) {
            return List.of(); // Return empty list if user not found
        }

        Customer customer = customerService.findByUser(user);
        if (customer == null) {
            return List.of(); // Return empty list if customer profile not found
        }

        return getBookingsByCustomer(customer.getCustomerId());
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

    public BookingResponseDTO updateBookingDetails(Long id, Map<String, Object> updateData) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Cannot update a cancelled booking");
        }

        if (booking.getBookingStatus() == BookingStatus.CHECKED_OUT) {
            throw new RuntimeException("Cannot update a completed booking");
        }

        System.out.println("📝 Updating booking details for: " + booking.getBookingReference());

        // Update only allowed fields (not dates or room)
        if (updateData.containsKey("numberOfGuests")) {
            Integer numberOfGuests = (Integer) updateData.get("numberOfGuests");
            if (numberOfGuests != null && numberOfGuests > 0) {
                Room room = booking.getRoom();
                if (numberOfGuests > room.getRoomType().getMaxOccupancy()) {
                    throw new RuntimeException("Number of guests (" + numberOfGuests + ") exceeds room capacity (" + room.getRoomType().getMaxOccupancy() + ")");
                }
                booking.setNumberOfGuests(numberOfGuests);
                System.out.println("📝 Updated number of guests to: " + numberOfGuests);
            }
        }

        if (updateData.containsKey("specialRequests")) {
            String specialRequests = (String) updateData.get("specialRequests");
            booking.setSpecialRequests(specialRequests);
            System.out.println("📝 Updated special requests");
        }

        if (updateData.containsKey("customerNotes")) {
            String customerNotes = (String) updateData.get("customerNotes");
            booking.setCustomerNotes(customerNotes);
            System.out.println("📝 Updated customer notes");
        }

        booking.setUpdatedAt(LocalDateTime.now());
        Booking updatedBooking = bookingRepository.save(booking);
        
        System.out.println("✅ Booking updated successfully: " + booking.getBookingReference());
        return convertToResponseDTO(updatedBooking);
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
            emailService.sendBookingCancellation(booking, booking.getCustomer().getUser().getEmail());
        } catch (Exception e) {
            System.err.println("Failed to send cancellation email: " + e.getMessage());
        }
    }

    public void cancelBookingWithEmail(Long id, String reason) {
        System.out.println("❌ Starting booking cancellation and deletion process for ID: " + id);
        
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        if (booking.getBookingStatus() == BookingStatus.CHECKED_OUT) {
            throw new RuntimeException("Cannot cancel a completed booking");
        }

        // Send cancellation email before deleting the booking
        try {
            emailService.sendBookingCancellationWithDetails(booking, booking.getCustomer().getUser().getEmail(), reason);
            System.out.println("📧 Cancellation email sent successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to send cancellation email: " + e.getMessage());
            e.printStackTrace();
            // Continue with deletion even if email fails
        }

        // Delete the booking from database completely
        bookingRepository.deleteById(id);
        System.out.println("🗑️ Booking completely removed from database: " + booking.getBookingReference());
    }

    public boolean isRoomAvailable(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        Long conflictingBookings = bookingRepository.countConflictingBookings(roomId, checkIn, checkOut);
        return conflictingBookings == 0;
    }

    public BookingResponseDTO updateBookingStatus(Long id, BookingStatus status) {
        System.out.println("🔄 Updating booking status for ID: " + id + " to: " + status);
        
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + id));
        
        booking.setBookingStatus(status);
        booking.setUpdatedAt(LocalDateTime.now());
        
        if (status == BookingStatus.CHECKED_IN) {
            booking.setCheckedInAt(LocalDateTime.now());
        } else if (status == BookingStatus.CHECKED_OUT) {
            booking.setCheckedOutAt(LocalDateTime.now());
        }
        
        Booking updatedBooking = bookingRepository.save(booking);
        System.out.println("✅ Booking status updated: " + booking.getBookingReference());
        
        return convertToResponseDTO(updatedBooking);
    }

    private void validateBookingRequest(BookingRequestDTO request) {
        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Check-in date cannot be in the past");
        }

        if (request.getCheckOutDate().isBefore(request.getCheckInDate())) {
            throw new RuntimeException("Check-out date must be after check-in date");
        }

        long daysBetween = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        
        if (daysBetween <= 0) {
            throw new RuntimeException("Check-out date must be after check-in date");
        }

        // Allow stays from 1 day to 90 days (3 months)
        if (daysBetween > 90) {
            throw new RuntimeException("Maximum stay duration is 90 days");
        }
    }

    private void calculateBookingDetails(Booking booking) {
        long numberOfNights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        booking.setNumberOfNights((int) numberOfNights);

        // Use the room's actual price
        BigDecimal roomPrice = booking.getRoom() != null ? booking.getRoom().getPricePerNight() : DEFAULT_ROOM_PRICE;
        booking.setRoomPricePerNight(roomPrice);

        BigDecimal subtotal = roomPrice.multiply(new BigDecimal(numberOfNights));
        BigDecimal serviceCharge = subtotal.multiply(new BigDecimal("0.10")); // 10% service charge
        BigDecimal taxes = subtotal.multiply(new BigDecimal("0.02")); // 2% taxes
        BigDecimal totalAmount = subtotal.add(serviceCharge).add(taxes);

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

        // Add promo code fields
        dto.setPromoCode(booking.getPromoCode());
        dto.setDiscountAmount(booking.getDiscountAmount());
        dto.setOriginalAmount(booking.getOriginalAmount());

        return dto;
    }

    /**
     * Create booking with promo code support
     */
    public BookingResponseDTO createBookingWithPromoCode(BookingRequestDTO request, String promoCode) {
        validateBookingRequest(request);

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User must be authenticated to create a booking");
        }

        String userEmail = authentication.getName();
        User user = userService.findByEmail(userEmail);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Get or create customer profile
        Customer customer = customerService.getOrCreateCustomerProfile(user);

        // Get room
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

        // Calculate booking details first
        calculateBookingDetails(booking);

        // Apply promo code if provided
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            try {
                applyPromoCodeToBooking(booking, promoCode, customer);
            } catch (Exception e) {
                throw new RuntimeException("Error applying promo code: " + e.getMessage());
            }
        }

        // Set initial booking and payment status
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);

        booking = bookingRepository.save(booking);

        return convertToResponseDTO(booking);
    }

    /**
     * Apply promo code to existing booking
     */
    public BookingResponseDTO applyPromoCodeToBooking(Long bookingId, String promoCode) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Can only apply promo codes to pending bookings");
        }

        if (booking.getPromoCode() != null) {
            throw new RuntimeException("A promo code has already been applied to this booking");
        }

        applyPromoCodeToBooking(booking, promoCode, booking.getCustomer());
        booking = bookingRepository.save(booking);

        return convertToResponseDTO(booking);
    }

    /**
     * Remove promo code from booking
     */
    public BookingResponseDTO removePromoCodeFromBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Can only remove promo codes from pending bookings");
        }

        if (booking.getPromoCode() == null) {
            throw new RuntimeException("No promo code applied to this booking");
        }

        // Reset to original amount
        if (booking.getOriginalAmount() != null) {
            booking.setTotalAmount(booking.getOriginalAmount());
        }
        booking.setPromoCode(null);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setOriginalAmount(null);

        booking = bookingRepository.save(booking);
        return convertToResponseDTO(booking);
    }

    /**
     * Validate promo code for a booking
     */
    public Map<String, Object> validatePromoCode(String promoCode, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        Customer customer = booking.getCustomer();
        if (customer == null) {
            throw new RuntimeException("Customer not found for this booking");
        }

        var validationResult = promotionService.validatePromoCode(promoCode, booking.getTotalAmount(), customer.getCustomerId());

        Map<String, Object> response = new HashMap<>();
        response.put("valid", validationResult.isValid());

        if (validationResult.isValid()) {
            response.put("discountAmount", validationResult.getDiscountAmount());
            response.put("newTotal", booking.getTotalAmount().subtract(validationResult.getDiscountAmount()));
            response.put("promotion", validationResult.getPromotion());
        } else {
            response.put("errorMessage", validationResult.getErrorMessage());
        }

        return response;
    }

    /**
     * Internal method to apply promo code to booking
     */
    private void applyPromoCodeToBooking(Booking booking, String promoCode, Customer customer) {
        // Validate the promo code
        var validationResult = promotionService.validatePromoCode(promoCode, booking.getTotalAmount(), customer.getCustomerId());

        if (!validationResult.isValid()) {
            throw new RuntimeException(validationResult.getErrorMessage());
        }

        // Apply the promotion to the booking
        promotionService.applyPromoCode(promoCode, booking, customer);
    }
}