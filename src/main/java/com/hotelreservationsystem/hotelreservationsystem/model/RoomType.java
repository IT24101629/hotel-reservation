package com.hotelreservationsystem.hotelreservationsystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "room_types")
public class RoomType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long typeId;
    
    @Column(name = "type_name", unique = true, nullable = false, length = 50)
    @NotBlank(message = "Room type name is required")
    private String typeName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    private BigDecimal basePrice;
    
    @Column(name = "max_occupancy", nullable = false)
    @Min(value = 1, message = "Maximum occupancy must be at least 1")
    private Integer maxOccupancy;
    
    @Column(name = "amenities", columnDefinition = "JSON")
    private String amenities;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // One-to-many relationship with Room
    @OneToMany(mappedBy = "roomType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Room> rooms;
    
    // Default constructor
    public RoomType() {}
    
    // Constructor
    public RoomType(String typeName, String description, BigDecimal basePrice, Integer maxOccupancy) {
        this.typeName = typeName;
        this.description = description;
        this.basePrice = basePrice;
        this.maxOccupancy = maxOccupancy;
    }
    
    // Getters and Setters
    public Long getTypeId() {
        return typeId;
    }
    
    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getBasePrice() {
        return basePrice;
    }
    
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }
    
    public Integer getMaxOccupancy() {
        return maxOccupancy;
    }
    
    public void setMaxOccupancy(Integer maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }
    
    public String getAmenities() {
        return amenities;
    }
    
    public void setAmenities(String amenities) {
        this.amenities = amenities;
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
    
    public List<Room> getRooms() {
        return rooms;
    }
    
    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }
    
    @Override
    public String toString() {
        return "RoomType{" +
                "typeId=" + typeId +
                ", typeName='" + typeName + '\'' +
                ", basePrice=" + basePrice +
                ", maxOccupancy=" + maxOccupancy +
                '}';
    }
}