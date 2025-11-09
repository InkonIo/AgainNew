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
public class OrderConfirmationResponse {
    
    private Long id;
    private Long orderId;
    private Long botId;
    private String botName;
    
    // Информация о клиенте
    private Long clientUserId;
    private Long clientTelegramChatId;
    private String clientTelegramUsername;
    
    // Детали заказа
    private String deliveryAddress;
    private String contactPhone;
    private String orderComment;
    private String totalAmount;
    private String orderItems; // Форматированная строка с товарами
    
    // Информация об оплате
    private String paymentScreenshotUrl;
    private String clientMessage;
    
    // Статус
    private String status;
    private String ownerResponse;
    
    // Даты
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}