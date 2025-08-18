package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.model.UserRole;
import com.hotelreservationsystem.hotelreservationsystem.service.BookingService;
import com.hotelreservationsystem.hotelreservationsystem.service.RoomService;
import com.hotelreservationsystem.hotelreservationsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private BookingService bookingService;

    // Home page
    @GetMapping("/")
    public String home(Model model) {
        // Add any data needed for the home page
        model.addAttribute("totalRooms", roomService.getTotalRoomsCount());
        model.addAttribute("availableRooms", roomService.getAvailableRoomsCount());
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
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userService.findByEmail(email);

            if (user != null) {
                model.addAttribute("user", user);

                // Add user-specific data based on role
                if (UserRole.CUSTOMER.equals(user.getRole())) {
                    // Customer dashboard data
                    try {
                        model.addAttribute("recentBookings", bookingService.getBookingsByCustomer(user.getUserId()));
                    } catch (Exception e) {
                        // Handle gracefully if no customer profile exists yet
                        model.addAttribute("recentBookings", java.util.Collections.emptyList());
                    }
                } else if (UserRole.ADMIN.equals(user.getRole())) {
                    // Admin dashboard data
                    model.addAttribute("allBookings", bookingService.getAllBookings());
                    model.addAttribute("todaysCheckIns", bookingService.getTodaysCheckIns());
                    model.addAttribute("todaysCheckOuts", bookingService.getTodaysCheckOuts());
                }
            }
        }
        return "dashboard";
    }

    // Rooms page
    @GetMapping("/rooms")
    public String rooms(Model model,
                        @RequestParam(value = "checkIn", required = false) String checkIn,
                        @RequestParam(value = "checkOut", required = false) String checkOut,
                        @RequestParam(value = "guests", required = false) Integer guests,
                        @RequestParam(value = "roomType", required = false) String roomType) {

        // Add search parameters to model
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("guests", guests);
        model.addAttribute("roomType", roomType);

        // Get available rooms based on search criteria
        if (checkIn != null && checkOut != null) {
            model.addAttribute("availableRooms",
                    roomService.findAvailableRoomsForDates(checkIn, checkOut, guests, roomType));
        } else {
            model.addAttribute("availableRooms", roomService.getAllAvailableRooms());
        }

        // Get room types for filter
        model.addAttribute("roomTypes", roomService.getAllRoomTypes());

        return "rooms";
    }

    // Booking page
    @GetMapping("/booking")
    public String booking(Model model, Authentication authentication) {
        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login?returnUrl=/booking";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);
        model.addAttribute("user", user);

        return "booking";
    }

    // Payment page
    @GetMapping("/payment")
    public String payment(Model model,
                          @RequestParam(value = "bookingId", required = false) Long bookingId,
                          Authentication authentication) {

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login?returnUrl=/payment";
        }

        if (bookingId != null) {
            model.addAttribute("bookingId", bookingId);
        }

        return "payment";
    }

    // Booking confirmation page
    @GetMapping("/booking/confirmation")
    public String bookingConfirmation(Model model,
                                      @RequestParam("bookingId") Long bookingId,
                                      Authentication authentication) {

        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        try {
            model.addAttribute("booking", bookingService.getBookingById(bookingId));
            return "booking-confirmation";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Booking not found");
            return "redirect:/dashboard";
        }
    }

    // Payment success page
    @GetMapping("/payment/success")
    public String paymentSuccess(Model model,
                                 @RequestParam(value = "order_id", required = false) String orderId) {

        if (orderId != null) {
            try {
                model.addAttribute("booking", bookingService.getBookingByReference(orderId));
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Booking not found");
            }
        }

        return "payment-success";
    }

    // Payment cancel page
    @GetMapping("/payment/cancel")
    public String paymentCancel(Model model) {
        model.addAttribute("message", "Payment was cancelled. You can try again or contact support.");
        return "payment-cancel";
    }

    // My Bookings page
    @GetMapping("/my-bookings")
    public String myBookings(Model model, Authentication authentication) {
        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login?returnUrl=/my-bookings";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        if (user != null) {
            try {
                model.addAttribute("bookings", bookingService.getBookingsByCustomer(user.getUserId()));
                model.addAttribute("user", user);
            } catch (Exception e) {
                model.addAttribute("bookings", java.util.Collections.emptyList());
                model.addAttribute("errorMessage", "Unable to load your bookings at this time.");
            }
        }

        return "my-bookings";
    }

    // Profile page
    @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        // Check if user is logged in
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login?returnUrl=/profile";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);
        model.addAttribute("user", user);

        return "profile";
    }

    // Admin Dashboard
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        // Check if user is logged in and is admin
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        if (user == null || !UserRole.ADMIN.equals(user.getRole())) {
            return "redirect:/dashboard?error=access_denied";
        }

        // Add admin dashboard data
        model.addAttribute("user", user);
        model.addAttribute("allBookings", bookingService.getAllBookings());
        model.addAttribute("todaysCheckIns", bookingService.getTodaysCheckIns());
        model.addAttribute("todaysCheckOuts", bookingService.getTodaysCheckOuts());
        model.addAttribute("totalRooms", roomService.getTotalRoomsCount());
        model.addAttribute("availableRooms", roomService.getAvailableRoomsCount());

        return "admin/dashboard";
    }

    // Admin Rooms Management
    @GetMapping("/admin/rooms")
    public String adminRooms(Model model, Authentication authentication) {
        // Check if user is logged in and is admin
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        if (user == null || !UserRole.ADMIN.equals(user.getRole())) {
            return "redirect:/dashboard?error=access_denied";
        }

        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("roomTypes", roomService.getAllRoomTypes());

        return "admin/rooms";
    }

    // Admin Bookings Management
    @GetMapping("/admin/bookings")
    public String adminBookings(Model model, Authentication authentication) {
        // Check if user is logged in and is admin
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        if (user == null || !UserRole.ADMIN.equals(user.getRole())) {
            return "redirect:/dashboard?error=access_denied";
        }

        model.addAttribute("bookings", bookingService.getAllBookings());

        return "admin/bookings";
    }

    // Terms and conditions page
    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    // Privacy policy page
    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }

    // Contact page
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    // About page
    @GetMapping("/about")
    public String about() {
        return "about";
    }

    // Error pages
    @GetMapping("/error")
    public String error() {
        return "error";
    }
}