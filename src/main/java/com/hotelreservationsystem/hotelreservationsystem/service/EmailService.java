package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.Booking;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendBookingConfirmation(Booking booking, String customerEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(customerEmail);
            helper.setSubject("🎉 Booking Confirmation - " + booking.getBookingReference());
            helper.setText(createBookingConfirmationHtml(booking), true);
            
            // Generate and attach QR code
            byte[] qrCodeImage = generateQRCode(booking.getBookingReference(), 
                "Booking: " + booking.getBookingReference() + 
                "\nCheck-in: " + booking.getCheckInDate() + 
                "\nRoom: " + booking.getRoom().getRoomNumber());
            
            helper.addAttachment("booking-qr-" + booking.getBookingReference() + ".png", 
                                new ByteArrayResource(qrCodeImage));
            
            mailSender.send(mimeMessage);
            System.out.println("Booking confirmation email sent with QR code to: " + customerEmail);
        } catch (Exception e) {
            System.err.println("Failed to send booking confirmation email: " + e.getMessage());
            e.printStackTrace();
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
    
    private String createBookingConfirmationHtml(Booking booking) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <style>" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            "        .header { background: linear-gradient(135deg, #3498db, #2c3e50); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
            "        .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }" +
            "        .booking-details { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #3498db; }" +
            "        .detail-row { display: flex; justify-content: space-between; margin: 10px 0; padding: 8px 0; border-bottom: 1px solid #eee; }" +
            "        .label { font-weight: bold; color: #2c3e50; }" +
            "        .value { color: #34495e; }" +
            "        .total { font-size: 1.2em; font-weight: bold; color: #27ae60; background: #e8f5e8; padding: 15px; border-radius: 5px; text-align: center; }" +
            "        .qr-note { background: #fff3cd; color: #856404; padding: 15px; border-radius: 5px; margin: 20px 0; }" +
            "        .footer { text-align: center; margin: 30px 0; color: #7f8c8d; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class='container'>" +
            "        <div class='header'>" +
            "            <h1>🎉 Booking Confirmed!</h1>" +
            "            <p>Thank you for choosing Gold Palm Hotel</p>" +
            "        </div>" +
            "        <div class='content'>" +
            "            <h2>Dear Valued Guest,</h2>" +
            "            <p>We're delighted to confirm your reservation! Your booking has been successfully processed.</p>" +
            "            " +
            "            <div class='booking-details'>" +
            "                <h3>📋 Booking Details</h3>" +
            "                <div class='detail-row'>" +
            "                    <span class='label'>🔖 Reference Number:</span>" +
            "                    <span class='value'>%s</span>" +
            "                </div>" +
            "                <div class='detail-row'>" +
            "                    <span class='label'>🏨 Room:</span>" +
            "                    <span class='value'>Room %s (%s)</span>" +
            "                </div>" +
            "                <div class='detail-row'>" +
            "                    <span class='label'>📅 Check-in Date:</span>" +
            "                    <span class='value'>%s</span>" +
            "                </div>" +
            "                <div class='detail-row'>" +
            "                    <span class='label'>📅 Check-out Date:</span>" +
            "                    <span class='value'>%s</span>" +
            "                </div>" +
            "                <div class='detail-row'>" +
            "                    <span class='label'>👥 Number of Guests:</span>" +
            "                    <span class='value'>%d</span>" +
            "                </div>" +
            "                <div class='detail-row'>" +
            "                    <span class='label'>🌃 Number of Nights:</span>" +
            "                    <span class='value'>%d</span>" +
            "                </div>" +
            "                %s" +
            "            </div>" +
            "            " +
            "            <div class='total'>" +
            "                💰 Total Amount: LKR %.2f" +
            "            </div>" +
            "            " +
            "            <div class='qr-note'>" +
            "                <strong>📱 QR Code Attached!</strong><br>" +
            "                We've attached a QR code with your booking details. Show this at check-in for faster service!" +
            "            </div>" +
            "            " +
            "            <h3>🏨 Hotel Information</h3>" +
            "            <p><strong>Address:</strong> 123 Luxury Avenue, Colombo 03, Sri Lanka<br>" +
            "            <strong>Phone:</strong> +94 11 1234567<br>" +
            "            <strong>Email:</strong> info@goldpalmhotel.com</p>" +
            "            " +
            "            <h3>ℹ️ Important Information</h3>" +
            "            <ul>" +
            "                <li><strong>Check-in Time:</strong> 2:00 PM</li>" +
            "                <li><strong>Check-out Time:</strong> 11:00 AM</li>" +
            "                <li><strong>Cancellation:</strong> Free cancellation up to 24 hours before check-in</li>" +
            "                <li><strong>ID Required:</strong> Please bring a valid government-issued photo ID</li>" +
            "            </ul>" +
            "            " +
            "            <div class='footer'>" +
            "                <p>We look forward to welcoming you!</p>" +
            "                <p><em>Gold Palm Hotel Team</em></p>" +
            "                <p style='font-size: 0.9em; margin-top: 20px;'>" +
            "                    If you have any questions, please don't hesitate to contact us at" +
            "                    <a href='mailto:support@goldpalmhotel.com'>support@goldpalmhotel.com</a>" +
            "                </p>" +
            "            </div>" +
            "        </div>" +
            "    </div>" +
            "</body>" +
            "</html>",
            booking.getBookingReference(),
            booking.getRoom().getRoomNumber(),
            booking.getRoom().getRoomType().getTypeName(),
            booking.getCheckInDate(),
            booking.getCheckOutDate(),
            booking.getNumberOfGuests(),
            calculateNights(booking),
            booking.getSpecialRequests() != null && !booking.getSpecialRequests().trim().isEmpty() ?
                String.format("<div class='detail-row'><span class='label'>📝 Special Requests:</span><span class='value'>%s</span></div>", 
                            booking.getSpecialRequests()) : "",
            booking.getTotalAmount()
        );
    }
    
    private long calculateNights(Booking booking) {
        return java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
    }
    
    private byte[] generateQRCode(String bookingReference, String qrContent) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300);
        
        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        
        return baos.toByteArray();
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