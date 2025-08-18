package com.hotelreservationsystem.hotelreservationsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;
    
    @Column(name = "room_number", unique = true, nullable = false, length = 10)
    @NotBlank(message = "Room number is required")
    private String roomNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    @NotNull(message = "Room type is required")
    private RoomType roomType;
    
    @Column(name = "floor_number", nullable = false)
    @Min(value = 1, message = "Floor number must be at least 1")
    private Integer floorNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status = RoomStatus.AVAILABLE;
    
    @Column(name = "price_per_night", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price per night is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePerNight;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "amenities", columnDefinition = "JSON")
    private String amenities;
    
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // One-to-many relationship with Booking
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings;
    
    // Default constructor
    public Room() {}
    
    // Constructor
    public Room(String roomNumber, RoomType roomType, Integer floorNumber, BigDecimal pricePerNight) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.floorNumber = floorNumber;
        this.pricePerNight = pricePerNight;
    }
    
    // Getters and Setters
    public Long getRoomId() {
        return roomId;
    }
    
    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
    
    public String getRoomNumber() {
        return roomNumber;
    }
    
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
    
    public RoomType getRoomType() {
        return roomType;
    }
    
    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }
    
    public Integer getFloorNumber() {
        return floorNumber;
    }
    
    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }
    
    public RoomStatus getStatus() {
        return status;
    }
    
    public void setStatus(RoomStatus status) {
        this.status = status;
    }
    
    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }
    
    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getAmenities() {
        return amenities;
    }
    
    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<Booking> getBookings() {
        return bookings;
    }
    
    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }
    
    // Convenience methods for availability
    public boolean getIsAvailable() {
        return status == RoomStatus.AVAILABLE;
    }
    
    public void setIsAvailable(boolean isAvailable) {
        this.status = isAvailable ? RoomStatus.AVAILABLE : RoomStatus.OCCUPIED;
    }
    
    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", roomNumber='" + roomNumber + '\'' +
                ", floorNumber=" + floorNumber +
                ", status=" + status +
                ", pricePerNight=" + pricePerNight +
                '}';
    }
}