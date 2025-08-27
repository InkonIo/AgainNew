package com.chatalyst.backend.Notifications.Repository;

import com.chatalyst.backend.Notifications.Entity.Notification;
import com.chatalyst.backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.notificationId > :lastId ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndNotificationIdGreaterThanOrderByCreatedAtDesc(
        @Param("user") User user, 
        @Param("lastId") String lastId
    );
    
    @Query("SELECT n FROM Notification n WHERE n.user IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findAdminNotificationsOrderByCreatedAtDesc();
    
    @Query("SELECT n FROM Notification n WHERE n.user IS NULL AND n.notificationId > :lastId ORDER BY n.createdAt DESC")
    List<Notification> findAdminNotificationsByNotificationIdGreaterThanOrderByCreatedAtDesc(
        @Param("lastId") String lastId
    );
}

