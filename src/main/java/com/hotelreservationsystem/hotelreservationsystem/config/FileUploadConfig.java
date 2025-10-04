package com.hotelreservationsystem.hotelreservationsystem.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig implements WebMvcConfigurer {

    private String promotionImagePath = "uploads/promotions/";
    private String roomImagePath = "uploads/rooms/";
    private long maxFileSize = 5 * 1024 * 1024; // 5MB
    private long maxRequestSize = 6 * 1024 * 1024; // 6MB

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Get absolute path for promotion images
        String promotionUploadPath = Paths.get(promotionImagePath).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/images/promotions/**")
                .addResourceLocations(promotionUploadPath);
    }

    // Getters and Setters
    public String getPromotionImagePath() {
        return promotionImagePath;
    }

    public void setPromotionImagePath(String promotionImagePath) {
        this.promotionImagePath = promotionImagePath;
    }

    public String getRoomImagePath() {
        return roomImagePath;
    }

    public void setRoomImagePath(String roomImagePath) {
        this.roomImagePath = roomImagePath;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }
}