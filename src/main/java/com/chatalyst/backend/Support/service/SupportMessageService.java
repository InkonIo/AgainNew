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

    @PersistenceContext
    private EntityManager em;

    public SupportMessageService(SupportMessageRepository supportMessageRepository, SupportMessageReplyRepository supportMessageReplyRepository, UserRepository userRepository) {
        this.supportMessageRepository = supportMessageRepository;
        this.supportMessageReplyRepository = supportMessageReplyRepository;
        this.userRepository = userRepository;
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

        return SupportMessageReplyResponse.fromEntity(savedReply);
    }

    @Transactional
    public SupportMessageResponse updateMessageStatus(Long messageId, Long adminId, UpdateMessageStatusRequest request) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin) {
            throw new RuntimeException("Only admins can update message status");
        }

        message.setStatus(request.getStatus());
        SupportMessage savedMessage = supportMessageRepository.save(message);
        log.info("Message {} status updated to {} by admin {}", messageId, request.getStatus(), adminId);

        return SupportMessageResponse.fromEntity(savedMessage);
    }

    @Transactional
    public SupportMessageResponse assignMessageToAdmin(Long messageId, Long adminId) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin) {
            throw new RuntimeException("Only admins can assign messages");
        }

        message.setAdmin(admin);
        if (message.getStatus() == MessageStatus.OPEN) {
            message.setStatus(MessageStatus.IN_PROGRESS);
        }

        SupportMessage savedMessage = supportMessageRepository.save(message);
        log.info("Message {} assigned to admin {}", messageId, adminId);

        return SupportMessageResponse.fromEntity(savedMessage);
    }

    @Transactional
    public SupportMessageResponse updateMessage(Long messageId, Long adminId, UpdateSupportMessageRequest request) {
        SupportMessage message = supportMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin) {
            throw new RuntimeException("Only admins can update messages");
        }

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

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin) {
            throw new RuntimeException("Only admins can delete messages");
        }

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

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin) {
            throw new RuntimeException("Only admins can delete replies");
        }

        supportMessageReplyRepository.delete(reply);
        log.info("Reply {} deleted by admin {}", replyId, adminId);
    }

    @Transactional
    public SupportMessageReplyResponse updateReply(Long replyId, Long adminId, UpdateReplyRequest request) {
        SupportMessageReply reply = supportMessageReplyRepository.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ROLE_ADMIN);

        if (!isAdmin) {
            throw new RuntimeException("Only admins can update replies");
        }

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

        // --- Statistics by admin ---
        Map<String, Long> messagesByAdmin = new HashMap<>();
        List<Object[]> adminCounts = em.createQuery("SELECT COALESCE(CONCAT(m.admin.firstName, ' ', m.admin.lastName), 'UNASSIGNED'), COUNT(m) FROM SupportMessage m GROUP BY m.admin.firstName, m.admin.lastName")
                .getResultList();

        for (Object[] result : adminCounts) {
            messagesByAdmin.put((String) result[0], (Long) result[1]);
        }

        // --- Unassigned messages ---
        Long unassignedMessages = supportMessageRepository.countByAdminIsNull();

        // --- Messages over the last 7 days ---
        Map<LocalDate, Long> last7DaysStats = new HashMap<>();
        List<SupportMessageRepository.MessagesByDay> messagesByDays = supportMessageRepository.countByCreatedAtSince(weekAgo);

        // Initialize map with 0 for the last 7 days
        for (int i = 0; i < 7; i++) {
            last7DaysStats.put(today.minusDays(i), 0L);
        }

        // Populate with actual data
        for (SupportMessageRepository.MessagesByDay item : messagesByDays) {
            last7DaysStats.put(item.getDate(), item.getCount());
        }

        return SupportStatsResponse.builder()
                .totalMessages(totalMessages)
                .openMessages(openMessages)
                .inProgressMessages(inProgressMessages)
                .closedMessages(closedMessages)
                .highPriorityMessages(highPriorityMessages)
                .mediumPriorityMessages(mediumPriorityMessages)
                .lowPriorityMessages(lowPriorityMessages)
                .messagesByAdmin(messagesByAdmin)
                .unassignedMessages(unassignedMessages)
                .last7DaysStats(last7DaysStats)
                .build();
    }

    private LocalDateTime parseDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return null;
        }
        try {
            // Try parsing as full date-time first
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                // Fallback to date only, assuming start of day
                return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
            } catch (Exception ex) {
                log.warn("Failed to parse date: {}", dateString);
                return null;
            }
        }
    }
}

