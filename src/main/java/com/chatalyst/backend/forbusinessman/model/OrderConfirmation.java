package com.chatalyst.backend.forbusinessman.model;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_confirmations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderConfirmation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;
    
    @ManyToOne
    @JoinColumn(name = "client_user_id", nullable = false)
    private User clientUser;
    
    @Column(name = "client_telegram_chat_id", nullable = false)
    private Long clientTelegramChatId;
    
    @Column(name = "client_telegram_username", length = 100)
    private String clientTelegramUsername;
    
    @Column(name = "payment_screenshot_url", length = 500)
    private String paymentScreenshotUrl;
    
    @Column(name = "client_message", length = 1000)
    private String clientMessage;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ConfirmationStatus status = ConfirmationStatus.PENDING;
    
    @Column(name = "owner_response", length = 1000)
    private String ownerResponse;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum ConfirmationStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}