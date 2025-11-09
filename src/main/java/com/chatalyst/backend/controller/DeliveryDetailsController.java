package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.DeliveryDetailsRequest;
import com.chatalyst.backend.dto.DeliveryDetailsResponse;
import com.chatalyst.backend.security.jwt.JwtUtils;
import com.chatalyst.backend.security.services.DeliveryDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/delivery-details")
@RequiredArgsConstructor
public class DeliveryDetailsController {

    private final DeliveryDetailsService deliveryDetailsService;
    private final JwtUtils jwtUtils;

    /**
     * Создает или обновляет детали доставки для бота.
     * Доступно только владельцу бота.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<DeliveryDetailsResponse> createOrUpdateDeliveryDetails(
            @RequestBody DeliveryDetailsRequest request,
            HttpServletRequest httpRequest) {
        
        Long userId = jwtUtils.getUserIdFromRequest(httpRequest);
        DeliveryDetailsResponse response = deliveryDetailsService.createOrUpdateDeliveryDetails(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получает детали доставки для бота.
     * Доступно всем, кто может получить доступ к данным бота (например, для отображения в UI).
     */
    @GetMapping("/{botId}")
    public ResponseEntity<DeliveryDetailsResponse> getDeliveryDetails(@PathVariable Long botId) {
        DeliveryDetailsResponse response = deliveryDetailsService.getDeliveryDetailsByBotId(botId);
        return ResponseEntity.ok(response);
    }
}
