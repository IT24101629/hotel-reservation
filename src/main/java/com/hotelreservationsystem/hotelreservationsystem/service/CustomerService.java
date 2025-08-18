package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.Customer;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import com.hotelreservationsystem.hotelreservationsystem.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    public Customer findByUser(User user) {
        return customerRepository.findByUser(user).orElse(null);
    }

    public Customer findByUserId(Long userId) {
        return customerRepository.findByUser_UserId(userId).orElse(null);
    }

    public Customer findByEmail(String email) {
        return customerRepository.findByUser_Email(email).orElse(null);
    }

    public Customer save(Customer customer) {
        customer.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    public Customer createCustomerProfile(User user) {
        // Check if customer profile already exists
        Optional<Customer> existingCustomer = customerRepository.findByUser(user);
        if (existingCustomer.isPresent()) {
            return existingCustomer.get();
        }

        // Create new customer profile
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        return customerRepository.save(customer);
    }

    public Customer getOrCreateCustomerProfile(User user) {
        Customer customer = findByUser(user);
        if (customer == null) {
            customer = createCustomerProfile(user);
        }
        return customer;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public boolean existsByUserId(Long userId) {
        return customerRepository.existsByUser_UserId(userId);
    }

    public void deleteCustomer(Long customerId) {
        customerRepository.deleteById(customerId);
    }
}