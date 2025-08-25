package com.chatalyst.backend.StripePayment.service;

import com.chatalyst.backend.StripePayment.exception.QuotaExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Шлюз для чата, который управляет лимитами перед вызовом OpenAI
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGateway {
    
    private final QuotaService quotaService;
    // TODO: Заменить на ваш реальный OpenAIService
    // private final OpenAIService openAIService;

    /**
     * Отправляет запрос к OpenAI с предварительной проверкой лимитов
     * 
     * @param ownerUserId ID владельца бота
     * @param prompt текст запроса
     * @return ответ от OpenAI
     * @throws QuotaExceededException если лимит исчерпан
     */
    public String ask(Long ownerUserId, String prompt) {
        log.debug("Попытка отправить запрос к OpenAI для пользователя {}: {}", ownerUserId, prompt);
        
        // 1) Списываем 1 сообщение (или другое число, если нужно)
        quotaService.consumeOrThrow(ownerUserId, 1);
        
        // 2) Вызываем OpenAI
        // TODO: Раскомментировать и настроить под ваш OpenAIService
        // return openAIService.ask(prompt);
        
        // Временная заглушка для тестирования
        log.info("Лимит проверен, OpenAI вызов симулирован для пользователя {}", ownerUserId);
        return "Это тестовый ответ от OpenAI. В реальной реализации здесь будет вызов вашего OpenAIService.";
    }

    /**
     * Отправляет запрос к OpenAI с указанным количеством единиц
     * 
     * @param ownerUserId ID владельца бота
     * @param prompt текст запроса
     * @param units количество единиц для списания
     * @return ответ от OpenAI
     * @throws QuotaExceededException если лимит исчерпан
     */
    public String ask(Long ownerUserId, String prompt, int units) {
        log.debug("Попытка отправить запрос к OpenAI для пользователя {} ({} единиц): {}", ownerUserId, units, prompt);
        
        // 1) Списываем указанное количество единиц
        quotaService.consumeOrThrow(ownerUserId, units);
        
        // 2) Вызываем OpenAI
        // TODO: Раскомментировать и настроить под ваш OpenAIService
        // return openAIService.ask(prompt);
        
        // Временная заглушка для тестирования
        log.info("Лимит проверен ({} единиц), OpenAI вызов симулирован для пользователя {}", units, ownerUserId);
        return String.format("Это тестовый ответ от OpenAI (списано %d единиц). В реальной реализации здесь будет вызов вашего OpenAIService.", units);
    }
}
