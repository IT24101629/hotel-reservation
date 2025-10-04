package com.hotelreservationsystem.hotelreservationsystem.repository;

import com.hotelreservationsystem.hotelreservationsystem.model.Notification;
import com.hotelreservationsystem.hotelreservationsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    List<Notification> findByRecipientAndIsReadOrderByCreatedAtDesc(User recipient, boolean isRead);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipient = :user AND n.isRead = false")
    long countUnreadNotifications(@Param("user") User user);

    @Query("SELECT n FROM Notification n WHERE n.recipient = :user ORDER BY n.createdAt DESC")
    List<Notification> findNotificationsByUser(@Param("user") User user);
}