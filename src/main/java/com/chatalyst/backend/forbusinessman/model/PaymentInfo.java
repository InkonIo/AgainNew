package com.chatalyst.backend.forbusinessman.model;

import com.chatalyst.backend.model.Bot;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "bot_id", nullable = false, unique = true)
    private Bot bot;
    
    // QR-коды для оплаты
    @Column(name = "kaspi_qr_url", length = 500)
    private String kaspiQrUrl;
    
    @Column(name = "halyk_qr_url", length = 500)
    private String halykQrUrl;
    
    @Column(name = "other_qr_url", length = 500)
    private String otherQrUrl;
    
    // Банковские реквизиты
    @Column(name = "bank_account", length = 100)
    private String bankAccount;
    
    @Column(name = "card_number", length = 20)
    private String cardNumber;
    
    // Контактная информация владельца
    @Column(name = "owner_telegram_username", length = 100)
    private String ownerTelegramUsername;
    
    @Column(name = "owner_telegram_chat_id")
    private Long ownerTelegramChatId;
    
    @Column(name = "owner_phone", length = 20)
    private String ownerPhone;
    
    @Column(name = "owner_email", length = 100)
    private String ownerEmail;
    
    // Инструкции по оплате
    @Column(name = "payment_instructions", length = 1000)
    private String paymentInstructions;
    
    // Включена ли оплата
    @Column(name = "payment_enabled", nullable = false)
    @Builder.Default
    private Boolean paymentEnabled = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}