package com.chatalyst.backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class UpdateBotRequest {
    @Size(min = 1, max = 255, message = "Имя бота должно быть от 1 до 255 символов")
    private String name;

    private String description;

    @Size(max = 255, message = "Название магазина не должно превышать 255 символов")
    private String shopName;
}