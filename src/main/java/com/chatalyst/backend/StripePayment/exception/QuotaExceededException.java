package com.chatalyst.backend.StripePayment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED) // 402
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String msg) {
        super(msg);
    }
}
