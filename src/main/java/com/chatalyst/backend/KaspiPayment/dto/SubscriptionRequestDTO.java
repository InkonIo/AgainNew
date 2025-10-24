package com.chatalyst.backend.KaspiPayment.dto;

import lombok.Data;

@Data
public class SubscriptionRequestDTO {
    private Long userId;          // ID пользователя
    private String subscriptionType; // "STANDARD" или "PREMIUM"
}
