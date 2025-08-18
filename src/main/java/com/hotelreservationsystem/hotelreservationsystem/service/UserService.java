package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.dto.UserRegistrationDTO;
import com.hotelreservationsystem.hotelreservationsystem.model.Customer;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.model.UserRole;
import com.hotelreservationsystem.hotelreservationsystem.repository.CustomerRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Service
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getIsActive(),
                true, true, true,
                getAuthorities(user.getRole())
        );
    }

    private Collection<? extends GrantedAuthority> getAuthorities(UserRole role) {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public User registerUser(UserRegistrationDTO registrationDto) {
        // Create user
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setRole(UserRole.CUSTOMER);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        user = userRepository.save(user);

        // Create customer profile
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setPhoneNumber(registrationDto.getPhoneNumber());
        customer.setAddress(registrationDto.getAddress());
        customer.setDateOfBirth(registrationDto.getDateOfBirth());
        customer.setIdNumber(registrationDto.getIdNumber());
        customer.setCountry(registrationDto.getNationality()); // Using country field for nationality
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        customerRepository.save(customer);

        return user;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public User save(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(false);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(true);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // Placeholder methods for password reset functionality
    public void processForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // In a real implementation, you would:
        // 1. Generate a unique reset token
        // 2. Store it with expiration time
        // 3. Send email with reset link
        
        throw new RuntimeException("Password reset functionality not implemented yet");
    }

    public boolean validatePasswordResetToken(String token) {
        // In a real implementation, validate the token and check if it's not expired
        return false;
    }

    public void resetPassword(String token, String newPassword) {
        // In a real implementation:
        // 1. Validate token
        // 2. Find user by token
        // 3. Update password
        // 4. Invalidate token
        
        throw new RuntimeException("Password reset functionality not implemented yet");
    }
}