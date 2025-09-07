package com.chatalyst.backend.Repository;

import com.chatalyst.backend.Entity.Notification;
import com.chatalyst.backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find notifications for a specific user, ordered by creation date (newest first)
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // Find unread notifications for a specific user
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);

    // Count unread notifications for a specific user
    Long countByUserAndIsReadFalse(User user);

    // Find notifications for a specific user with pagination
    @Query("SELECT n FROM Notification n WHERE n.user = :user ORDER BY n.createdAt DESC")
    List<Notification> findByUserWithLimit(@Param("user") User user, org.springframework.data.domain.Pageable pageable);

    // Find recent notifications (last N days) for a user
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndCreatedAtAfter(@Param("user") User user, @Param("since") LocalDateTime since);

    // Mark all notifications as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt WHERE n.user = :user AND n.isRead = false")
    void markAllAsReadForUser(@Param("user") User user, @Param("readAt") LocalDateTime readAt);

    // Delete old notifications (older than specified date)
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Find notifications by type for a user
    List<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, String type);

    // Find notifications related to a specific message
    List<Notification> findByMessageIdOrderByCreatedAtDesc(Long messageId);

    // Get notification statistics for a user
    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.user = :user GROUP BY n.type")
    List<Object[]> getNotificationStatsByUser(@Param("user") User user);
}

