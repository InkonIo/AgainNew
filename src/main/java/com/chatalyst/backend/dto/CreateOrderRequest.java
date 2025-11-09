package com.chatalyst.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
    
    @NotNull(message = "Bot ID must not be null")
    private Long botId;

    // Эти поля заполняются клиентом в Telegram-боте
    @NotBlank(message = "Delivery address must not be blank")
    private String clientDeliveryAddress;
    
    @NotBlank(message = "Contact phone must not be blank")
    private String clientContactPhone;
    
    private String clientComment; // Опционально
}
