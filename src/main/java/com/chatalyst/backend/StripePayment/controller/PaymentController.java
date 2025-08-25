package com.chatalyst.backend.StripePayment.controller;

import com.chatalyst.backend.StripePayment.dto.PaymentMethodRequest;
import com.chatalyst.backend.StripePayment.dto.PaymentMethodResponse;
import com.chatalyst.backend.StripePayment.dto.StripeCreatePaymentIntentRequest;
import com.chatalyst.backend.StripePayment.dto.StripePaymentResponse;
import com.chatalyst.backend.StripePayment.service.CardManagementService;
import com.chatalyst.backend.StripePayment.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PaymentController {

    private final StripeService stripeService;
    private final CardManagementService cardManagementService;

    // ============ КОНФИГУРАЦИЯ ============
    
    /**
     * Получение конфигурации Stripe (publishable key)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        log.info("GET /api/payments/config - получение Stripe конфигурации");
        return ResponseEntity.ok(stripeService.getStripeConfig());
    }

    // ============ ПЛАНЫ ============
    
    /**
     * Получение списка доступных планов подписки
     */
    @GetMapping("/plans")
    public ResponseEntity<Map<String, Object>> getAvailablePlans() {
        log.info("GET /api/payments/plans - получение доступных планов");
        
        Map<String, Object> plans = Map.of(
            "BASIC", Map.of(
                "name", "Basic Plan",
                "price", 9.99,
                "priceInCents", 999,
                "messages", 5000,
                "bots", 1,
                "features", new String[]{"5000 сообщений в месяц", "1 бот", "Базовая поддержка"}
            ),
            "PREMIUM", Map.of(
                "name", "Premium Plan",
                "price", 19.99,
                "priceInCents", 1999,
                "messages", 20000,
                "bots", 5,
                "features", new String[]{"20000 сообщений в месяц", "5 ботов", "Приоритетная поддержка"}
            )
        );
        return ResponseEntity.ok(plans);
    }

    /**
     * Покупка плана (создание платежного намерения для плана)
     */
    @PostMapping("/plans/purchase")
    public ResponseEntity<StripePaymentResponse> purchasePlan(@RequestBody StripeCreatePaymentIntentRequest request) {
        log.info("POST /api/payments/plans/purchase - покупка плана для пользователя {} с priceId {}", 
                request.getUserId(), request.getPriceId());
        
        try {
            StripePaymentResponse response = stripeService.createPaymentIntent(request);
            
            if ("failed".equals(response.getStatus())) {
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка при покупке плана: {}", e.getMessage(), e);
            StripePaymentResponse errorResponse = new StripePaymentResponse();
            errorResponse.setStatus("failed");
            errorResponse.setMessage("Ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Активация плана после успешной оплаты
     */
    @PostMapping("/plans/activate")
    public ResponseEntity<Map<String, String>> activatePlan(
            @RequestParam String paymentIntentId,
            @RequestParam String planType,
            @RequestParam Long userId) {
        
        log.info("POST /api/payments/plans/activate - активация плана {} для пользователя {}", planType, userId);
        
        try {
            stripeService.activatePlanAfterPayment(paymentIntentId, planType, userId);
            return ResponseEntity.ok(Map.of("message", "План успешно активирован"));
        } catch (Exception e) {
            log.error("Ошибка активации плана: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Не удалось активировать план: " + e.getMessage()));
        }
    }

    // ============ ПЛАТЕЖНЫЕ НАМЕРЕНИЯ ============
    
    /**
     * Создание платежного намерения (PaymentIntent)
     */
    @PostMapping("/payment-intents")
    public ResponseEntity<StripePaymentResponse> createPaymentIntent(@RequestBody StripeCreatePaymentIntentRequest request) {
        log.info("POST /api/payments/payment-intents - создание платежного намерения для пользователя {} с priceId {}", 
                request.getUserId(), request.getPriceId());
        
        try {
            StripePaymentResponse response = stripeService.createPaymentIntent(request);
            
            if ("failed".equals(response.getStatus())) {
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка создания платежного намерения: {}", e.getMessage(), e);
            StripePaymentResponse errorResponse = new StripePaymentResponse();
            errorResponse.setStatus("failed");
            errorResponse.setMessage("Ошибка: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // ============ УПРАВЛЕНИЕ КАРТАМИ (СОВРЕМЕННЫЙ ПОДХОД) ============
    
    /**
     * Получение списка сохраненных карт пользователя
     */
    @GetMapping("/cards")
    public ResponseEntity<List<PaymentMethodResponse>> getUserCards(@RequestParam(required = false) Long userId) {
        log.info("GET /api/payments/cards - получение карт для пользователя {}", userId);
        
        try {
            Long targetUserId = userId != null ? userId : 1L; // Тестовый ID
            List<PaymentMethodResponse> cards = cardManagementService.getUserCards(targetUserId);
            
            log.info("Найдено {} карт для пользователя {}", cards.size(), targetUserId);
            return ResponseEntity.ok(cards);
            
        } catch (Exception e) {
            log.error("Ошибка получения карт: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Получение информации о конкретной карте
     */
    @GetMapping("/cards/{cardId}")
    public ResponseEntity<PaymentMethodResponse> getCard(
            @PathVariable String cardId,
            @RequestParam(required = false) Long userId) {
        
        log.info("GET /api/payments/cards/{} - получение информации о карте для пользователя {}", cardId, userId);
        
        try {
            Long targetUserId = userId != null ? userId : 1L; // Тестовый ID
            PaymentMethodResponse card = cardManagementService.getCard(targetUserId, cardId);
            
            if ("failed".equals(card.getStatus())) {
                return ResponseEntity.badRequest().body(card);
            }
            
            return ResponseEntity.ok(card);
            
        } catch (Exception e) {
            log.error("Ошибка получения информации о карте: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                PaymentMethodResponse.builder()
                    .status("failed")
                    .message("Не удалось получить информацию о карте: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Добавление новой карты
     */
    @PostMapping("/cards")
    public ResponseEntity<PaymentMethodResponse> addCard(
            @RequestBody PaymentMethodRequest request,
            @RequestParam(required = false) Long userId) {
        
        log.info("POST /api/payments/cards - добавление карты для пользователя {}", userId);
        
        try {
            Long targetUserId = userId != null ? userId : 1L; // Тестовый ID
            PaymentMethodResponse response = cardManagementService.createCard(targetUserId, request);
            
            if ("failed".equals(response.getStatus())) {
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка добавления карты: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                PaymentMethodResponse.builder()
                    .status("failed")
                    .message("Не удалось добавить карту: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Обновление карты (например, установка как карты по умолчанию)
     */
    @PutMapping("/cards/{cardId}")
    public ResponseEntity<PaymentMethodResponse> updateCard(
            @PathVariable String cardId,
            @RequestBody PaymentMethodRequest request,
            @RequestParam(required = false) Long userId) {
        
        log.info("PUT /api/payments/cards/{} - обновление карты для пользователя {}", cardId, userId);
        
        try {
            Long targetUserId = userId != null ? userId : 1L; // Тестовый ID
            
            // Проверяем, принадлежит ли карта пользователю
            if (!cardManagementService.isCardOwnedByUser(targetUserId, cardId)) {
                return ResponseEntity.badRequest().body(
                    PaymentMethodResponse.builder()
                        .status("failed")
                        .message("Карта не принадлежит пользователю")
                        .build()
                );
            }
            
            PaymentMethodResponse response = cardManagementService.updateCard(targetUserId, cardId, request);
            
            if ("failed".equals(response.getStatus())) {
                return ResponseEntity.badRequest().body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Ошибка обновления карты: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(
                PaymentMethodResponse.builder()
                    .status("failed")
                    .message("Не удалось обновить карту: " + e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Удаление карты
     */
    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Map<String, String>> deleteCard(
            @PathVariable String cardId,
            @RequestParam(required = false) Long userId) {
        
        log.info("DELETE /api/payments/cards/{} - удаление карты для пользователя {}", cardId, userId);
        
        try {
            Long targetUserId = userId != null ? userId : 1L; // Тестовый ID
            
            // Проверяем, принадлежит ли карта пользователю
            if (!cardManagementService.isCardOwnedByUser(targetUserId, cardId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Карта не принадлежит пользователю"));
            }
            
            boolean success = cardManagementService.deleteCard(targetUserId, cardId);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Карта успешно удалена"));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Не удалось удалить карту"));
            }
            
        } catch (Exception e) {
            log.error("Ошибка удаления карты: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось удалить карту: " + e.getMessage()));
        }
    }

    // ============ ОБРАТНАЯ СОВМЕСТИМОСТЬ ============
    
    /**
     * Получение списка карт (старый формат для обратной совместимости)
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<List<Map<String, Object>>> getPaymentMethods(@RequestParam(required = false) Long userId) {
        log.info("GET /api/payments/payment-methods - получение карт (старый формат) для пользователя {}", userId);
        
        try {
            Long targetUserId = userId != null ? userId : 1L;
            List<Map<String, Object>> paymentMethods = stripeService.getPaymentMethods(targetUserId);
            
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            log.error("Ошибка получения карт: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    /**
     * Привязка карты (старый формат для обратной совместимости)
     */
    @PostMapping("/payment-methods")
    public ResponseEntity<Map<String, String>> attachPaymentMethod(
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false) Long userId) {
        
        String paymentMethodId = (String) request.get("paymentMethodId");
        log.info("POST /api/payments/payment-methods - привязка карты {} (старый формат) для пользователя {}", paymentMethodId, userId);
        
        try {
            if (paymentMethodId == null || paymentMethodId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "paymentMethodId обязателен"));
            }
            
            Long targetUserId = userId != null ? userId : 1L;
            boolean success = stripeService.attachPaymentMethodToCustomer(targetUserId, paymentMethodId);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Карта успешно добавлена"));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Не удалось добавить карту"));
            }
        } catch (Exception e) {
            log.error("Ошибка привязки карты: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось добавить карту: " + e.getMessage()));
        }
    }
    
    /**
     * Удаление карты (старый формат для обратной совместимости)
     */
    @DeleteMapping("/payment-methods/{paymentMethodId}")
    public ResponseEntity<Map<String, String>> removePaymentMethod(@PathVariable String paymentMethodId) {
        log.info("DELETE /api/payments/payment-methods/{} - удаление карты (старый формат)", paymentMethodId);
        
        try {
            boolean success = stripeService.detachPaymentMethod(paymentMethodId);
            
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Карта успешно удалена"));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "Не удалось удалить карту"));
            }
        } catch (Exception e) {
            log.error("Ошибка удаления карты: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Не удалось удалить карту: " + e.getMessage()));
        }
    }

    // ============ WEBHOOK ============
    
    /**
     * Обработка Stripe webhook событий
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload, 
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        log.info("POST /api/payments/webhook - получен Stripe webhook");

        try {
            // TODO: В реальном приложении здесь нужно валидировать подпись webhook'а
            log.info("Webhook payload: {}", payload);
            
            // Здесь можно добавить логику обработки различных типов событий
            // например: payment_intent.succeeded, customer.subscription.created и т.д.
            
            return ResponseEntity.ok(Map.of("message", "Webhook обработан"));
        } catch (Exception e) {
            log.error("Ошибка обработки webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Ошибка обработки webhook"));
        }
    }
}

