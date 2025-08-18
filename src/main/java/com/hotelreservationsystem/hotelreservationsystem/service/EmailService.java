package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendBookingConfirmation(Booking booking, String customerEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(customerEmail);
            message.setSubject("Booking Confirmation - " + booking.getBookingReference());
            message.setText(createBookingConfirmationText(booking));
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send booking confirmation email: " + e.getMessage());
        }
    }
    
    public void sendBookingCancellation(Booking booking, String customerEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(customerEmail);
            message.setSubject("Booking Cancellation - " + booking.getBookingReference());
            message.setText(createBookingCancellationText(booking));
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send booking cancellation email: " + e.getMessage());
        }
    }
    
    public void sendCheckInReminder(Booking booking, String customerEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(customerEmail);
            message.setSubject("Check-in Reminder - " + booking.getBookingReference());
            message.setText(createCheckInReminderText(booking));
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send check-in reminder email: " + e.getMessage());
        }
    }
    
    private String createBookingConfirmationText(Booking booking) {
        return String.format(
            "Dear Guest,\n\n" +
            "Your booking has been confirmed!\n\n" +
            "Booking Details:\n" +
            "Reference: %s\n" +
            "Check-in Date: %s\n" +
            "Check-out Date: %s\n" +
            "Number of Guests: %d\n" +
            "Total Amount: $%.2f\n\n" +
            "Special Requests: %s\n\n" +
            "Thank you for choosing our hotel!\n\n" +
            "Best regards,\n" +
            "Hotel Reservation System",
            booking.getBookingReference(),
            booking.getCheckInDate(),
            booking.getCheckOutDate(),
            booking.getNumberOfGuests(),
            booking.getTotalAmount(),
            booking.getSpecialRequests() != null ? booking.getSpecialRequests() : "None"
        );
    }
    
    private String createBookingCancellationText(Booking booking) {
        return String.format(
            "Dear Guest,\n\n" +
            "Your booking has been cancelled.\n\n" +
            "Booking Details:\n" +
            "Reference: %s\n" +
            "Check-in Date: %s\n" +
            "Check-out Date: %s\n" +
            "Cancellation Reason: %s\n\n" +
            "If you have any questions, please contact us.\n\n" +
            "Best regards,\n" +
            "Hotel Reservation System",
            booking.getBookingReference(),
            booking.getCheckInDate(),
            booking.getCheckOutDate(),
            booking.getCancellationReason() != null ? booking.getCancellationReason() : "Not specified"
        );
    }
    
    private String createCheckInReminderText(Booking booking) {
        return String.format(
            "Dear Guest,\n\n" +
            "This is a reminder that your check-in is scheduled for today!\n\n" +
            "Booking Details:\n" +
            "Reference: %s\n" +
            "Check-in Date: %s\n" +
            "Check-out Date: %s\n" +
            "Number of Guests: %d\n\n" +
            "We look forward to welcoming you!\n\n" +
            "Best regards,\n" +
            "Hotel Reservation System",
            booking.getBookingReference(),
            booking.getCheckInDate(),
            booking.getCheckOutDate(),
            booking.getNumberOfGuests()
        );
    }
}