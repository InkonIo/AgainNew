package com.chatalyst.backend.Notifications.Controller;

import com.chatalyst.backend.Notifications.dto.NotificationDTO;
import com.chatalyst.backend.security.services.UserPrincipal;
import com.chatalyst.backend.Notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/poll")
    public ResponseEntity<Map<String, Object>> pollNotifications(
            @RequestParam(required = false) String lastId,
            @RequestParam(defaultValue = "30") int timeout,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // Для простоты реализации, возвращаем уведомления сразу без long polling
        List<NotificationDTO> notifications;
        
        if (userPrincipal != null) {
            // Проверяем, является ли пользователь админом
            boolean isAdmin = userPrincipal.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
            
            if (isAdmin) {
                notifications = notificationService.getAdminNotifications(lastId);
            } else {
                notifications = notificationService.getUserNotifications(userPrincipal.getId(), lastId);
            }
        } else {
            notifications = List.of();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", notifications);
        
        if (!notifications.isEmpty()) {
            response.put("lastId", notifications.get(0).getId());
        } else {
            response.put("lastId", lastId);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testNotification(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        String type = request.getOrDefault("type", "system");
        String title = request.getOrDefault("title", "Тестовое уведомление");
        String message = request.getOrDefault("message", "Это тестовое уведомление для проверки системы");

        if (userPrincipal != null) {
            notificationService.sendNotificationToUser(userPrincipal.getId(), type, title, message, null);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Test notification sent");

        return ResponseEntity.ok(response);
    }
}

