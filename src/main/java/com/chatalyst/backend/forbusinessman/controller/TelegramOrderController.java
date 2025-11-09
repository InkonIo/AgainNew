package com.chatalyst.backend.forbusinessman.controller;

import com.chatalyst.backend.forbusinessman.dto.OrderConfirmationResponse;
import com.chatalyst.backend.forbusinessman.service.BusinessmanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Контроллер для Telegram бота - создание подтверждений заказов
 * Этот эндпоинт используется ботом, когда клиент отправляет скриншот оплаты
 */
@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TelegramOrderController {

    private final BusinessmanService businessmanService;

    /**
     * Создать подтверждение оплаты заказа (вызывается из Telegram бота)
     */
    @PostMapping(value = "/order-confirmation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrderConfirmationResponse> createOrderConfirmation(
            @RequestParam("orderId") Long orderId,
            @RequestParam("clientUserId") Long clientUserId,
            @RequestParam("clientTelegramChatId") Long clientTelegramChatId,
            @RequestParam(value = "clientTelegramUsername", required = false) String clientTelegramUsername,
            @RequestParam(value = "clientMessage", required = false) String clientMessage,
            @RequestParam(value = "paymentScreenshot", required = false) MultipartFile paymentScreenshot
    ) {
        try {
            log.info("Получен запрос на создание подтверждения для заказа ID: {}", orderId);
            
            OrderConfirmationResponse response = businessmanService.createOrderConfirmation(
                    orderId,
                    clientUserId,
                    clientTelegramChatId,
                    clientTelegramUsername,
                    clientMessage,
                    paymentScreenshot
            );
            
            log.info("Подтверждение успешно создано для заказа ID: {}", orderId);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Ошибка при создании подтверждения заказа: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Получить информацию об оплате для бота (для отображения в Telegram)
     */
    @GetMapping("/payment-info/{botId}")
    public ResponseEntity<?> getPaymentInfoForBot(@PathVariable Long botId) {
        try {
            var paymentInfo = businessmanService.getPaymentInfoPublic(botId);
            return ResponseEntity.ok(paymentInfo);
        } catch (RuntimeException e) {
            log.error("Ошибка при получении информации об оплате: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Ошибка в TelegramOrderController: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(Map.of("error", ex.getMessage()));
    }
}