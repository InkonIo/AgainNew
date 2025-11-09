package com.chatalyst.backend.forbusinessman.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderConfirmationRequest {
    private Long orderId;
    private String clientMessage;
    // paymentScreenshotUrl будет установлен после загрузки файла
}