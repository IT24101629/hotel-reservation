package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.Customer;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    // Find customer by user
    Optional<Customer> findByUser(User user);
    
    // Find customer by user ID
    Optional<Customer> findByUser_UserId(Long userId);
    
    // Find customer by email (through user relationship)
    Optional<Customer> findByUser_Email(String email);
    
    // Find customer by phone number
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    
    // Check if customer exists by user ID
    boolean existsByUser_UserId(Long userId);
}