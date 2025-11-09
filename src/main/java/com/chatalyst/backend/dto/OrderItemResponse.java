package com.chatalyst.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
