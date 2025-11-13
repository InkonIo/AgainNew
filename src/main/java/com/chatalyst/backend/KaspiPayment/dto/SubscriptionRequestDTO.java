package com.chatalyst.backend.KaspiPayment.dto;

import lombok.Data;

@Data
public class SubscriptionRequestDTO {
    private Long userId;          // ID пользователя
    private String subscriptionType; // "STANDARD", "PREMIUM" или "USER"
    private int durationInMonths; // 1, 3, 6, 12 (игнорируется для "USER")
}
