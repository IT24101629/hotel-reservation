package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.dto.UserRegistrationDTO;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    // Show registration form
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDTO());
        return "register";
    }

    // Process registration
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDTO registrationDto,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        // Check for validation errors
        if (result.hasErrors()) {
            return "register";
        }

        // Check if email already exists
        if (userService.existsByEmail(registrationDto.getEmail())) {
            result.rejectValue("email", "error.user", "Email already exists");
        }

        // Check if username already exists
        if (userService.existsByUsername(registrationDto.getUsername())) {
            result.rejectValue("username", "error.user", "Username already exists");
        }

        // Check password confirmation
        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Passwords do not match");
        }

        if (result.hasErrors()) {
            return "register";
        }

        try {
            // Register the user
            User user = userService.registerUser(registrationDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! You can now log in with your credentials.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "register";
        }
    }

    // Show login form
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
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

        return "login";
    }

    // Handle login success
    @GetMapping("/login/success")
    public String loginSuccess(Authentication authentication) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        if ("ROLE_ADMIN".equals(role)) {
            return "redirect:/admin/dashboard";
        } else if ("ROLE_STAFF".equals(role)) {
            return "redirect:/receptionist/dashboard";
        } else {
            return "redirect:/dashboard";
        }
    }

    // Logout
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response,
                         RedirectAttributes redirectAttributes) {

        SecurityContextHolder.clearContext();
        redirectAttributes.addFlashAttribute("successMessage", "You have been logged out successfully");
        return "redirect:/auth/login";
    }

    // Forgot password form
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    // Process forgot password
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        try {
            userService.processForgotPassword(email);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password reset instructions have been sent to your email");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error processing password reset: " + e.getMessage());
            return "forgot-password";
        }
    }

    // Password reset form
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        // Validate reset token
        if (!userService.validatePasswordResetToken(token)) {
            model.addAttribute("errorMessage", "Invalid or expired password reset token");
            return "login";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    // Process password reset
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("newPassword") String newPassword,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Passwords do not match");
            model.addAttribute("token", token);
            return "reset-password";
        }

        try {
            userService.resetPassword(token, newPassword);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password reset successful! You can now log in with your new password.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error resetting password: " + e.getMessage());
            model.addAttribute("token", token);
            return "reset-password";
        }
    }
}