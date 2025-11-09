package com.chatalyst.backend.dto;

import com.chatalyst.backend.model.Order;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private Long botId;
    private Long userId; // ID клиента, сделавшего заказ
    private String userEmail; // Email клиента (если доступен)
    private String clientDeliveryAddress;
    private String clientContactPhone;
    private String clientComment;
    private BigDecimal totalAmount;
    private Order.OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
