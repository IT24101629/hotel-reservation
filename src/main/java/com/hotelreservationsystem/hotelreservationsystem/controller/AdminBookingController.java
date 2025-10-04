package com.hotelreservationsystem.hotelreservationsystem.controller;

import com.hotelreservationsystem.hotelreservationsystem.model.Booking;
import com.hotelreservationsystem.hotelreservationsystem.model.BookingStatus;
import com.hotelreservationsystem.hotelreservationsystem.model.Room;
import com.hotelreservationsystem.hotelreservationsystem.repository.BookingRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.RoomRepository;
import com.hotelreservationsystem.hotelreservationsystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    @Autowired
    public AdminBookingController(BookingRepository bookingRepository, RoomRepository roomRepository, NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listAllBookings(Model model) {
        model.addAttribute("bookings", bookingRepository.findAll());
        return "admin/bookings-list";
    }

    @GetMapping("/approve/{id}")
    public String approveBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        booking.setBookingStatus(BookingStatus.APPROVED);
        bookingRepository.save(booking);

        Room room = booking.getRoom();
        room.setIsAvailable(false);
        roomRepository.save(room);

        // CREATE NOTIFICATION FOR CUSTOMER
        String message = String.format(
                "Your booking for Room %s from %s to %s has been approved!",
                room.getRoomNumber(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );
        notificationService.createNotification(booking.getCustomer().getUser(), message);

        redirectAttributes.addFlashAttribute("successMessage", "Booking approved and customer notified!");
        return "redirect:/admin/bookings";
    }

    @GetMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Booking booking = bookingRepository.findById(id).orElseThrow();
        BookingStatus previousStatus = booking.getBookingStatus();

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        if (previousStatus == BookingStatus.APPROVED) {
            Room room = booking.getRoom();
            room.setIsAvailable(true);
            roomRepository.save(room);
        }

        // Optional: You could also create a notification for cancellations
        String message = String.format(
                "Your booking for Room %s from %s to %s has been cancelled.",
                booking.getRoom().getRoomNumber(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );
        notificationService.createNotification(booking.getCustomer().getUser(), message);

        redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully!");
        return "redirect:/admin/bookings";
    }
}