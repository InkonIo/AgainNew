package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.OrderResponse;
import com.chatalyst.backend.model.Order;
import com.chatalyst.backend.security.jwt.JwtUtils;
import com.chatalyst.backend.security.services.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtUtils jwtUtils;

    /**
     * Получает список заказов для конкретного бота (доступно владельцу бота).
     */
    @GetMapping("/bot/{botId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getOrdersByBot(
            @PathVariable Long botId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        Long userId = jwtUtils.getUserIdFromRequest(httpRequest);
        Page<OrderResponse> orders = orderService.getOrdersByBot(botId, userId, page, size);
        return ResponseEntity.ok(orders);
    }

    /**
     * Обновляет статус заказа (доступно владельцу бота).
     */
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam Order.OrderStatus status,
            HttpServletRequest httpRequest) {

        Long userId = jwtUtils.getUserIdFromRequest(httpRequest);
        OrderResponse updatedOrder = orderService.updateOrderStatus(orderId, status, userId);
        return ResponseEntity.ok(updatedOrder);
    }
}
