package com.chatalyst.backend.Support.service;

import com.chatalyst.backend.Entity.*;
import com.chatalyst.backend.Repository.*;
import com.chatalyst.backend.Support.Entity.MessagePriority;
import com.chatalyst.backend.Support.Entity.MessageStatus;
import com.chatalyst.backend.Support.Entity.SupportMessage;
import com.chatalyst.backend.Support.Entity.SupportMessageReply;
import com.chatalyst.backend.Support.Repository.SupportMessageReplyRepository;
import com.chatalyst.backend.Support.Repository.SupportMessageRepository;
import com.chatalyst.backend.Support.dto.CreateReplyRequest;
import com.chatalyst.backend.Support.dto.CreateSupportMessageRequest;
import com.chatalyst.backend.Support.dto.MessageDetailResponse;
import com.chatalyst.backend.Support.dto.SupportMessageReplyResponse;
import com.chatalyst.backend.Support.dto.SupportMessageResponse;
import com.chatalyst.backend.Support.dto.UpdateMessageStatusRequest;
import com.chatalyst.backend.Support.dto.UpdateReplyRequest;
import com.chatalyst.backend.Support.dto.UpdateSupportMessageRequest;
import com.chatalyst.backend.Support.dto.SupportStatsResponse;
import com.chatalyst.backend.dto.*;
import com.chatalyst.backend.security.services.NotificationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SupportMessageService {

    private final SupportMessageRepository supportMessageRepository;
    private final SupportMessageReplyRepository supportMessageReplyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @PersistenceContext
    private EntityManager em;

    public SupportMessageService(SupportMessageRepository supportMessageRepository, 
                               SupportMessageReplyRepository supportMessageReplyRepository, 
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.supportMessageRepository = supportMessageRepository;
        this.supportMessageReplyRepository = supportMessageReplyRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SupportMessageResponse createMessage(Long userId, CreateSupportMessageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (isAdmin) {
            throw new RuntimeException("Admins cannot create new support messages.");
        }

        SupportMessage message = new SupportMessage();
        message.setUser(user);
        message.setSubject(request.getSubject());
        message.setMessage(request.getMessage());
        message.setPriority(request.getPriority());
        message.setStatus(MessageStatus.OPEN);

        SupportMessage savedMessage = supportMessageRepository.save(message);
        log.info("Support message created by user {}: {}", userId, savedMessage.getId());

        // Save notification to database instead of sending via WebSocket
        notificationService.createNotificationForAdmins(
            "new_message", 
            "Новое сообщение поддержки", 
            String.format("Пользователь %s %s создал новое сообщение: %s", 
                user.getFirstName(), user.getLastName(), savedMessage.getSubject()),
            savedMessage.getId()
        );

        return SupportMessageResponse.fromEntity(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getUserMessages(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (isAdmin) {
            // Admins should not use this endpoint to get their own messages, they see all messages
            return List.of(); // Return empty list for admins
        }

        return supportMessageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SupportMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getAllMessages() {
        return supportMessageRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SupportMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getMessagesWithFilters(
            MessageStatus status,
            MessagePriority priority,
            Long adminId,
            String search,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String sortBy,
            String sortDirection) {

        return supportMessageRepository.findWithAdvancedFilters(
                        status,
                        priority,
                        adminId,
                        search,
                        dateFrom,
                        dateTo,
                        sortBy,
                        sortDirection)
                .stream()
                .map(SupportMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getMessagesWithAdvancedFilters(
            MessageStatus status, MessagePriority priority, Long adminId,
            String search, String sortBy, String sortDirection,
            String dateFrom, String dateTo) {

        // Приведение search к строке и обрезка пробелов
        String safeSearch = (search == null || search.isBlank()) ? null : "%" + search.toLowerCase() + "%";

        List<SupportMessage> messages = supportMessageRepository.findWithAdvancedFilters(
                status,
                priority,
                adminId,
                safeSearch,
                parseDate(dateFrom),
                parseDate(dateTo),
                sortBy,
                sortDirection
        );

        return messages.stream()
                .map(SupportMessageResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MessageDetailResponse getMessageDetail(Long messageId, Long requesterId) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Проверяем права доступа - пользователь может видеть только свои сообщения, админы - все
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = requester.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin && !message.getUser().getId().equals(requesterId)) {
            throw new RuntimeException("Access denied");
        }

        return MessageDetailResponse.fromEntity(message);
    }

    @Transactional
    public SupportMessageReplyResponse addReply(Long messageId, Long senderId, CreateReplyRequest request) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверяем права доступа
        boolean isAdmin = sender.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin && !message.getUser().getId().equals(senderId)) {
            throw new RuntimeException("Access denied");
        }

        // Admins cannot reply to their own messages
        if (isAdmin && message.getUser().getId().equals(senderId)) {
            throw new RuntimeException("Admins cannot reply to their own support messages.");
        }

        SupportMessageReply reply = new SupportMessageReply();
        reply.setMessage(message);
        reply.setSender(sender);
        reply.setReplyText(request.getReplyText());
        reply.setIsAdminReply(isAdmin);

        SupportMessageReply savedReply = supportMessageReplyRepository.save(reply);
        log.info("Reply added to message {} by user {}", messageId, senderId);

        // Save notification to database instead of sending via WebSocket
        if (isAdmin) {
            // Admin replied - notify the user who created the message
            notificationService.createNotificationForUser(
                message.getUser().getId(), 
                "admin_reply",
                "Новый ответ от администратора",
                String.format("Администратор ответил на ваше сообщение: %s", message.getSubject()),
                messageId
            );
        } else {
            // User replied - notify all admins
            notificationService.createNotificationForAdmins(
                "user_reply",
                "Новый ответ пользователя",
                String.format("Пользователь %s %s ответил на сообщение: %s", 
                    sender.getFirstName(), sender.getLastName(), message.getSubject()),
                messageId
            );
        }

        return SupportMessageReplyResponse.fromEntity(savedReply);
    }

    @Transactional
    public SupportMessageResponse updateMessageStatus(Long messageId, Long adminId, UpdateMessageStatusRequest request) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MessageStatus oldStatus = message.getStatus();
        message.setStatus(request.getStatus());
        SupportMessage savedMessage = supportMessageRepository.save(message);
        log.info("Message {} status updated to {} by admin {}", messageId, request.getStatus(), adminId);

        // Notify user about status change
        String statusText = getStatusText(request.getStatus());
        notificationService.createNotificationForUser(
            message.getUser().getId(), 
            "status_update",
            "Статус сообщения изменен",
            String.format("Статус вашего сообщения '%s' изменен на: %s", message.getSubject(), statusText),
            messageId
        );

        return SupportMessageResponse.fromEntity(savedMessage);
    }

    @Transactional
    public SupportMessageResponse assignMessageToAdmin(Long messageId, Long adminId) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        message.setAdmin(admin);
        if (message.getStatus() == MessageStatus.OPEN) {
            message.setStatus(MessageStatus.IN_PROGRESS);
        }

        SupportMessage savedMessage = supportMessageRepository.save(message);
        log.info("Message {} assigned to admin {}", messageId, adminId);

        // Notify user about assignment
        notificationService.createNotificationForUser(
            message.getUser().getId(), 
            "message_assigned",
            "Сообщение назначено администратору",
            String.format("Ваше сообщение '%s' назначено администратору %s %s", 
                message.getSubject(), admin.getFirstName(), admin.getLastName()),
            messageId
        );

        return SupportMessageResponse.fromEntity(savedMessage);
    }

    @Transactional
    public SupportMessageResponse updateMessage(Long messageId, Long adminId, UpdateSupportMessageRequest request) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getSubject() != null) {
            message.setSubject(request.getSubject());
        }
        if (request.getMessage() != null) {
            message.setMessage(request.getMessage());
        }
        if (request.getStatus() != null) {
            message.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            message.setPriority(request.getPriority());
        }

        SupportMessage savedMessage = supportMessageRepository.save(message);
        log.info("Message {} updated by admin {}", messageId, adminId);

        return SupportMessageResponse.fromEntity(savedMessage);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long adminId) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete all replies first
        supportMessageReplyRepository.deleteByMessageId(messageId);
        // Then delete the message
        supportMessageRepository.delete(message);
        log.info("Message {} deleted by admin {}", messageId, adminId);
    }

    @Transactional
    public void deleteReply(Long replyId, Long adminId) {
        SupportMessageReply reply = supportMessageReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        supportMessageReplyRepository.delete(reply);
        log.info("Reply {} deleted by admin {}", replyId, adminId);
    }

    @Transactional
    public SupportMessageReplyResponse updateReply(Long replyId, Long adminId, UpdateReplyRequest request) {
        SupportMessageReply reply = supportMessageReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        reply.setReplyText(request.getReplyText());
        SupportMessageReply savedReply = supportMessageReplyRepository.save(reply);
        log.info("Reply {} updated by admin {}", replyId, adminId);

        return SupportMessageReplyResponse.fromEntity(savedReply);
    }

    @Transactional(readOnly = true)
    public SupportStatsResponse getSupportStats(String from, String to) {
        LocalDate today = LocalDate.now();
        LocalDateTime weekAgo = today.minusDays(7).atStartOfDay();

        // --- Total messages ---
        Long totalMessages = supportMessageRepository.count();

        // --- Messages by status ---
        List<SupportMessageRepository.StatusPriorityCount> statusPriorityCounts = 
            supportMessageRepository.countByStatusAndPriority();

        Long openMessages = statusPriorityCounts.stream()
                .filter(c -> c.getStatus() == MessageStatus.OPEN)
                .mapToLong(SupportMessageRepository.StatusPriorityCount::getCount)
                .sum();
        Long inProgressMessages = statusPriorityCounts.stream()
                .filter(c -> c.getStatus() == MessageStatus.IN_PROGRESS)
                .mapToLong(SupportMessageRepository.StatusPriorityCount::getCount)
                .sum();
        Long closedMessages = statusPriorityCounts.stream()
                .filter(c -> c.getStatus() == MessageStatus.CLOSED)
                .mapToLong(SupportMessageRepository.StatusPriorityCount::getCount)
                .sum();

        // --- Messages by priority ---
        Long highPriorityMessages = statusPriorityCounts.stream()
                .filter(c -> c.getPriority() == MessagePriority.HIGH)
                .mapToLong(SupportMessageRepository.StatusPriorityCount::getCount)
                .sum();
        Long mediumPriorityMessages = statusPriorityCounts.stream()
                .filter(c -> c.getPriority() == MessagePriority.MEDIUM)
                .mapToLong(SupportMessageRepository.StatusPriorityCount::getCount)
                .sum();
        Long lowPriorityMessages = statusPriorityCounts.stream()
                .filter(c -> c.getPriority() == MessagePriority.LOW)
                .mapToLong(SupportMessageRepository.StatusPriorityCount::getCount)
                .sum();

        // --- Recent activity ---
        Long recentMessages = supportMessageRepository.countByCreatedAtAfter(weekAgo);
        Long recentReplies = supportMessageReplyRepository.countByCreatedAtAfter(weekAgo);

        // --- Average response time (simplified calculation) ---
        Double avgResponseTime = supportMessageReplyRepository.findAverageResponseTimeInHours();
        if (avgResponseTime == null) {
            avgResponseTime = 0.0;
        }

        // --- Messages by date (last 7 days) ---
        Map<String, Long> messagesByDate = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            
            Long count = supportMessageRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            messagesByDate.put(date.format(DateTimeFormatter.ISO_LOCAL_DATE), count);
        }

        return SupportStatsResponse.builder()
                .totalMessages(totalMessages)
                .openMessages(openMessages)
                .inProgressMessages(inProgressMessages)
                .closedMessages(closedMessages)
                .highPriorityMessages(highPriorityMessages)
                .mediumPriorityMessages(mediumPriorityMessages)
                .lowPriorityMessages(lowPriorityMessages)
                .recentMessages(recentMessages)
                .recentReplies(recentReplies)
                .averageResponseTimeHours(avgResponseTime)
                .messagesByDate(messagesByDate)
                .build();
    }

    // Helper methods

    private String getStatusText(MessageStatus status) {
        switch (status) {
            case OPEN: return "Открыто";
            case IN_PROGRESS: return "В работе";
            case CLOSED: return "Закрыто";
            default: return status.toString();
        }
    }

    private LocalDateTime parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateString);
            return null;
        }
    }
}

