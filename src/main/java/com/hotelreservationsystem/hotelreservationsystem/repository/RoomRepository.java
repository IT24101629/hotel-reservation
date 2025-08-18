package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.Room;
import com.hotelreservationsystem.hotelreservationsystem.model.RoomStatus;
import com.hotelreservationsystem.hotelreservationsystem.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    
    // Find rooms by room type
    List<Room> findByRoomType(RoomType roomType);
    
    // Find rooms by room type ID
    List<Room> findByRoomType_TypeId(Long typeId);
    
    // Find room by room number
    Optional<Room> findByRoomNumber(String roomNumber);
    
    // Find available rooms
    List<Room> findByStatus(RoomStatus status);
    
    // Find rooms by room type and availability
    List<Room> findByRoomType_TypeIdAndStatus(Long typeId, RoomStatus status);
    
    // Check if room number exists
    boolean existsByRoomNumber(String roomNumber);
    
    // Find available rooms for specific dates (custom query)
    @Query("SELECT DISTINCT r FROM Room r " +
           "WHERE r.status = 'AVAILABLE' " +
           "AND r.roomId NOT IN (" +
           "    SELECT b.room.roomId FROM Booking b " +
           "    WHERE b.bookingStatus IN ('CONFIRMED', 'CHECKED_IN') " +
           "    AND NOT (b.checkOutDate <= :checkIn OR b.checkInDate >= :checkOut)" +
           ")")
    List<Room> findAvailableRoomsForDates(@Param("checkIn") LocalDate checkIn, 
                                         @Param("checkOut") LocalDate checkOut);
    
    // Find available rooms by room type for specific dates
    @Query("SELECT DISTINCT r FROM Room r " +
           "WHERE r.status = 'AVAILABLE' " +
           "AND r.roomType.typeId = :typeId " +
           "AND r.roomId NOT IN (" +
           "    SELECT b.room.roomId FROM Booking b " +
           "    WHERE b.bookingStatus IN ('CONFIRMED', 'CHECKED_IN') " +
           "    AND NOT (b.checkOutDate <= :checkIn OR b.checkInDate >= :checkOut)" +
           ")")
    List<Room> findAvailableRoomsByTypeForDates(@Param("typeId") Long typeId,
                                               @Param("checkIn") LocalDate checkIn, 
                                               @Param("checkOut") LocalDate checkOut);
    
    // Count available rooms
    long countByStatus(RoomStatus status);
    
    // Count rooms by type
    long countByRoomType_TypeId(Long typeId);
    
    // Find rooms with price range
    @Query("SELECT r FROM Room r WHERE r.roomType.basePrice BETWEEN :minPrice AND :maxPrice")
    List<Room> findRoomsInPriceRange(@Param("minPrice") Double minPrice, 
                                    @Param("maxPrice") Double maxPrice);
}