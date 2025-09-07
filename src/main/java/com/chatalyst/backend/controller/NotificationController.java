package com.chatalyst.backend.controller;

import com.chatalyst.backend.Entity.Notification;
import com.chatalyst.backend.security.services.UserPrincipal;
import com.chatalyst.backend.security.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for the authenticated user
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Notification>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "50") int limit) {
        
        List<Notification> notifications = notificationService.getNotificationsForUser(userPrincipal.getId(), limit);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for the authenticated user
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<Notification> notifications = notificationService.getUnreadNotificationsForUser(userPrincipal.getId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notification count for the authenticated user
     */
    @GetMapping("/unread/count")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long count = notificationService.countUnreadNotificationsForUser(userPrincipal.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get recent notifications (last 7 days) for the authenticated user
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        List<Notification> notifications = notificationService.getRecentNotificationsForUser(userPrincipal.getId());
        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notifications by type for the authenticated user
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Notification>> getNotificationsByType(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String type) {
        
        List<Notification> notifications = notificationService.getNotificationsByTypeForUser(userPrincipal.getId(), type);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Mark a notification as read
     */
    @PostMapping("/{notificationId}/read")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> markNotificationAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        
        notificationService.markNotificationAsRead(notificationId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all notifications as read for the authenticated user
     */
    @PostMapping("/mark-all-read")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> markAllNotificationsAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        notificationService.markAllNotificationsAsReadForUser(userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long notificationId) {
        
        notificationService.deleteNotification(notificationId, userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Delete all notifications for the authenticated user
     */
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAllNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        notificationService.deleteAllNotificationsForUser(userPrincipal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Get notifications related to a specific message (admin only)
     */
    @GetMapping("/message/{messageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Notification>> getNotificationsByMessageId(
            @PathVariable Long messageId) {
        
        List<Notification> notifications = notificationService.getNotificationsByMessageId(messageId);
        return ResponseEntity.ok(notifications);
    }
}

