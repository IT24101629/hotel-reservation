package com.hotelreservationsystem.hotelreservationsystem.dto;

public class ChatMessageDTO {
    private String sessionId;
    private String message;
    private Long userId;

    // Default constructor
    public ChatMessageDTO() {}

    // Constructor with parameters
    public ChatMessageDTO(String sessionId, String message, Long userId) {
        this.sessionId = sessionId;
        this.message = message;
        this.userId = userId;
    }

    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "ChatMessageDTO{" +
                "sessionId='" + sessionId + '\'' +
                ", message='" + message + '\'' +
                ", userId=" + userId +
                '}';
    }
}