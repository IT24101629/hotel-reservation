package com.hotelreservationsystem.hotelreservationsystem.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for generating QR codes for hotel bookings.
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code image as a byte array.
     *
     * @param text The text/data to encode in the QR code
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @return Byte array representing the QR code image
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static byte[] generateQRCodeImage(String text, int width, int height) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        System.out.println("QR Code generated for: " + text);
        System.out.println("QR Code dimensions: " + width + "x" + height);

        return outputStream.toByteArray();
    }

    /**
     * Creates a JSON-formatted string containing booking information for a QR code.
     *
     * @param bookingId The booking ID
     * @param bookingReference The booking reference
     * @param customerId The customer ID
     * @param checkInDate Check-in date
     * @param checkOutDate Check-out date
     * @return A JSON-formatted string
     */
    public static String createQRCodeContent(String bookingId, String bookingReference, String customerId, 
                                           String checkInDate, String checkOutDate) {
        return String.format(
            "{\"bookingId\":\"%s\",\"bookingReference\":\"%s\",\"customerId\":\"%s\",\"checkInDate\":\"%s\",\"checkOutDate\":\"%s\",\"timestamp\":\"%s\"}",
            bookingId, bookingReference, customerId, checkInDate, checkOutDate, System.currentTimeMillis());
    }

    /**
     * Creates a Base64 QR code representation that can be used in HTML emails and pages.
     *
     * @param text The text to encode in the QR code
     * @return A Base64 string representing the QR code image
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static String createQRCodeBase64(String text) throws WriterException, IOException {
        return createQRCodeBase64(text, 200, 200);
    }

    /**
     * Creates a Base64 QR code representation with specified dimensions.
     *
     * @param text The text to encode in the QR code
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @return A Base64 string representing the QR code image
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static String createQRCodeBase64(String text, int width, int height) throws WriterException, IOException {
        byte[] qrCodeBytes = generateQRCodeImage(text, width, height);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrCodeBytes);
    }

    /**
     * Saves a QR code image to a file.
     *
     * @param text The text to encode in the QR code
     * @param filePath The path where the QR code image should be saved
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static void saveQRCodeImage(String text, String filePath, int width, int height) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
        Path path = FileSystems.getDefault().getPath(filePath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

        System.out.println("QR Code saved to: " + filePath);
    }

    /**
     * Generates a BufferedImage from a QR code.
     *
     * @param text The text to encode in the QR code
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @return A BufferedImage containing the QR code
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static BufferedImage generateQRCodeImage2(String text, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Saves a QR code to a file using the BufferedImage approach with default PNG format.
     *
     * @param text The text to encode in the QR code
     * @param filePath The path where the QR code image should be saved
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @return true if successful, false otherwise
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static boolean saveQRCodeToFile(String text, String filePath, int width, int height)
            throws WriterException, IOException {
        try {
            BufferedImage image = generateQRCodeImage2(text, width, height);
            File qrFile = new File(filePath);
            
            // Create parent directories if they don't exist
            File parentDir = qrFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            ImageIO.write(image, "PNG", qrFile);

            System.out.println("QR Code saved to file: " + filePath);
            return true;
        } catch (Exception e) {
            System.err.println("Error saving QR code to file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Saves a QR code to a file using the BufferedImage approach with specific format.
     *
     * @param text The text to encode in the QR code
     * @param filePath The path where the QR code image should be saved
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @param imageFormat The image format (e.g., "PNG", "JPEG")
     * @return true if successful, false otherwise
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static boolean saveQRCodeToFile(String text, String filePath, int width, int height, String imageFormat)
            throws WriterException, IOException {
        try {
            BufferedImage image = generateQRCodeImage2(text, width, height);
            File qrFile = new File(filePath);
            
            // Create parent directories if they don't exist
            File parentDir = qrFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            ImageIO.write(image, imageFormat, qrFile);

            System.out.println("QR Code saved to file: " + filePath);
            return true;
        } catch (Exception e) {
            System.err.println("Error saving QR code to file: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Generates a QR code and writes it directly to a FileOutputStream.
     *
     * @param text The text to encode in the QR code
     * @param outputStream The output stream to write the QR code to
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @throws WriterException If there is an error during QR code generation
     * @throws IOException If there is an I/O error
     */
    public static void generateQRCodeToStream(String text, FileOutputStream outputStream, int width, int height)
            throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        System.out.println("QR Code written to output stream");
    }
    
    /**
     * Creates a QR code content specifically for hotel bookings
     *
     * @param bookingReference The booking reference number
     * @param customerEmail Customer's email
     * @param checkInDate Check-in date
     * @param checkOutDate Check-out date
     * @param roomNumber Room number
     * @return Formatted QR code content string
     */
    public static String createBookingQRContent(String bookingReference, String customerEmail, 
                                              String checkInDate, String checkOutDate, String roomNumber) {
        return String.format(
            "Hotel Booking\nRef: %s\nEmail: %s\nCheck-in: %s\nCheck-out: %s\nRoom: %s\nVerify at: https://hotel.com/verify/%s",
            bookingReference, customerEmail, checkInDate, checkOutDate, roomNumber, bookingReference
        );
    }
}