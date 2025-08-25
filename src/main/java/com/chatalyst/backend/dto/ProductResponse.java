// src/main/java/com/chatalyst/backend/dto/ProductResponse.java
package com.chatalyst.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO для ответа с информацией о товаре.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String description;
    private String catalog;
    private String subcategory;
    private String imageUrl; // URL изображения товара
    private boolean inStock;
    private Long botId;
}

