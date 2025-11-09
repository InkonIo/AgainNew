package com.chatalyst.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal; // price * quantity
}
