package com.chatalyst.backend.Notifications.service;

import com.chatalyst.backend.Notifications.Entity.Notification;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Notifications.Repository.NotificationRepository;
import com.chatalyst.backend.Notifications.dto.NotificationDTO;
import com.chatalyst.backend.Repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void sendNotificationToUser(Long userId, String type, String title, String message, Long relatedId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("User not found: {}", userId);
                return;
            }

            String notificationId = UUID.randomUUID().toString();
            
            // Создаем данные уведомления
            Map<String, Object> data = new HashMap<>();
            if (relatedId != null) {
                data.put("messageId", relatedId);
                data.put("userId", userId);
                data.put("userName", user.getFirstName() + " " + user.getLastName());
            }

            // Сохраняем в базу данных
            Notification notification = new Notification();
            notification.setNotificationId(notificationId);
            notification.setUser(user);
            notification.setType(Notification.NotificationType.valueOf(type.toUpperCase()));
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setData(objectMapper.writeValueAsString(data));
            notification.setIsRead(false);
            
            notificationRepository.save(notification);

            // Отправляем через WebSocket
            NotificationDTO notificationDTO = new NotificationDTO(
                    notificationId, type, title, message, data, LocalDateTime.now()
            );

            messagingTemplate.convertAndSendToUser(
                    user.getEmail(), 
                    "/queue/notifications", 
                    notificationDTO
            );

            log.info("Notification sent to user {}: {}", userId, title);

        } catch (Exception e) {
            log.error("Error sending notification to user {}: {}", userId, e.getMessage());
        }
    }

    public void sendNotificationToAdmins(String type, String title, String message, Long relatedId) {
        try {
            String notificationId = UUID.randomUUID().toString();
            
            // Создаем данные уведомления
            Map<String, Object> data = new HashMap<>();
            data.put("priority", "HIGH");
            if (relatedId != null) {
                data.put("messageId", relatedId);
            }

            // Сохраняем в базу данных (для админов user = null)
            Notification notification = new Notification();
            notification.setNotificationId(notificationId);
            notification.setUser(null); // null означает уведомление для админов
            notification.setType(Notification.NotificationType.valueOf(type.toUpperCase()));
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setData(objectMapper.writeValueAsString(data));
            notification.setIsRead(false);
            
            notificationRepository.save(notification);

            // Отправляем через WebSocket всем админам
            NotificationDTO notificationDTO = new NotificationDTO(
                    notificationId, type, title, message, data, LocalDateTime.now()
            );

            messagingTemplate.convertAndSend("/topic/admin-notifications", notificationDTO);

            log.info("Notification sent to admins: {}", title);

        } catch (Exception e) {
            log.error("Error sending notification to admins: {}", e.getMessage());
        }
    }

    public List<NotificationDTO> getUserNotifications(Long userId, String lastId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }

        List<Notification> notifications;
        if (lastId != null && !lastId.isEmpty()) {
            notifications = notificationRepository.findByUserAndNotificationIdGreaterThanOrderByCreatedAtDesc(user, lastId);
        } else {
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        }

        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getAdminNotifications(String lastId) {
        List<Notification> notifications;
        if (lastId != null && !lastId.isEmpty()) {
            notifications = notificationRepository.findAdminNotificationsByNotificationIdGreaterThanOrderByCreatedAtDesc(lastId);
        } else {
            notifications = notificationRepository.findAdminNotificationsOrderByCreatedAtDesc();
        }

        return notifications.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private NotificationDTO convertToDTO(Notification notification) {
        try {
            Object data = null;
            if (notification.getData() != null) {
                data = objectMapper.readValue(notification.getData(), Object.class);
            }

            return new NotificationDTO(
                    notification.getNotificationId(),
                    notification.getType().name().toLowerCase(),
                    notification.getTitle(),
                    notification.getMessage(),
                    data,
                    notification.getCreatedAt()
            );
        } catch (JsonProcessingException e) {
            log.error("Error converting notification to DTO: {}", e.getMessage());
            return new NotificationDTO(
                    notification.getNotificationId(),
                    notification.getType().name().toLowerCase(),
                    notification.getTitle(),
                    notification.getMessage(),
                    null,
                    notification.getCreatedAt()
            );
        }
    }
}

