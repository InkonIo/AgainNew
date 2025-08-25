// src/main/java/com/chatalyst/backend/security/services/OpenAIService.java
package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Repository.OpenAITokenUsageRepository;
import com.chatalyst.backend.model.OpenAITokenUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class OpenAIService {

    // Цены на токены для модели gpt-3.5-turbo (актуально на 2024 год)
    private static final double USD_PER_1K_PROMPT_TOKENS = 0.0015;
    private static final double USD_PER_1K_COMPLETION_TOKENS = 0.002;
    private static final double KZT_EXCHANGE_RATE = 540.0; // Курс тенге

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Qualifier("openAiWebClient")
    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;
    private final OpenAITokenUsageRepository tokenUsageRepository;

    public OpenAIService(WebClient openAiWebClient, ObjectMapper objectMapper, OpenAITokenUsageRepository tokenUsageRepository) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
        this.tokenUsageRepository = tokenUsageRepository;
    }

    /**
     * Умный ответ с учетом истории сообщений и информации о товарах.
     * @param conversationHistory История диалога.
     * @param productCatalogInfo Информация о товарах.
     * @param shopName Название магазина.
     * @param botIdentifier Идентификатор бота.
     * @param chatId ID чата для сохранения статистики.
     * @return Ответ от AI.
     */
    public String getBotResponse(List<String[]> chatHistory, String productCatalogInfo, String shopName, String botIdentifier, Long chatId) {
        ArrayNode messages = objectMapper.createArrayNode();

        // Системное сообщение: инструкция для ассистента
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "Ты — умный Telegram-бот-консультант, который помогает пользователю найти товары в магазине \"" + shopName + "\". " +
                        "Вот информация из каталога: " + productCatalogInfo + ". " +
                        "Отвечай кратко и по делу, если пользователь что-то просит — предлагай товары по смыслу. " +
                        "Ты можешь догадываться, что он имеет в виду, даже если формулировка не точная. " +
                        "Не выдумывай товары — только из каталога. Если ничего не найдено — мягко скажи об этом."
        );
        messages.add(systemMessage);

        // Добавляем историю сообщений (роль: user / assistant)
        for (String[] msg : chatHistory) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", msg[0]);
            messageNode.put("content", msg[1]);
            messages.add(messageNode);
        }

        return callOpenAI(messages, botIdentifier, chatId);
    }

    /**
     * Улучшенный метод для ответа с поддержкой изображений товаров.
     * @param chatHistory История диалога.
     * @param productCatalogInfo Информация о товарах с URL изображений.
     * @param shopName Название магазина.
     * @param botIdentifier Идентификатор бота.
     * @param chatId ID чата.
     * @return Ответ от AI с указаниями о товарах для показа.
     */
    public String getBotResponseWithImageSupport(List<String[]> chatHistory, String productCatalogInfo, String shopName, String botIdentifier, Long chatId) {
        ArrayNode messages = objectMapper.createArrayNode();

        // Расширенное системное сообщение с инструкциями по изображениям
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content",
                "Ты — умный Telegram-бот-консультант, который помогает пользователю найти товары в магазине \"" + shopName + "\". " +
                        "Вот информация из каталога: " + productCatalogInfo + ". " +
                        "Отвечай кратко и по делу, если пользователь что-то просит — предлагай товары по смыслу. " +
                        "Ты можешь догадываться, что он имеет в виду, даже если формулировка не точная. " +
                        "Не выдумывай товары — только из каталога. Если ничего не найдено — мягко скажи об этом. " +
                        "Когда рекомендуешь товары, упоминай их точные названия в своем ответе — это поможет системе автоматически показать изображения товаров пользователю. " +
                        "Если у товара есть изображение (отмечено как [ИЗОБРАЖЕНИЕ: URL]), то при упоминании этого товара пользователь увидит его фото."
        );
        messages.add(systemMessage);

        // Добавляем историю сообщений
        for (String[] msg : chatHistory) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", msg[0]);
            messageNode.put("content", msg[1]);
            messages.add(messageNode);
        }

        return callOpenAI(messages, botIdentifier, chatId);
    }

    /**
     * Общий метод для вызова OpenAI API.
     * @param messages Массив сообщений для отправки.
     * @param botIdentifier Идентификатор бота.
     * @param chatId ID чата.
     * @return Ответ от AI.
     */
    private String callOpenAI(ArrayNode messages, String botIdentifier, Long chatId) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", openaiModel);
        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.7);

        log.info("⏳ Sending OpenAI request with context for bot: {}", botIdentifier);

        try {
            String responseString = openAiWebClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(responseString);
            String assistantResponse = rootNode.path("choices").get(0).path("message").path("content").asText();

            // Извлекаем информацию об использовании токенов и сохраняем ее
            JsonNode usageNode = rootNode.path("usage");
            if (usageNode.isObject() && botIdentifier != null && chatId != null) {
                saveTokenUsage(usageNode, botIdentifier, chatId);
            }

            log.info("✅ AI response: {}", assistantResponse);
            return assistantResponse;

        } catch (Exception e) {
            log.error("❌ OpenAI error: {}", e.getMessage(), e);
            return "Извините, произошла ошибка при обработке вашего запроса. Попробуйте позже.";
        }
    }

    /**
     * Сохраняет статистику использования токенов.
     * @param usageNode Узел с информацией об использовании токенов.
     * @param botIdentifier Идентификатор бота.
     * @param chatId ID чата.
     */
    private void saveTokenUsage(JsonNode usageNode, String botIdentifier, Long chatId) {
        int promptTokens = usageNode.path("prompt_tokens").asInt();
        int completionTokens = usageNode.path("completion_tokens").asInt();
        int totalTokens = usageNode.path("total_tokens").asInt();

        double usdCost = (promptTokens / 1000.0) * USD_PER_1K_PROMPT_TOKENS +
                (completionTokens / 1000.0) * USD_PER_1K_COMPLETION_TOKENS;
        double kztCost = usdCost * KZT_EXCHANGE_RATE;

        OpenAITokenUsage tokenUsage = OpenAITokenUsage.builder()
                .botIdentifier(botIdentifier)
                .chatId(chatId)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .usdCost(usdCost)
                .kztCost(kztCost)
                .timestamp(LocalDateTime.now())
                .build();

        tokenUsageRepository.save(tokenUsage);
        log.info("📊 Saved token usage for bot {}: prompt={} completion={} cost=${:.6f} (₸{:.2f})",
                botIdentifier, promptTokens, completionTokens, usdCost, kztCost);
    }

    /**
     * Простой ответ без каталога и истории (например, для общего чата).
     */
    public String getBotResponse(String userMessage) {
        try {
            ArrayNode messages = objectMapper.createArrayNode();

            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", "Ты — вежливый помощник Telegram-бота. Отвечай понятно и дружелюбно.");
            messages.add(systemMessage);

            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            return callOpenAI(messages, null, null);

        } catch (Exception e) {
            log.error("Error calling OpenAI API (simple): {}", e.getMessage(), e);
            return "Извините, произошла ошибка при обработке вашего запроса.";
        }
    }
}


