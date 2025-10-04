package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.model.UserRole;
import com.hotelreservationsystem.hotelreservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AdminStaffController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listStaff(Model model) {
        List<User> staffList = userRepository.findAll().stream()
                .filter(user -> user.getRole() != UserRole.ADMIN)
                .collect(Collectors.toList());
        model.addAttribute("staffList", staffList);
        return "admin/staff-list";
    }

    @GetMapping("/new")
    public String showAddStaffForm(Model model) {
        model.addAttribute("staff", new User());
        List<UserRole> roles = Arrays.stream(UserRole.values())
                .filter(role -> role != UserRole.ADMIN)
                .collect(Collectors.toList());
        model.addAttribute("roles", roles);
        return "admin/staff-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditStaffForm(@PathVariable Long id, Model model) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid staff Id:" + id));
        model.addAttribute("staff", staff);
        List<UserRole> roles = Arrays.stream(UserRole.values())
                .filter(role -> role != UserRole.ADMIN)
                .collect(Collectors.toList());
        model.addAttribute("roles", roles);
        return "admin/staff-form";
    }

    @PostMapping("/save")
    public String saveStaff(@ModelAttribute("staff") User staff, RedirectAttributes redirectAttributes) {
        // When updating, the password field might be empty.
        // If it is, we should keep the old password.
        if (staff.getUserId() != null) { // This is an update
            User existingUser = userRepository.findById(staff.getUserId()).orElseThrow();
            if (staff.getPasswordHash() == null || staff.getPasswordHash().isEmpty()) {
                staff.setPasswordHash(existingUser.getPasswordHash()); // Keep old password
            } else {
                staff.setPasswordHash(passwordEncoder.encode(staff.getPasswordHash())); // Set new password
            }
        } else { // This is a new user
            // Check if username already exists for new users
            if (userRepository.findByUsername(staff.getUsername()).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Username already exists!");
                return "redirect:/admin/staff/new";
            }
            staff.setPasswordHash(passwordEncoder.encode(staff.getPasswordHash()));
        }

        userRepository.save(staff);
        redirectAttributes.addFlashAttribute("successMessage", "Staff member saved successfully!");
        return "redirect:/admin/staff";
    }

    @GetMapping("/delete/{id}")
    public String deleteStaff(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Staff member deleted successfully!");
        return "redirect:/admin/staff";
    }
}