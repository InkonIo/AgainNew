package com.chatalyst.backend.StripePayment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StripePaymentResponse {
    private String status;
    private String clientSecret;
    private String paymentIntentId;
    private String message;
}
