package com.hotelreservationsystem.hotelreservationsystem.dto;

import java.util.List;
import java.util.Map;

public class ChatResponseDTO {
    private String sessionId;
    private String response;
    private String intent;
    private Map<String, Object> entities;
    private String bookingStatus;
    private List<Map<String, Object>> suggestedRooms;
    private boolean bookingReady;
    private boolean needsBookingDetails;

    // Default constructor
    public ChatResponseDTO() {}

    // Constructor with parameters
    public ChatResponseDTO(String sessionId, String response, String intent, Map<String, Object> entities) {
        this.sessionId = sessionId;
        this.response = response;
        this.intent = intent;
        this.entities = entities;
        this.bookingReady = false;
        this.needsBookingDetails = false;
    }

    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public Map<String, Object> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, Object> entities) {
        this.entities = entities;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public List<Map<String, Object>> getSuggestedRooms() {
        return suggestedRooms;
    }

    public void setSuggestedRooms(List<Map<String, Object>> suggestedRooms) {
        this.suggestedRooms = suggestedRooms;
    }

    public boolean isBookingReady() {
        return bookingReady;
    }

    public void setBookingReady(boolean bookingReady) {
        this.bookingReady = bookingReady;
    }

    public boolean isNeedsBookingDetails() {
        return needsBookingDetails;
    }

    public void setNeedsBookingDetails(boolean needsBookingDetails) {
        this.needsBookingDetails = needsBookingDetails;
    }

    @Override
    public String toString() {
        return "ChatResponseDTO{" +
                "sessionId='" + sessionId + '\'' +
                ", response='" + response + '\'' +
                ", intent='" + intent + '\'' +
                ", bookingStatus='" + bookingStatus + '\'' +
                ", bookingReady=" + bookingReady +
                ", needsBookingDetails=" + needsBookingDetails +
                '}';
    }
}