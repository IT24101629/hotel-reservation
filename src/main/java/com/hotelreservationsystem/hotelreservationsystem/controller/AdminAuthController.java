package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.BookingStatus;
import com.hotelreservationsystem.hotelreservationsystem.model.RoomStatus;
import com.hotelreservationsystem.hotelreservationsystem.model.UserRole;
import com.hotelreservationsystem.hotelreservationsystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin")
public class AdminAuthController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    // Show admin/staff login form
    @GetMapping("/login")
    public String showAdminLoginForm(@RequestParam(value = "error", required = false) String error,
                                     @RequestParam(value = "logout", required = false) String logout,
                                     @RequestParam(value = "returnUrl", required = false) String returnUrl,
                                     Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password");
        }

        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully");
        }

        if (returnUrl != null) {
            model.addAttribute("returnUrl", returnUrl);
        }

        return "admin/login";
    }

    // Handle admin login success
    @GetMapping("/login/success")
    public String adminLoginSuccess(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if ("ROLE_ADMIN".equals(role)) {
            return "redirect:/admin/dashboard";
        } else if ("ROLE_STAFF".equals(role)) {
            return "redirect:/receptionist/dashboard";
        } else {
            // If customer somehow logs in through admin login, redirect to customer dashboard
            return "redirect:/dashboard";
        }
    }

    // Admin dashboard (default admin landing page)
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if ("ROLE_ADMIN".equals(role)) {
            // Get statistics
            long totalBookings = bookingRepository.count();
            long pendingBookings = bookingRepository.countByBookingStatus(BookingStatus.PENDING);
            long totalRooms = roomRepository.count();
            long availableRooms = roomRepository.countByStatus(RoomStatus.AVAILABLE);
            long totalCustomers = customerRepository.count();
            long activePromotions = promotionRepository.countByIsActiveTrue();
            long totalStaff = userRepository.countByUserRole(UserRole.STAFF) + userRepository.countByUserRole(UserRole.ADMIN);

            // Calculate total revenue from completed bookings
            BigDecimal totalRevenue = paymentRepository.findAll().stream()
                    .map(payment -> payment.getAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Get recent bookings (last 5)
            var recentBookings = bookingRepository.findTop5ByOrderByCreatedAtDesc();

            // Get admin name
            String adminName = authentication.getName();

            model.addAttribute("totalBookings", totalBookings);
            model.addAttribute("pendingBookings", pendingBookings);
            model.addAttribute("totalRooms", totalRooms);
            model.addAttribute("availableRooms", availableRooms);
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("activePromotions", activePromotions);
            model.addAttribute("totalStaff", totalStaff);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("recentBookings", recentBookings);
            model.addAttribute("adminName", adminName);

            return "admin/dashboard";
        } else if ("ROLE_STAFF".equals(role)) {
            return "redirect:/receptionist/dashboard";
        } else {
            return "redirect:/dashboard";
        }
    }
}