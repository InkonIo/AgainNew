package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.BotRepository;
import com.chatalyst.backend.Repository.ChatMessageRepository;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.dto.UpdateBotRequest;
import com.chatalyst.backend.dto.BotStats;
import com.chatalyst.backend.dto.CreateBotRequest;
import com.chatalyst.backend.model.Bot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import com.chatalyst.backend.security.services.ProductService;
import com.chatalyst.backend.dto.ProductResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotService {

    private final BotRepository botRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    @Qualifier("telegramWebClient")
    private final WebClient telegramWebClient;
    private final ChatMessageRepository chatMessageRepository;
    private final ProductService productService;
    private final PsObjectStorageService psObjectStorageService;

    @Value("${telegram.webhook.base-url}")
    private String telegramWebhookBaseUrl;

    /**
     * Создает нового бота и настраивает его в Telegram.
     * @param createBotRequest DTO с данными для создания бота.
     * @param userId ID пользователя, создающего бота.
     * @return Созданный объект Bot.
     * @throws RuntimeException если токен недействителен, бот уже существует или другие ошибки.
     */
    @Transactional
    public Bot createBot(CreateBotRequest createBotRequest, Long userId) {
        // 1. Проверяем, существует ли пользователь
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с ID: " + userId));

        // 2. Проверяем, не существует ли уже бот с таким botIdentifier или accessToken
        if (botRepository.findByBotIdentifier(createBotRequest.getBotIdentifier()).isPresent()) {
            throw new RuntimeException("Бот с таким идентификатором уже существует.");
        }
        if (botRepository.findByAccessToken(createBotRequest.getAccessToken()).isPresent()) {
            throw new RuntimeException("Бот с таким токеном доступа уже зарегистрирован.");
        }

        // 3. Валидация токена Telegram через getMe API
        JsonNode botInfo = validateTelegramBotToken(createBotRequest.getAccessToken(), createBotRequest.getBotIdentifier());
        Long telegramBotApiId = botInfo.path("id").asLong(); // Получаем ID бота от Telegram

        // 4. Создаем и сохраняем сущность Bot
        Bot newBot = new Bot();
        newBot.setName(createBotRequest.getName());
        newBot.setBotIdentifier(createBotRequest.getBotIdentifier());
        newBot.setPlatform(createBotRequest.getPlatform());
        newBot.setAccessToken(createBotRequest.getAccessToken());
        newBot.setTelegramBotApiId(telegramBotApiId); // Сохраняем Telegram API ID
        newBot.setOwner(owner);
        newBot.setDescription(createBotRequest.getDescription()); // Сохраняем описание

        Bot savedBot = botRepository.save(newBot);
        log.info("Бот сохранен в БД: {}", savedBot.getName());

        // 5. Устанавливаем Webhook для нового бота
        setTelegramWebhook(savedBot.getAccessToken(), savedBot.getBotIdentifier());

        return savedBot;
    }

    /**
     * Валидирует токен Telegram бота, вызывая метод getMe.
     * @param token Токен Telegram бота.
     * @param expectedBotIdentifier Ожидаемый username бота.
     * @return JsonNode с информацией о боте.
     * @throws RuntimeException если токен недействителен или не соответствует ожидаемому botIdentifier.
     */
    private JsonNode validateTelegramBotToken(String token, String expectedBotIdentifier) {
        log.info("Валидация токена Telegram бота: {}", token);
        try {
            Mono<String> responseMono = telegramWebClient.get()
                    // ИЗМЕНЕНИЕ: Используем относительный URI, так как базовый URL уже настроен в WebClientConfig
                    .uri(String.format("/bot%s/getMe", token))
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);

            if (!rootNode.path("ok").asBoolean()) {
                throw new RuntimeException("Недействительный токен Telegram бота: " + rootNode.path("description").asText());
            }

            JsonNode botUser = rootNode.path("result");
            String actualBotIdentifier = botUser.path("username").asText();
            Long actualTelegramBotApiId = botUser.path("id").asLong(); // Получаем ID бота

            // Telegram требует, чтобы username бота заканчивался на "_bot"
            if (!actualBotIdentifier.equalsIgnoreCase(expectedBotIdentifier)) {
                throw new RuntimeException(String.format(
                        "Идентификатор бота в токене (%s) не совпадает с введенным (%s). " +
                        "Убедитесь, что вы ввели правильный username бота, полученный от BotFather.",
                        actualBotIdentifier, expectedBotIdentifier));
            }

            // Проверяем, не зарегистрирован ли уже этот Telegram API ID у нас в БД
            if (botRepository.findByTelegramBotApiId(actualTelegramBotApiId).isPresent()) {
                throw new RuntimeException("Этот Telegram бот (по его ID) уже зарегистрирован в системе.");
            }


            log.info("Токен Telegram бота успешно валидирован для botIdentifier: {}", actualBotIdentifier);
            return botUser;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Недействительный токен Telegram бота. Проверьте правильность токена.", e);
            }
            throw new RuntimeException("Ошибка при валидации токена Telegram бота: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Неизвестная ошибка при валидации токена Telegram бота: " + e.getMessage(), e);
        }
    }

    /**
     * Устанавливает Webhook для Telegram бота.
     * @param botToken Токен Telegram бота.
     * @param botIdentifier Идентификатор бота (username).
     * @throws RuntimeException если не удалось установить Webhook.
     */
    private void setTelegramWebhook(String botToken, String botIdentifier) {
        String webhookUrl = telegramWebhookBaseUrl + "/api/telegram/webhook/" + botIdentifier; // Добавляем botIdentifier в URL
        log.info("Установка Webhook для бота {}: {}", botIdentifier, webhookUrl);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("url", webhookUrl);

        try {
            Mono<String> responseMono = telegramWebClient.post()
                    // ИЗМЕНЕНИЕ: Используем относительный URI
                    .uri(String.format("/bot%s/setWebhook", botToken))
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);

            if (!rootNode.path("ok").asBoolean()) {
                throw new RuntimeException("Не удалось установить Webhook для бота " + botIdentifier + ": " + rootNode.path("description").asText());
            }
            log.info("Webhook успешно установлен для бота {}", botIdentifier);

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при установке Webhook для бота " + botIdentifier + ": " + e.getMessage(), e);
        }
    }

    /**
     * Обновляет данные существующего бота.
     * @param botId ID бота для обновления.
     * @param userId ID пользователя, который пытается обновить бота.
     * @param updateBotRequest DTO с данными для обновления.
     * @return Обновленный объект бота.
     * @throws RuntimeException если бот не найден или пользователь не является его владельцем.
     */
    @Transactional
    public Bot updateBot(Long botId, Long userId, UpdateBotRequest updateBotRequest) {
        // 1. Находим бота по его ID
        Bot existingBot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

        // 2. Добавлена более надёжная проверка, что текущий пользователь является владельцем этого бота
        if (existingBot.getOwner() == null || !existingBot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для обновления этого бота.");
        }

        // 3. Обновляем поля, если они переданы в запросе
        if (updateBotRequest.getName() != null && !updateBotRequest.getName().isBlank()) {
            existingBot.setName(updateBotRequest.getName());
        }
        if (updateBotRequest.getDescription() != null) {
            existingBot.setDescription(updateBotRequest.getDescription());
        }
        if (updateBotRequest.getShopName() != null) {
            existingBot.setShopName(updateBotRequest.getShopName());
        }

        // 4. Сохраняем обновленный объект в базу данных
        return botRepository.save(existingBot);
    }

    /**
     * Получает список всех ботов, принадлежащих определенному пользователю.
     * @param userId ID пользователя.
     * @return Список объектов Bot.
     */
    public List<Bot> getBotsByUserId(Long userId) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с ID: " + userId));
        return botRepository.findByOwner(owner);
    }

    /**
     * Получает бота по его ID.
     * @param botId ID бота.
     * @return Optional с объектом Bot.
     */
    public Optional<Bot> getBotById(Long botId) {
        return botRepository.findById(botId);
    }

    /**
     * Удаляет бота.
     * @param botId ID бота.
     * @param userId ID пользователя, который пытается удалить бота (для проверки прав).
     * @throws RuntimeException если бот не найден или пользователь не является владельцем.
     */
    @Transactional
    public void deleteBot(Long botId, Long userId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для удаления этого бота.");
        }
        
                // Удаляем все продукты, связанные с ботом, и их изображения
        List<ProductResponse> productsToDelete = productService.getProductsByBotId(botId, userId);
        for (ProductResponse product : productsToDelete) {
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                psObjectStorageService.deleteImage(product.getImageUrl());
                log.info("Изображение {} удалено для продукта ID {}", product.getImageUrl(), product.getId());
            }
            productService.deleteProduct(product.getId(), userId);
            log.info("Продукт ID {} удален.", product.getId());
        }

        // ИЗМЕНЕНИЕ: Удаляем Webhook из Telegram перед удалением бота из БД
        deleteTelegramWebhook(bot.getAccessToken());

        botRepository.delete(bot);
        log.info("Бот с ID {} успешно удален.", botId);
    }

    /**
     * Метод для удаления вебхука.
     * @param botToken Токен Telegram бота.
     */
    private void deleteTelegramWebhook(String botToken) {
        log.info("Удаление Webhook для бота с токеном: {}", botToken);
        try {
            Mono<String> responseMono = telegramWebClient.post()
                    // ИЗМЕНЕНИЕ: Используем относительный URI
                    .uri(String.format("/bot%s/deleteWebhook", botToken))
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);

            if (!rootNode.path("ok").asBoolean()) {
                log.warn("Не удалось удалить Webhook: {}", rootNode.path("description").asText());
            } else {
                log.info("Webhook успешно удален.");
            }
        } catch (Exception e) {
            log.error("Ошибка при удалении Webhook: {}", e.getMessage(), e);
        }
    }

    public BotStats getBotStatistics(Long botId, Long userId) {
    // 1. Находим бота по его ID
    Bot bot = botRepository.findById(botId)
            .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

    // 2. Проверяем, что пользователь является владельцем этого бота
    if (!bot.getOwner().getId().equals(userId)) {
        throw new RuntimeException("У вас нет прав для доступа к статистике этого бота.");
    }
    
    // 3. Получаем botIdentifier из найденного бота
    String botIdentifier = bot.getBotIdentifier();

    // 4. Получаем статистику, используя методы репозитория
    long totalMessages = chatMessageRepository.countByBotIdentifier(botIdentifier);
    long totalDialogues = chatMessageRepository.countDistinctChatIdsByBotIdentifier(botIdentifier);

    // 5. Возвращаем DTO со статистикой
    return new BotStats(totalMessages, totalDialogues);
}

    public void updateShopName(Long botId, Long userId, String shopName) {
    Bot bot = botRepository.findById(botId)
            .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

    // Проверяем, что этот бот принадлежит пользователю
    if (!bot.getOwner().getId().equals(userId)) {
        throw new RuntimeException("Нет прав на изменение имени магазина этого бота");
    }
    bot.setShopName(shopName);
    botRepository.save(bot);
}

    /**
     * Активирует webhook для бота.
     * @param botId ID бота.
     * @param userId ID пользователя (для проверки прав).
     * @throws RuntimeException если бот не найден или пользователь не является владельцем.
     */
    @Transactional
    public void activateWebhook(Long botId, Long userId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для управления этим ботом.");
        }

        // Устанавливаем webhook
        setTelegramWebhook(bot.getAccessToken(), bot.getBotIdentifier());
        log.info("Webhook активирован для бота {} (ID: {})", bot.getBotIdentifier(), botId);
    }

    /**
     * Деактивирует webhook для бота.
     * @param botId ID бота.
     * @param userId ID пользователя (для проверки прав).
     * @throws RuntimeException если бот не найден или пользователь не является владельцем.
     */
    @Transactional
    public void deactivateWebhook(Long botId, Long userId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для управления этим ботом.");
        }

        // Удаляем webhook
        deleteTelegramWebhook(bot.getAccessToken());
        log.info("Webhook деактивирован для бота {} (ID: {})", bot.getBotIdentifier(), botId);
    }

    /**
     * Получает статус webhook для бота.
     * @param botId ID бота.
     * @param userId ID пользователя (для проверки прав).
     * @return Строка с информацией о статусе webhook.
     * @throws RuntimeException если бот не найден или пользователь не является владельцем.
     */
    public String getWebhookStatus(Long botId, Long userId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден с ID: " + botId));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для доступа к информации об этом боте.");
        }

        try {
            Mono<String> responseMono = telegramWebClient.get()
                    .uri(String.format("/bot%s/getWebhookInfo", bot.getAccessToken()))
                    .retrieve()
                    .bodyToMono(String.class);

            String responseString = responseMono.block();
            JsonNode rootNode = objectMapper.readTree(responseString);

            if (!rootNode.path("ok").asBoolean()) {
                return "Ошибка получения информации о webhook: " + rootNode.path("description").asText();
            }

            JsonNode result = rootNode.path("result");
            String url = result.path("url").asText();
            boolean hasCustomCertificate = result.path("has_custom_certificate").asBoolean();
            int pendingUpdateCount = result.path("pending_update_count").asInt();
            String lastErrorDate = result.path("last_error_date").asText();
            String lastErrorMessage = result.path("last_error_message").asText();

            StringBuilder status = new StringBuilder();
            if (url.isEmpty()) {
                status.append("Webhook не установлен");
            } else {
                status.append("Webhook активен: ").append(url);
                status.append("\nОжидающих обновлений: ").append(pendingUpdateCount);
                if (hasCustomCertificate) {
                    status.append("\nИспользуется пользовательский сертификат");
                }
                if (!lastErrorDate.isEmpty() && !lastErrorMessage.isEmpty()) {
                    status.append("\nПоследняя ошибка: ").append(lastErrorMessage);
                }
            }

            return status.toString();

        } catch (Exception e) {
            log.error("Ошибка при получении статуса webhook для бота {}: {}", botId, e.getMessage(), e);
            return "Ошибка при получении статуса webhook: " + e.getMessage();
        }
    }
}

