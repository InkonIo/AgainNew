// src/main/java/com/chatalyst/backend/dto/UpdateProductRequest.java
package com.chatalyst.backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    @Size(min = 2, max = 100, message = "Название товара должно быть от 2 до 100 символов")
    private String name;

    @DecimalMin(value = "0.01", message = "Цена товара должна быть больше 0")
    private BigDecimal price;

    @Size(max = 1000, message = "Описание товара не может превышать 1000 символов")
    private String description;

    private String catalog;
    private String subcategory;

    private String imageUrl;

    private boolean inStock; // Статус наличия товара
}
