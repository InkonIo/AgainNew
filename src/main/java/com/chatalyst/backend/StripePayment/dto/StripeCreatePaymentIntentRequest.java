package com.chatalyst.backend.StripePayment.dto;

import lombok.Data;

@Data
public class StripeCreatePaymentIntentRequest {
    private String priceId;
    private Long userId;
}
