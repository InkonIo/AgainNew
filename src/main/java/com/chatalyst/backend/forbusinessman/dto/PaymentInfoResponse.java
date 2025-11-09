package com.chatalyst.backend.forbusinessman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInfoResponse {
    private Long id;
    private Long botId;
    private String kaspiQrUrl;
    private String halykQrUrl;
    private String otherQrUrl;
    private String bankAccount;
    private String cardNumber;
    private String ownerTelegramUsername;
    private Long ownerTelegramChatId;
    private String ownerPhone;
    private String ownerEmail;
    private String paymentInstructions;
    private Boolean paymentEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
