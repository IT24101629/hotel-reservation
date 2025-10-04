package com.hotelreservationsystem.hotelreservationsystem.config;

import com.hotelreservationsystem.hotelreservationsystem.model.*;
import com.hotelreservationsystem.hotelreservationsystem.repository.*;
import com.hotelreservationsystem.hotelreservationsystem.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeData();
    }

    private void initializeData() {
        try {
            // Create demo users
            userService.createDemoUsersIfNotExist();

            // Create room types if they don't exist
            createRoomTypesIfNotExist();

            // Create rooms if they don't exist
            createRoomsIfNotExist();

            System.out.println("Demo data initialized successfully!");
        } catch (Exception e) {
            System.err.println("Error initializing demo data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createRoomTypesIfNotExist() {
        if (roomTypeRepository.count() == 0) {
            // Standard Single
            RoomType standardSingle = new RoomType();
            standardSingle.setTypeName("Standard Single");
            standardSingle.setDescription("Perfect for solo travelers seeking comfort and convenience with modern amenities.");
            standardSingle.setBasePrice(new BigDecimal("8500.00"));
            standardSingle.setMaxOccupancy(1);
            standardSingle.setAmenities("[\"WiFi\", \"AC\", \"TV\", \"Private Bathroom\"]");
            standardSingle.setCreatedAt(LocalDateTime.now());
            standardSingle.setUpdatedAt(LocalDateTime.now());
            roomTypeRepository.save(standardSingle);

            // Standard Double
            RoomType standardDouble = new RoomType();
            standardDouble.setTypeName("Standard Double");
            standardDouble.setDescription("Spacious accommodation ideal for couples and business travelers with enhanced amenities.");
            standardDouble.setBasePrice(new BigDecimal("12000.00"));
            standardDouble.setMaxOccupancy(2);
            standardDouble.setAmenities("[\"WiFi\", \"AC\", \"TV\", \"Mini Fridge\", \"Private Bathroom\"]");
            standardDouble.setCreatedAt(LocalDateTime.now());
            standardDouble.setUpdatedAt(LocalDateTime.now());
            roomTypeRepository.save(standardDouble);

            // Deluxe Room
            RoomType deluxeRoom = new RoomType();
            deluxeRoom.setTypeName("Deluxe Room");
            deluxeRoom.setDescription("Luxury accommodation with premium amenities and stunning city or garden views.");
            deluxeRoom.setBasePrice(new BigDecimal("18000.00"));
            deluxeRoom.setMaxOccupancy(3);
            deluxeRoom.setAmenities("[\"WiFi\", \"AC\", \"TV\", \"Mini Fridge\", \"Balcony\", \"Room Service\"]");
            deluxeRoom.setCreatedAt(LocalDateTime.now());
            deluxeRoom.setUpdatedAt(LocalDateTime.now());
            roomTypeRepository.save(deluxeRoom);

            // Family Suite
            RoomType familySuite = new RoomType();
            familySuite.setTypeName("Family Suite");
            familySuite.setDescription("Spacious suite perfect for families with separate living area and enhanced amenities.");
            familySuite.setBasePrice(new BigDecimal("25000.00"));
            familySuite.setMaxOccupancy(4);
            familySuite.setAmenities("[\"WiFi\", \"AC\", \"TV\", \"Mini Fridge\", \"Balcony\", \"Room Service\", \"Living Area\"]");
            familySuite.setCreatedAt(LocalDateTime.now());
            familySuite.setUpdatedAt(LocalDateTime.now());
            roomTypeRepository.save(familySuite);

            // Presidential Suite
            RoomType presidentialSuite = new RoomType();
            presidentialSuite.setTypeName("Presidential Suite");
            presidentialSuite.setDescription("Ultimate luxury accommodation with panoramic views, premium services, and exclusive amenities.");
            presidentialSuite.setBasePrice(new BigDecimal("45000.00"));
            presidentialSuite.setMaxOccupancy(6);
            presidentialSuite.setAmenities("[\"WiFi\", \"AC\", \"TV\", \"Mini Fridge\", \"Balcony\", \"Room Service\", \"Jacuzzi\", \"Butler Service\"]");
            presidentialSuite.setCreatedAt(LocalDateTime.now());
            presidentialSuite.setUpdatedAt(LocalDateTime.now());
            roomTypeRepository.save(presidentialSuite);

            System.out.println("Room types created successfully!");
        }
    }

    private void createRoomsIfNotExist() {
        if (roomRepository.count() == 0) {
            // Get room types
            RoomType standardSingle = roomTypeRepository.findByTypeName("Standard Single").orElse(null);
            RoomType standardDouble = roomTypeRepository.findByTypeName("Standard Double").orElse(null);
            RoomType deluxeRoom = roomTypeRepository.findByTypeName("Deluxe Room").orElse(null);
            RoomType familySuite = roomTypeRepository.findByTypeName("Family Suite").orElse(null);
            RoomType presidentialSuite = roomTypeRepository.findByTypeName("Presidential Suite").orElse(null);

            if (standardSingle != null) {
                // Standard Single Rooms
                createRoom("101", standardSingle, 1, new BigDecimal("8500.00"), "Cozy single room with modern amenities");
                createRoom("102", standardSingle, 1, new BigDecimal("8500.00"), "Comfortable single room with garden view");
            }

            if (standardDouble != null) {
                // Standard Double Rooms
                createRoom("201", standardDouble, 2, new BigDecimal("12000.00"), "Spacious double room with city view");
                createRoom("202", standardDouble, 2, new BigDecimal("12000.00"), "Modern double room with balcony");
            }

            if (deluxeRoom != null) {
                // Deluxe Rooms
                createRoom("301", deluxeRoom, 3, new BigDecimal("18000.00"), "Luxury deluxe room with premium amenities");
                createRoom("302", deluxeRoom, 3, new BigDecimal("18000.00"), "Deluxe room with stunning city view");
            }

            if (familySuite != null) {
                // Family Suite
                createRoom("401", familySuite, 4, new BigDecimal("25000.00"), "Family suite with separate living area");
            }

            if (presidentialSuite != null) {
                // Presidential Suite
                createRoom("501", presidentialSuite, 5, new BigDecimal("45000.00"), "Presidential suite with panoramic views");
            }

            System.out.println("Rooms created successfully!");
        }
    }

    private void createRoom(String roomNumber, RoomType roomType, int floor, BigDecimal price, String description) {
        Room room = new Room();
        room.setRoomNumber(roomNumber);
        room.setRoomType(roomType);
        room.setFloorNumber(floor);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setPricePerNight(price);
        room.setDescription(description);
        room.setAmenities(roomType.getAmenities());
        room.setImageUrl("/images/room-default.jpg");
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);
    }
}