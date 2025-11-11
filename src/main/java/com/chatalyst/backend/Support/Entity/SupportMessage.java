package com.chatalyst.backend.Support.Entity;

import com.chatalyst.backend.Entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "support_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status = MessageStatus.OPEN;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessagePriority priority = MessagePriority.MEDIUM;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SupportMessageReply> replies = new ArrayList<>();

    // Поле для связи с заказом, если сообщение создано на основе заказа
    @Column(name = "order_id")
    private Long orderId;

    // === НОВЫЕ ПОЛЯ ДЛЯ АРХИВАЦИИ ===
    
    @Column(nullable = false)
    private Boolean archived = false;
    
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
}

