package com.chatalyst.backend.StripePayment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodResponse {
    private String id;
    private String type;
    private CardDetails card;
    private boolean isDefault;
    private String status;
    private String message;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CardDetails {
        private String brand;
        private String last4;
        private Integer expMonth;
        private Integer expYear;
        private String country;
        private String funding;
        private String fingerprint;
    }
}

