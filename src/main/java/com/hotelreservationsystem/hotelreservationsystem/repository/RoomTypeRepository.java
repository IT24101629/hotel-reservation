package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    
    // Find room type by name
    Optional<RoomType> findByTypeName(String typeName);
    
    // Check if room type name exists
    boolean existsByTypeName(String typeName);
    
    // Find room types by price range
    List<RoomType> findByBasePriceBetween(Double minPrice, Double maxPrice);
    
    // Find room types by max occupancy
    List<RoomType> findByMaxOccupancy(Integer maxOccupancy);
    
    // Find room types by max occupancy greater than or equal
    List<RoomType> findByMaxOccupancyGreaterThanEqual(Integer minOccupancy);
}