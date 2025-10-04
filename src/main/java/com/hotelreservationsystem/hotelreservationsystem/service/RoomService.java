package com.hotelreservationsystem.hotelreservationsystem.service;

import com.hotelreservationsystem.hotelreservationsystem.model.Room;
import com.hotelreservationsystem.hotelreservationsystem.model.RoomStatus;
import com.hotelreservationsystem.hotelreservationsystem.model.RoomType;
import com.hotelreservationsystem.hotelreservationsystem.repository.RoomRepository;
import com.hotelreservationsystem.hotelreservationsystem.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAllAvailableRooms() {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    public List<Room> findAvailableRoomsForDates(String checkInStr, String checkOutStr, Integer guests, String roomTypeName) {
        try {
            LocalDate checkIn = LocalDate.parse(checkInStr);
            LocalDate checkOut = LocalDate.parse(checkOutStr);

            List<Room> availableRooms = roomRepository.findAvailableRoomsForDates(checkIn, checkOut);

            // Filter by guests if specified
            if (guests != null) {
                availableRooms = availableRooms.stream()
                        .filter(room -> room.getRoomType().getMaxOccupancy() >= guests)
                        .collect(Collectors.toList());
            }

            // Filter by room type if specified
            if (roomTypeName != null && !roomTypeName.isEmpty()) {
                availableRooms = availableRooms.stream()
                        .filter(room -> room.getRoomType().getTypeName().equalsIgnoreCase(roomTypeName))
                        .collect(Collectors.toList());
            }

            return availableRooms;
        } catch (Exception e) {
            // If parsing fails, return all available rooms
            return getAllAvailableRooms();
        }
    }

    public Room findById(Long id) {
        return roomRepository.findById(id).orElse(null);
    }

    public Room findByRoomNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber).orElse(null);
    }

    public List<Room> findByRoomType(Long typeId) {
        return roomRepository.findByRoomType_TypeId(typeId);
    }

    public List<Room> findAvailableRoomsByType(Long typeId) {
        return roomRepository.findByRoomType_TypeIdAndStatus(typeId, RoomStatus.AVAILABLE);
    }

    public Room save(Room room) {
        return roomRepository.save(room);
    }

    public Room updateRoom(Room room) {
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    public long getTotalRoomsCount() {
        return roomRepository.count();
    }

    public long getAvailableRoomsCount() {
        return roomRepository.countByStatus(RoomStatus.AVAILABLE);
    }

    public boolean isRoomNumberExists(String roomNumber) {
        return roomRepository.existsByRoomNumber(roomNumber);
    }

    public void updateRoomAvailability(Long roomId, boolean isAvailable) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        room.setStatus(isAvailable ? RoomStatus.AVAILABLE : RoomStatus.OCCUPIED);
        roomRepository.save(room);
    }

    // Room Type methods
    public List<RoomType> getAllRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public RoomType findRoomTypeById(Long id) {
        return roomTypeRepository.findById(id).orElse(null);
    }

    public RoomType findRoomTypeByName(String typeName) {
        return roomTypeRepository.findByTypeName(typeName).orElse(null);
    }

    public RoomType saveRoomType(RoomType roomType) {
        return roomTypeRepository.save(roomType);
    }

    public void deleteRoomType(Long id) {
        roomTypeRepository.deleteById(id);
    }

    public long getRoomCountByType(Long typeId) {
        return roomRepository.countByRoomType_TypeId(typeId);
    }

    public List<Room> findRoomsInPriceRange(Double minPrice, Double maxPrice) {
        return roomRepository.findRoomsInPriceRange(minPrice, maxPrice);
    }

    // Admin-specific methods
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public Room saveOrUpdateRoom(Room room) {
        return roomRepository.save(room);
    }

    public void deleteRoomById(Long id) {
        roomRepository.deleteById(id);
    }
}