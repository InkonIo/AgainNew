package com.chatalyst.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBotRequest {
    @NotBlank(message = "Bot name cannot be empty")
    @Size(min = 2, max = 100, message = "Bot name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Bot identifier (username) cannot be empty")
    @Size(min = 5, max = 32, message = "Bot identifier must be between 5 and 32 characters")
    private String botIdentifier; // Например, username бота в Telegram (должен заканчиваться на _bot)

    @NotBlank(message = "Access token cannot be empty")
    private String accessToken; // Токен Telegram API

    @NotBlank(message = "Platform cannot be empty")
    private String platform; // Например, "telegram"

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description; // Описание бота

    @Size(max = 100, message = "Shop name must not exceed 100 characters")
    private String shopName; // Имя магазина, опционально
}
