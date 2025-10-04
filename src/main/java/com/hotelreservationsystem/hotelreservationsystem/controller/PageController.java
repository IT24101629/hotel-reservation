package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.Room;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.model.UserRole;
import com.hotelreservationsystem.hotelreservationsystem.service.BookingService;
import com.hotelreservationsystem.hotelreservationsystem.service.CustomerService;
import com.hotelreservationsystem.hotelreservationsystem.service.ReviewService;
import com.hotelreservationsystem.hotelreservationsystem.service.RoomService;
import com.hotelreservationsystem.hotelreservationsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PageController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private ReviewService reviewService;

    // Home page
    @GetMapping("/")
    public String home(Model model) {
        try {
            // Add any data needed for the home page
            model.addAttribute("totalRooms", roomService.getTotalRoomsCount());
            model.addAttribute("availableRooms", roomService.getAvailableRoomsCount());
        } catch (Exception e) {
            System.out.println("Home: Error loading room statistics - " + e.getMessage());
            model.addAttribute("totalRooms", 0);
            model.addAttribute("availableRooms", 0);
        }
        return "index";
    }

    // Home aliases
    @GetMapping("/index")
    public String index(Model model) {
        return home(model);
    }

    @GetMapping("/home")
    public String homeAlias(Model model) {
        return home(model);
    }

    // Dashboard - authenticated users only
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        System.out.println("Dashboard: Processing request");

        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            System.out.println("Dashboard: Authenticated user email - " + email);

            User user = userService.findByEmail(email);
            if (user != null) {
                model.addAttribute("user", user);
                System.out.println("Dashboard: User found - " + user.getFirstName() + " " + user.getLastName() + " with role " + user.getRole());

                // Add user-specific data based on role
                if (UserRole.CUSTOMER.equals(user.getRole())) {
                    // Customer dashboard data
                    try {
                        var recentBookings = bookingService.getBookingsByUser(email);
                        model.addAttribute("recentBookings", recentBookings);
                        System.out.println("Dashboard: Found " + recentBookings.size() + " bookings for customer");
                    } catch (Exception e) {
                        // Handle gracefully if no customer profile exists yet
                        System.out.println("Dashboard: Error loading customer bookings - " + e.getMessage());
                        e.printStackTrace();
                        model.addAttribute("recentBookings", java.util.Collections.emptyList());
                    }
                } else if (UserRole.ADMIN.equals(user.getRole())) {
                    // Admin dashboard data
                    try {
                        var allBookings = bookingService.getAllBookings();
                        var todaysCheckIns = bookingService.getTodaysCheckIns();
                        var todaysCheckOuts = bookingService.getTodaysCheckOuts();

                        model.addAttribute("allBookings", allBookings);
                        model.addAttribute("todaysCheckIns", todaysCheckIns);
                        model.addAttribute("todaysCheckOuts", todaysCheckOuts);
                        model.addAttribute("totalRooms", roomService.getTotalRoomsCount());

                        System.out.println("Dashboard: Admin data loaded - " + allBookings.size() + " total bookings");
                    } catch (Exception e) {
                        System.out.println("Dashboard: Error loading admin data - " + e.getMessage());
                        e.printStackTrace();
                        model.addAttribute("allBookings", java.util.Collections.emptyList());
                        model.addAttribute("todaysCheckIns", java.util.Collections.emptyList());
                        model.addAttribute("todaysCheckOuts", java.util.Collections.emptyList());
                    }
                }
            } else {
                System.out.println("Dashboard: User not found for email " + email);
                model.addAttribute("errorMessage", "User profile not found");
            }
        } else {
            System.out.println("Dashboard: User not authenticated, redirecting to login");
            return "redirect:/auth/login";
        }

        System.out.println("Dashboard: Rendering dashboard template");
        return "dashboard";
    }



    // Rooms page
    @GetMapping("/rooms")
    public String rooms(Model model,
                        @RequestParam(value = "checkIn", required = false) String checkIn,
                        @RequestParam(value = "checkOut", required = false) String checkOut,
                        @RequestParam(value = "guests", required = false) Integer guests,
                        @RequestParam(value = "roomType", required = false) String roomType) {

        System.out.println("Rooms: Processing request with params - checkIn: " + checkIn +
                ", checkOut: " + checkOut + ", guests: " + guests + ", roomType: " + roomType);

        // Add search parameters to model
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests);
        model.addAttribute("roomType", roomType);

        // Get available rooms based on search criteria
        try {
            if (checkIn != null && checkOut != null) {
                var availableRooms = roomService.findAvailableRoomsForDates(checkIn, checkOut, guests, roomType);
                model.addAttribute("availableRooms", availableRooms);
                System.out.println("Rooms: Found " + availableRooms.size() + " available rooms for dates");
            } else {
                var availableRooms = roomService.getAllAvailableRooms();
                model.addAttribute("availableRooms", availableRooms);
                System.out.println("Rooms: Found " + availableRooms.size() + " total available rooms");
            }

            // Get room types for filter
            var roomTypes = roomService.getAllRoomTypes();
            model.addAttribute("roomTypes", roomTypes);
            System.out.println("Rooms: Found " + roomTypes.size() + " room types");

        } catch (Exception e) {
            System.out.println("Rooms: Error loading room data - " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("availableRooms", java.util.Collections.emptyList());
            model.addAttribute("roomTypes", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", "Error loading room data");
        }

        return "rooms";
    }

    // Room detail page
    @GetMapping("/rooms/{roomId}")
    public String roomDetail(@PathVariable Long roomId, Model model, Authentication authentication) {
        System.out.println("Room Detail: Processing request for room ID: " + roomId);

        try {
            var roomOpt = roomService.getRoomById(roomId);
            if (roomOpt.isEmpty()) {
                System.out.println("Room Detail: Room not found with ID: " + roomId);
                return "redirect:/rooms?error=room_not_found";
            }

            Room room = roomOpt.get();
            model.addAttribute("room", room);
            System.out.println("Room Detail: Room found - Room #" + room.getRoomNumber());

            // Get reviews for this room
            var reviews = reviewService.getApprovedReviewsByRoom(roomId);
            model.addAttribute("reviews", reviews);
            System.out.println("Room Detail: Found " + reviews.size() + " reviews");

            // Get room statistics
            var roomStats = reviewService.getRoomReviewStatistics(roomId);
            model.addAttribute("roomStats", roomStats);
            System.out.println("Room Detail: Average rating - " + roomStats.get("averageRating"));

            // Check if logged-in user can review this room
            if (authentication != null && authentication.isAuthenticated()) {
                try {
                    String email = authentication.getName();
                    User user = userService.findByEmail(email);
                    if (user != null && UserRole.CUSTOMER.equals(user.getRole())) {
                        var customer = customerService.getOrCreateCustomerProfile(user);
                        boolean canReview = reviewService.canCustomerReviewRoom(customer.getCustomerId(), roomId);
                        model.addAttribute("canReview", canReview);
                        model.addAttribute("customerId", customer.getCustomerId());
                        System.out.println("Room Detail: User can review - " + canReview);
                    }
                } catch (Exception e) {
                    System.out.println("Room Detail: Error checking review eligibility - " + e.getMessage());
                }
            }

            return "room-detail";

        } catch (Exception e) {
            System.out.println("Room Detail: Error loading room - " + e.getMessage());
            e.printStackTrace();
            return "redirect:/rooms?error=room_not_found";
        }
    }

    // Booking page
    @GetMapping("/booking")
    public String booking(Model model, Authentication authentication) {
        System.out.println("Booking: Processing request");

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("Booking: User not authenticated, redirecting to login");
            return "redirect:/auth/login?returnUrl=/booking";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        if (user != null) {
            model.addAttribute("user", user);
            System.out.println("Booking: User found - " + user.getFirstName());
        } else {
            System.out.println("Booking: User not found for email " + email);
            model.addAttribute("errorMessage", "User not found");
        }

        return "booking";
    }

    // Payment page
    @GetMapping("/payment")
    public String payment(Model model,
                          @RequestParam(value = "bookingId", required = false) Long bookingId,
                          Authentication authentication,
                          jakarta.servlet.http.HttpServletRequest request) {

        System.err.println("=== PAYMENT PAGE REQUEST ===");
        System.err.println("Payment: Processing request for bookingId: " + bookingId);
        System.err.println("Payment: Request URL: " + request.getRequestURL());
        System.err.println("Payment: Query String: " + request.getQueryString());
        System.err.println("Payment: Referer: " + request.getHeader("Referer"));
        System.err.println("Payment: User-Agent: " + request.getHeader("User-Agent"));
        System.err.println("Payment: Method: " + request.getMethod());

        try {
            // Check if user is logged in
            if (authentication == null || !authentication.isAuthenticated()) {
                System.err.println("Payment: User not authenticated, redirecting to login");
                return "redirect:/auth/login?returnUrl=/payment";
            }

            String userEmail = authentication.getName();
            System.err.println("Payment: Authenticated user: " + userEmail);

            if (bookingId != null) {
                model.addAttribute("bookingId", bookingId);
                System.err.println("Payment: Added bookingId to model: " + bookingId);
            } else {
                System.err.println("Payment: No bookingId provided in request");
            }

            // Add base URL for payment endpoints
            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            model.addAttribute("baseUrl", baseUrl);
            System.err.println("Payment: Added baseUrl to model: " + baseUrl);

            System.err.println("Payment: Returning payment template");
            return "payment";
            
        } catch (Exception e) {
            System.err.println("Payment: Exception occurred: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // My Bookings page
    @GetMapping("/my-bookings")
    public String myBookings(Model model, Authentication authentication) {
        System.out.println("My Bookings: Processing request");

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("My Bookings: User not authenticated, redirecting to login");
            return "redirect:/auth/login?returnUrl=/my-bookings";
        }

        String email = authentication.getName();
        try {
            var bookings = bookingService.getBookingsByUser(email);
            model.addAttribute("bookings", bookings);
            System.out.println("My Bookings: Found " + bookings.size() + " bookings for user");
        } catch (Exception e) {
            System.out.println("My Bookings: Error loading bookings - " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("bookings", java.util.Collections.emptyList());
            model.addAttribute("errorMessage", "Error loading your bookings");
        }

        return "my-bookings";
    }

    // Profile page
    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        System.out.println("Profile: Processing request");

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("Profile: User not authenticated, redirecting to login");
            return "redirect:/auth/login?returnUrl=/profile";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        if (user != null) {
            model.addAttribute("user", user);
            System.out.println("Profile: User found - " + user.getFirstName());

            // Get or create customer profile
            try {
                var customer = customerService.getOrCreateCustomerProfile(user);
                model.addAttribute("customer", customer);
                System.out.println("Profile: Customer profile loaded/created");
            } catch (Exception e) {
                System.out.println("Profile: Error loading customer profile - " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("errorMessage", "Error loading profile data");
            }
        } else {
            System.out.println("Profile: User not found for email " + email);
            model.addAttribute("errorMessage", "User not found");
        }

        return "profile";
    }

    // Single booking view page
    @GetMapping("/booking/{bookingId}")
    public String viewBooking(@PathVariable Long bookingId, Model model, Authentication authentication) {
        System.out.println("View Booking: Processing request for booking ID: " + bookingId);

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("View Booking: User not authenticated, redirecting to login");
            return "redirect:/auth/login?returnUrl=/booking/" + bookingId;
        }

        String email = authentication.getName();
        try {
            var booking = bookingService.getBookingById(bookingId);
            
            // Check if this booking belongs to the logged-in user
            if (!booking.getCustomerEmail().equals(email)) {
                System.out.println("View Booking: Access denied - booking does not belong to user");
                return "redirect:/my-bookings?error=access_denied";
            }
            
            model.addAttribute("booking", booking);
            System.out.println("View Booking: Booking found - " + booking.getBookingReference());
            
            return "booking-detail";
            
        } catch (Exception e) {
            System.out.println("View Booking: Error loading booking - " + e.getMessage());
            e.printStackTrace();
            return "redirect:/my-bookings?error=booking_not_found";
        }
    }

    // Update booking page
    @GetMapping("/booking/{bookingId}/edit")
    public String editBooking(@PathVariable Long bookingId, Model model, Authentication authentication) {
        System.out.println("Edit Booking: Processing request for booking ID: " + bookingId);

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("Edit Booking: User not authenticated, redirecting to login");
            return "redirect:/auth/login?returnUrl=/booking/" + bookingId + "/edit";
        }

        String email = authentication.getName();
        try {
            var booking = bookingService.getBookingById(bookingId);
            
            // Check if this booking belongs to the logged-in user
            if (!booking.getCustomerEmail().equals(email)) {
                System.out.println("Edit Booking: Access denied - booking does not belong to user");
                return "redirect:/my-bookings?error=access_denied";
            }

            // Check if booking can be edited
            if (booking.getBookingStatus().name().equals("CANCELLED")) {
                System.out.println("Edit Booking: Cannot edit cancelled booking");
                return "redirect:/booking/" + bookingId + "?error=cannot_edit_cancelled";
            }

            if (booking.getBookingStatus().name().equals("CHECKED_OUT")) {
                System.out.println("Edit Booking: Cannot edit completed booking");
                return "redirect:/booking/" + bookingId + "?error=cannot_edit_completed";
            }
            
            model.addAttribute("booking", booking);
            System.out.println("Edit Booking: Booking found for editing - " + booking.getBookingReference());
            
            return "booking-edit";
            
        } catch (Exception e) {
            System.out.println("Edit Booking: Error loading booking - " + e.getMessage());
            e.printStackTrace();
            return "redirect:/my-bookings?error=booking_not_found";
        }
    }

    // Handle booking update via form submission
    @PostMapping("/booking/{bookingId}/update")
    public String updateBookingForm(@PathVariable Long bookingId,
                                  @RequestParam("numberOfGuests") Integer numberOfGuests,
                                  @RequestParam(value = "specialRequests", required = false) String specialRequests,
                                  @RequestParam(value = "customerNotes", required = false) String customerNotes,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            System.out.println("🔄 ===== FORM UPDATE BOOKING REQUEST =====");
            System.out.println("📝 Booking ID: " + bookingId);
            System.out.println("📝 Number of guests: " + numberOfGuests);
            System.out.println("📝 Special requests: " + specialRequests);
            System.out.println("📝 Customer notes: " + customerNotes);
            System.out.println("📝 User: " + authentication.getName());

            // Create update data map
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("numberOfGuests", numberOfGuests);
            updateData.put("specialRequests", specialRequests);
            updateData.put("customerNotes", customerNotes);

            // Verify user owns this booking
            String userEmail = authentication.getName();
            var existingBooking = bookingService.getBookingById(bookingId);
            
            if (!existingBooking.getCustomerEmail().equals(userEmail)) {
                System.out.println("❌ Access denied: booking belongs to " + existingBooking.getCustomerEmail() + ", user is " + userEmail);
                redirectAttributes.addFlashAttribute("error", "You can only update your own bookings");
                return "redirect:/my-bookings";
            }

            // Update the booking
            var updatedBooking = bookingService.updateBookingDetails(bookingId, updateData);
            System.out.println("✅ Booking updated successfully via form");

            redirectAttributes.addFlashAttribute("success", "Booking updated successfully!");
            return "redirect:/booking/" + bookingId;

        } catch (Exception e) {
            System.err.println("❌ Error updating booking via form: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error updating booking: " + e.getMessage());
            return "redirect:/booking/" + bookingId + "/edit";
        }
    }

    // Booking confirmation page
    @GetMapping("/booking/confirmation")
    public String bookingConfirmation(Model model,
                                      @RequestParam("bookingId") Long bookingId,
                                      Authentication authentication) {

        System.out.println("Booking Confirmation: Processing request for bookingId: " + bookingId);

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("Booking Confirmation: User not authenticated, redirecting to login");
            return "redirect:/auth/login";
        }

        try {
            var booking = bookingService.getBookingById(bookingId);
            model.addAttribute("booking", booking);
            System.out.println("Booking Confirmation: Booking found - " + booking.getBookingReference());
            return "booking-confirmation";
        } catch (Exception e) {
            System.out.println("Booking Confirmation: Error loading booking - " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "Booking not found");
            return "redirect:/dashboard";
        }
    }

    // Payment success page
    @GetMapping("/payment/success")
    public String paymentSuccess(Model model,
                                 @RequestParam(value = "order_id", required = false) String orderId) {

        System.out.println("Payment Success: Processing request for orderId: " + orderId);

        if (orderId != null) {
            try {
                var booking = bookingService.getBookingByReference(orderId);
                model.addAttribute("booking", booking);
                System.out.println("Payment Success: Booking found for orderId: " + orderId);
            } catch (Exception e) {
                System.out.println("Payment Success: Error loading booking for orderId: " + orderId + " - " + e.getMessage());
                model.addAttribute("errorMessage", "Booking not found");
            }
        }

        return "payment-success";
    }

    // Payment cancel page
    @GetMapping("/payment/cancel")
    public String paymentCancel(Model model) {
        System.out.println("Payment Cancel: Processing request");
        model.addAttribute("message", "Payment was cancelled. You can try again or contact support.");
        return "payment-cancel";
    }


    // Custom payment page
    @GetMapping("/custom-payment")
    public String customPayment(Model model) {
        System.out.println("Custom Payment: Processing request");
        return "custom-payment";
    }

    // Terms and conditions page
    @GetMapping("/terms")
    public String terms() {
        System.out.println("Terms: Processing request");
        return "terms";
    }

    // Privacy policy page
    @GetMapping("/privacy")
    public String privacy() {
        System.out.println("Privacy: Processing request");
        return "privacy";
    }

    // Contact page
    @GetMapping("/contact")
    public String contact() {
        System.out.println("Contact: Processing request");
        return "contact";
    }

    // About page
    @GetMapping("/about")
    public String about() {
        System.out.println("About: Processing request");
        return "about";
    }

}