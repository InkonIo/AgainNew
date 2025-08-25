// src/main/java/com/chatalyst/backend/dto/CreateProductRequest.java
package com.chatalyst.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateProductRequest {
    @NotBlank(message = "Название товара не может быть пустым")
    @Size(min = 2, max = 100, message = "Название товара должно быть от 2 до 100 символов")
    private String name;

    @NotNull(message = "Цена товара не может быть пустой")
    @DecimalMin(value = "0.01", message = "Цена товара должна быть больше 0")
    private BigDecimal price;

    @Size(max = 1000, message = "Описание товара не может превышать 1000 символов")
    private String description;

    @NotBlank(message = "Каталог не может быть пустым")
    private String catalog; // Каталог товара

    private String subcategory; // Подкаталог товара

    private String imageUrl;

    private boolean inStock = true; // Статус наличия товара

    @NotNull(message = "ID бота не может быть пустым")
    private Long botId; // ID бота, к которому привязывается товар
}
