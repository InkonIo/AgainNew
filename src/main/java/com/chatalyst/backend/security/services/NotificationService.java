package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.Notification;
import com.chatalyst.backend.Entity.RoleName;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.NotificationRepository;
import com.chatalyst.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Create a notification for a specific user
     */
    @Transactional
    public Notification createNotificationForUser(Long userId, String type, String title, String message, Long messageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return createNotificationForUser(user, type, title, message, messageId, "medium");
    }

    /**
     * Create a notification for a specific user with priority
     */
    @Transactional
    public Notification createNotificationForUser(User user, String type, String title, String message, Long messageId, String priority) {
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .messageId(messageId)
                .user(user)
                .priority(priority)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", user.getId(), title);
        
        return savedNotification;
    }

    /**
     * Create notifications for all admin users
     */
    @Transactional
    public List<Notification> createNotificationForAdmins(String type, String title, String message, Long messageId) {
        return createNotificationForAdmins(type, title, message, messageId, "medium");
    }

    /**
     * Create notifications for all admin users with priority
     */
    @Transactional
    public List<Notification> createNotificationForAdmins(String type, String title, String message, Long messageId, String priority) {
        // Find all users with ADMIN role using a custom query
        List<User> admins = userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN))
                .collect(Collectors.toList());
        
        List<Notification> notifications = admins.stream()
                .map(admin -> createNotificationForUser(admin, type, title, message, messageId, priority))
                .collect(Collectors.toList());

        log.info("Notifications created for {} admins: {}", admins.size(), title);
        return notifications;
    }

    /**
     * Get all notifications for a user
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    /**
     * Get notifications for a user with limit
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(0, limit);
        return notificationRepository.findByUserWithLimit(user, pageable);
    }

    /**
     * Count unread notifications for a user
     */
    @Transactional(readOnly = true)
    public Long countUnreadNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * Mark a notification as read
     */
    @Transactional
    public void markNotificationAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify that the notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        if (!notification.getIsRead()) {
            notification.markAsRead();
            notificationRepository.save(notification);
            log.info("Notification {} marked as read by user {}", notificationId, userId);
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    @Transactional
    public void markAllNotificationsAsReadForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        notificationRepository.markAllAsReadForUser(user, LocalDateTime.now());
        log.info("All notifications marked as read for user {}", userId);
    }

    /**
     * Delete a notification
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Verify that the notification belongs to the user
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", notificationId, userId);
    }

    /**
     * Delete all notifications for a user
     */
    @Transactional
    public void deleteAllNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Notification> userNotifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notificationRepository.deleteAll(userNotifications);
        log.info("All notifications deleted for user {}", userId);
    }

    /**
     * Get recent notifications (last 7 days) for a user
     */
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return notificationRepository.findByUserAndCreatedAtAfter(user, weekAgo);
    }

    /**
     * Cleanup old notifications (older than 30 days)
     */
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        notificationRepository.deleteOldNotifications(cutoffDate);
        log.info("Old notifications cleaned up (older than {})", cutoffDate);
    }

    /**
     * Get notifications by type for a user
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByTypeForUser(Long userId, String type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return notificationRepository.findByUserAndTypeOrderByCreatedAtDesc(user, type);
    }

    /**
     * Get notifications related to a specific message
     */
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByMessageId(Long messageId) {
        return notificationRepository.findByMessageIdOrderByCreatedAtDesc(messageId);
    }
}

