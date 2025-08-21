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
            "        .detail-row { margin: 10px 0; padding: 8px 0; border-bottom: 1px solid #eee; }" +
            "        .detail-table { width: 100%; border-collapse: collapse; }" +
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
                String.format("<tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>📝 Special Requests:</td><td class='value' style='color: #34495e; text-align: right;'>%s</td></tr>", 
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
    
    /**
     * Send booking confirmation with QR code after payment success
     */
    public void sendBookingConfirmationWithQR(Booking booking, String customerEmail, String qrCodeBase64) {
        try {
            System.out.println("🔄 Starting email sending process...");
            System.out.println("📧 Customer email: " + customerEmail);
            System.out.println("🎫 Booking reference: " + booking.getBookingReference());
            System.out.println("📱 QR code provided: " + (qrCodeBase64 != null && !qrCodeBase64.isEmpty()));
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(customerEmail);
            helper.setSubject("✅ Payment Confirmed - Booking " + booking.getBookingReference());
            helper.setFrom("goldpalmhotelsliit@gmail.com");
            
            System.out.println("📝 Creating email content...");
            String emailContent = createPaymentConfirmationHtml(booking, qrCodeBase64);
            helper.setText(emailContent, true);
            
            // Attach QR code as image if Base64 provided
            if (qrCodeBase64 != null && !qrCodeBase64.isEmpty()) {
                System.out.println("📎 Attaching QR code...");
                byte[] qrCodeBytes = java.util.Base64.getDecoder().decode(qrCodeBase64);
                ByteArrayResource qrResource = new ByteArrayResource(qrCodeBytes);
                helper.addAttachment("booking-qr-code.png", qrResource);
            }
            
            System.out.println("📤 Sending email...");
            mailSender.send(mimeMessage);
            System.out.println("✅ Payment confirmation email with QR code sent to: " + customerEmail);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending payment confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String createPaymentConfirmationHtml(Booking booking, String qrCodeBase64) {
        return String.format(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "    <meta charset='UTF-8'>" +
            "    <style>" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
            "        .header { background: linear-gradient(135deg, #27ae60, #2c3e50); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
            "        .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 10px 10px; }" +
            "        .payment-success { background: #d4edda; color: #155724; padding: 20px; margin: 20px 0; border-radius: 8px; border: 1px solid #c3e6cb; text-align: center; }" +
            "        .booking-details { background: white; padding: 20px; margin: 20px 0; border-radius: 8px; border-left: 4px solid #27ae60; }" +
            "        .detail-row { margin: 10px 0; padding: 8px 0; border-bottom: 1px solid #eee; }" +
            "        .detail-table { width: 100%; border-collapse: collapse; }" +
            "        .label { font-weight: bold; color: #2c3e50; }" +
            "        .value { color: #34495e; }" +
            "        .total { font-size: 1.2em; font-weight: bold; color: #27ae60; background: #e8f5e8; padding: 15px; border-radius: 5px; text-align: center; }" +
            "        .qr-section { background: white; padding: 25px; margin: 20px auto; border-radius: 8px; text-align: center; border: 2px solid #27ae60; box-shadow: 0 2px 5px rgba(0,0,0,0.1); max-width: 400px; }" +
            "        .qr-code { width: 200px; height: 200px; margin: 15px auto; display: block; border: 2px solid #27ae60; border-radius: 8px; }" +
            "        .footer { text-align: center; margin: 30px 0; color: #7f8c8d; }" +
            "    </style>" +
            "</head>" +
            "<body>" +
            "    <div class='container'>" +
            "        <div class='header'>" +
            "            <h1>🎉 Payment Confirmed!</h1>" +
            "            <p>Your hotel booking has been successfully paid and confirmed</p>" +
            "        </div>" +
            "        <div class='content'>" +
            "            <div class='payment-success'>" +
            "                <h2>✅ Payment Successful</h2>" +
            "                <p>Your payment has been processed successfully. Your booking is now confirmed!</p>" +
            "            </div>" +
            "            <div class='booking-details'>" +
            "                <h3>📋 Booking Details</h3>" +
            "                <table class='detail-table' width='100%' cellpadding='8' cellspacing='0'>" +
            "                    <tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>🎫 Booking Reference:</td><td class='value' style='color: #34495e; text-align: right;'>%s</td></tr>" +
            "                    <tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>🏨 Room Number:</td><td class='value' style='color: #34495e; text-align: right;'>%s</td></tr>" +
            "                    <tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>🛏️ Room Type:</td><td class='value' style='color: #34495e; text-align: right;'>%s</td></tr>" +
            "                    <tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>📅 Check-in Date:</td><td class='value' style='color: #34495e; text-align: right;'>%s</td></tr>" +
            "                    <tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>📅 Check-out Date:</td><td class='value' style='color: #34495e; text-align: right;'>%s</td></tr>" +
            "                    <tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>👥 Number of Guests:</td><td class='value' style='color: #34495e; text-align: right;'>%d</td></tr>" +
            "                    <tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>🌙 Number of Nights:</td><td class='value' style='color: #34495e; text-align: right;'>%d</td></tr>" +
            "                    %s" +
            "                </table>" +
            "                <div class='total' style='font-size: 1.2em; font-weight: bold; color: #27ae60; background: #e8f5e8; padding: 15px; border-radius: 5px; text-align: center; margin-top: 15px;'>💰 Total Amount Paid: LKR %.2f</div>" +
            "            </div>" +
            (qrCodeBase64 != null && !qrCodeBase64.isEmpty() ?
                "            <table class='qr-section' width='100%' cellpadding='0' cellspacing='0' style='background: white; padding: 25px; margin: 20px auto; border-radius: 8px; text-align: center; border: 2px solid #27ae60; box-shadow: 0 2px 5px rgba(0,0,0,0.1); max-width: 400px;'>" +
                "                <tr>" +
                "                    <td style='text-align: center; padding: 10px;'>" +
                "                        <h3 style='color: #27ae60; margin: 0 0 15px 0; font-size: 18px;'>📱 Your Booking QR Code</h3>" +
                "                        <p style='margin: 0 0 20px 0; font-weight: 500; color: #333;'>Present this QR code at check-in for quick verification:</p>" +
                "                        <div style='text-align: center; margin: 20px 0;'>" +
                "                            <img src='data:image/png;base64," + qrCodeBase64 + "' alt='Booking QR Code' style='width: 200px; height: 200px; border: 2px solid #27ae60; border-radius: 8px; display: block; margin: 0 auto;'>" +
                "                        </div>" +
                "                        <p style='margin: 15px 0 0 0; color: #666; font-size: 0.9em;'>📎 QR code is also attached as a separate file for convenience.</p>" +
                "                    </td>" +
                "                </tr>" +
                "            </table>" : "") +
            "            <div class='footer'>" +
            "                <p><strong>What's Next?</strong></p>" +
            "                <ul style='text-align: left; display: inline-block;'>" +
            "                    <li>Save this email for your records</li>" +
            "                    <li>Present the QR code at check-in</li>" +
            "                    <li>Arrive on your check-in date</li>" +
            "                    <li>Enjoy your stay!</li>" +
            "                </ul>" +
            "                <p style='margin-top: 30px;'><em>Thank you for choosing Gold Palm Hotel!</em></p>" +
            "                <p style='font-size: 0.9em; margin-top: 20px;'>" +
            "                    Questions? Contact us at <a href='mailto:support@goldpalmhotel.com'>support@goldpalmhotel.com</a>" +
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
                String.format("<tr class='detail-row'><td class='label' style='font-weight: bold; color: #2c3e50; width: 50%;'>📝 Special Requests:</td><td class='value' style='color: #34495e; text-align: right;'>%s</td></tr>", 
                            booking.getSpecialRequests()) : "",
            booking.getTotalAmount()
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