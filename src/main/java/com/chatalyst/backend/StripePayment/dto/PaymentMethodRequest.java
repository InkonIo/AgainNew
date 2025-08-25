package com.chatalyst.backend.StripePayment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodRequest {
    private String paymentMethodId;
    private Boolean setAsDefault;
    private String cardholderName;
    
    // Для создания новой карты
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
    private String cvc;
    private String postalCode;
}

