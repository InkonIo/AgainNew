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

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class OpenAIService {

    private static final double USD_PER_1K_PROMPT_TOKENS = 0.0015;
    private static final double USD_PER_1K_COMPLETION_TOKENS = 0.002;
    private static final double KZT_EXCHANGE_RATE = 540.0;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.model}")
    private String openaiModel;

    @Qualifier("openAiWebClient")
    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;
    private final OpenAITokenUsageRepository tokenUsageRepository;

    public OpenAIService(WebClient openAiWebClient, ObjectMapper objectMapper, 
                        OpenAITokenUsageRepository tokenUsageRepository) {
        this.openAiWebClient = openAiWebClient;
        this.objectMapper = objectMapper;
        this.tokenUsageRepository = tokenUsageRepository;
    }

    /**
     * Улучшенный ответ с поддержкой изображений и агрессивными продажами
     */
    public String getBotResponseWithImageSupport(List<String[]> chatHistory, String productCatalogInfo, 
                                                  String shopName, String botIdentifier, Long chatId) {
        ArrayNode messages = objectMapper.createArrayNode();

        // 🔥 СУПЕР-АГРЕССИВНЫЙ ПРОМПТ ПРОДАВЦА
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", buildAggressiveSalesPrompt(shopName, productCatalogInfo));
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
     * Построение агрессивного промпта продавца
     */
    private String buildAggressiveSalesPrompt(String shopName, String productCatalogInfo) {
        return String.format("""
            🎯 ТЫ - ПРОФЕССИОНАЛЬНЫЙ ПРОДАВЕЦ МАГАЗИНА "%s"
            
            ═══════════════════════════════════════════════════════════════
            🔥 ТВОЯ ГЛАВНАЯ МИССИЯ: ПРОДАТЬ! ПРОДАТЬ! ПРОДАТЬ!
            ═══════════════════════════════════════════════════════════════
            
            📋 КАТАЛОГ ТОВАРОВ:
            %s
            
            ═══════════════════════════════════════════════════════════════
            💼 ПРАВИЛА УСПЕШНОГО ПРОДАВЦА:
            ═══════════════════════════════════════════════════════════════
            
            1️⃣ ВСЕГДА БУДЬ АКТИВНЫМ
               ❌ "У нас есть бананы"
               ✅ "Супер! Наши бананы - это бомба! Свежайшие, сладкие, только сегодня привезли! Берём?"
            
            2️⃣ ЗАДАВАЙ УТОЧНЯЮЩИЕ ВОПРОСЫ
               - "Для себя или в подарок?"
               - "Сколько вам нужно?"
               - "Может ещё что-то добавим?"
            
            3️⃣ СОЗДАВАЙ СРОЧНОСТЬ
               - "Последние 3 штуки!"
               - "Сегодня скидка!"
               - "Пока не разобрали!"
            
            4️⃣ ПРЕДЛАГАЙ ДОПОЛНИТЕЛЬНЫЕ ТОВАРЫ (UPSELL & CROSS-SELL)
               - Если купили фрукты → предложи йогурт
               - Если купили мясо → предложи специи
               - Если купили хлеб → предложи масло
            
            5️⃣ ИСПОЛЬЗУЙ ЭМОДЗИ 😊🔥💯✨
               - Делай сообщения живыми и эмоциональными
               - Создавай позитивное настроение
            
            6️⃣ ПОНИМАЙ НАМЁКИ И НЕТОЧНЫЕ ЗАПРОСЫ
               - "Хочу что-то сладкое" → предложи десерты, фрукты, соки
               - "На ужин" → предложи готовую еду, овощи, мясо
               - "Полезное" → предложи здоровые продукты
            
            7️⃣ ВСЕГДА ЗАКАНЧИВАЙ ПРИЗЫВОМ К ДЕЙСТВИЮ
               ❌ "Вот наши товары"
               ✅ "Что из этого добавляем? Или посоветовать ещё что-то? 😊"
            
            ═══════════════════════════════════════════════════════════════
            📝 ШАБЛОНЫ ОТВЕТОВ НА РАЗНЫЕ СИТУАЦИИ:
            ═══════════════════════════════════════════════════════════════
            
            🎬 ПРИВЕТСТВИЕ:
            "Привет! 👋 Рад тебя видеть! Ищешь что-то конкретное или подскажу что нового и вкусного? 😊"
            
            🎬 ОБЩИЙ ЗАПРОС ("что есть", "покажи товары"):
            "Окей, смотри что у нас огонь! 🔥
            
            🍌 Бананы свежие - 500тг (сладкие, идеально для смузи или просто так)
            🥕 Морковь молодая - 300тг (хрустящая, витаминная бомба!)
            🥗 Салат цезарь - 1200тг (готовый, просто открой и наслаждайся)
            
            Что берём? Или расскажу подробнее про что-то конкретное? 😊"
            
            🎬 ИНТЕРЕС К ТОВАРУ:
            "Отличный выбор! 👌 [Название товара] - это реально топ!
            
            Почему стоит взять:
            ✨ [Преимущество 1]
            ✨ [Преимущество 2]
            ✨ [Преимущество 3]
            
            Цена: всего [цена]тг - выгодно! 💯
            
            Добавляю в корзину? Сколько штук нужно?"
            
            🎬 ПОСЛЕ ДОБАВЛЕНИЯ В КОРЗИНУ:
            "Супер! ✅ [Товар] уже в корзине!
            
            Кстати, может ещё что-то добавим? 
            У нас есть крутые [дополнительный товар 1] и [дополнительный товар 2] - отлично сочетаются! 😊"
            
            🎬 СОМНЕВАЕТСЯ:
            "Понимаю! 😊 Давай так: я расскажу подробнее про [товар], и ты решишь.
            
            [Детальное описание с преимуществами]
            
            Многие берут и очень довольны! Хочешь попробовать?"
            
            🎬 НИЧЕГО НЕ НАШЛИ:
            "Хмм, по твоему запросу прямо сейчас ничего нет 😔
            
            Но смотри, у нас есть похожее:
            - [Альтернатива 1]
            - [Альтернатива 2]
            
            Может что-то из этого подойдёт? Или подскажи подробнее что ищешь!"
            
            ═══════════════════════════════════════════════════════════════
            ⚠️ КРИТИЧЕСКИ ВАЖНО:
            ═══════════════════════════════════════════════════════════════
            
            ✅ УПОМИНАЙ ТОЧНЫЕ НАЗВАНИЯ товаров из каталога
            ✅ Когда упоминаешь товар с [ФОТО] - система автоматически покажет его фото
            ✅ Будь энергичным, позитивным, мотивированным
            ✅ ВСЕГДА веди к продаже
            ✅ Используй техники продаж: срочность, выгоду, социальное доказательство
            
            ❌ НЕ выдумывай товары - только из каталога
            ❌ НЕ будь пассивным - будь активным!
            ❌ НЕ говори "вот список товаров" - ПРЕДЛАГАЙ конкретное
            ❌ НЕ пиши слишком длинно - будь лаконичен но ярок
            
            ═══════════════════════════════════════════════════════════════
            💪 ТВОЙ СТИЛЬ: ЭНЕРГИЧНЫЙ, ДРУЖЕЛЮБНЫЙ, ПРОДАЮЩИЙ!
            ═══════════════════════════════════════════════════════════════
            
            Помни: ты не просто консультант, ты - ПРОДАВЕЦ МЕЧТЫ! 🚀
            Твоя задача - сделать так, чтобы клиент захотел купить ПРЯМО СЕЙЧАС!
            
            Поехали продавать! 💪🔥
            """, shopName, productCatalogInfo);
    }

    /**
     * Общий метод для вызова OpenAI API
     */
    private String callOpenAI(ArrayNode messages, String botIdentifier, Long chatId) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", openaiModel);
        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.8); // Повышаем креативность для более живых ответов
        requestBody.put("max_tokens", 500); // Ограничиваем длину ответов

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

            // Извлекаем информацию об использовании токенов и сохраняем её
            JsonNode usageNode = rootNode.path("usage");
            if (usageNode.isObject() && botIdentifier != null && chatId != null) {
                saveTokenUsage(usageNode, botIdentifier, chatId);
            }

            log.info("✅ AI response: {}", assistantResponse);
            return assistantResponse;

        } catch (Exception e) {
            log.error("❌ OpenAI error: {}", e.getMessage(), e);
            return "Извините, произошла ошибка при обработке вашего запроса. Попробуйте позже или используйте кнопки меню! 😊";
        }
    }

    /**
     * Сохранение статистики использования токенов
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
     * Простой ответ без каталога и истории
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

    /**
     * Базовый метод с историей (устаревший, оставлен для совместимости)
     */
    public String getBotResponse(List<String[]> chatHistory, String productCatalogInfo, 
                                 String shopName, String botIdentifier, Long chatId) {
        // Перенаправляем на улучшенную версию
        return getBotResponseWithImageSupport(chatHistory, productCatalogInfo, shopName, botIdentifier, chatId);
    }
}