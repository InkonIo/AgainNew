// src/main/java/com/chatalyst/backend/controller/TelegramWebhookController.java
package com.chatalyst.backend.controller;

import com.chatalyst.backend.security.services.TelegramService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class TelegramWebhookController {

    private final TelegramService telegramService;

    /**
     * Эндпоинт для обработки входящих вебхуков от Telegram.
     * Telegram будет отправлять обновления на URL вида:
     * https://ВАШ_NGROK_URL/api/telegram/webhook/ИДЕНТИФИКАТОР_БОТА
     * @param botIdentifier Идентификатор бота (username) из URL.
     * @param updateJson JSON-объект входящего обновления от Telegram.
     * @return ResponseEntity с пустым ответом (Telegram ожидает 200 OK).
     */
    @PostMapping("/webhook/{botIdentifier}") // ИЗМЕНЕНО: теперь принимает botIdentifier из пути
    public ResponseEntity<?> handleTelegramWebhook(@PathVariable String botIdentifier, @RequestBody JsonNode updateJson) {
        log.info("Received webhook for bot {}: {}", botIdentifier, updateJson.toString());
        try {
            telegramService.processUpdate(botIdentifier, updateJson); // Передаем botIdentifier в сервис
            return ResponseEntity.ok().build(); // Telegram ожидает 200 OK
        } catch (Exception e) {
            log.error("Error processing Telegram webhook for bot {}: {}", botIdentifier, e.getMessage(), e);
            // Возвращаем 200 OK, даже если произошла ошибка, чтобы Telegram не пытался повторно отправлять
            return ResponseEntity.ok().build();
        }
    }
}
