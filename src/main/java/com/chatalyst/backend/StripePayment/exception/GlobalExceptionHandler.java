package com.chatalyst.backend.StripePayment.exception;


import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Data
    public static class ErrorResponse {
        private final String code;
        private final String message;
        private final LocalDateTime timestamp;
        private final String path;

        public ErrorResponse(String code, String message, String path) {
            this.code = code;
            this.message = message;
            this.timestamp = LocalDateTime.now();
            this.path = path;
        }
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "QUOTA_EXCEEDED",
            ex.getMessage(),
            request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "Внутренняя ошибка сервера",
            request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
