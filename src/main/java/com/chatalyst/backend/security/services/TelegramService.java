package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Repository.*;
import com.chatalyst.backend.dto.CartItemResponse;
import com.chatalyst.backend.dto.CreateOrderRequest;
import com.chatalyst.backend.dto.DeliveryDetailsResponse;
import com.chatalyst.backend.dto.OrderResponse;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Product;
import com.chatalyst.backend.model.ChatMessage;
import com.chatalyst.backend.Entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String defaultBotToken;

    private final ObjectMapper objectMapper;
    private final OpenAIService openAIService;
    private final BotRepository botRepository;
    private final ProductRepository productRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CartService cartService;
    private final DeliveryDetailsService deliveryDetailsService;
    private final UserRepository userRepository;
    private final OrderService orderService; // Добавляем OrderService

    @Qualifier("telegramWebClient")
    private final WebClient telegramWebClient;

    public TelegramService(ObjectMapper objectMapper, OpenAIService openAIService,
                           BotRepository botRepository, ProductRepository productRepository,
                           ChatMessageRepository chatMessageRepository,
                           WebClient telegramWebClient,
                           CartService cartService,
                           DeliveryDetailsService deliveryDetailsService,
                           UserRepository userRepository,
                           OrderService orderService) {
        this.objectMapper = objectMapper;
        this.openAIService = openAIService;
        this.botRepository = botRepository;
        this.productRepository = productRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.telegramWebClient = telegramWebClient;
        this.cartService = cartService;
        this.deliveryDetailsService = deliveryDetailsService;
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    public void processUpdate(String botIdentifier, JsonNode updateJson) {
        if (updateJson.has("callback_query")) {
            handleCallbackQuery(botIdentifier, updateJson.get("callback_query"));
        } else if (updateJson.has("message")) {
            JsonNode message = updateJson.get("message");
            long chatId = message.get("chat").get("id").asLong();
            String text = message.has("text") ? message.get("text").asText() : "";
            log.info("Received message for bot {} from chat {}: {}", botIdentifier, chatId, text);

            if (text.startsWith("/")) {
                handleCommand(botIdentifier, chatId, text);
            } else {
                // Проверяем, находится ли пользователь в процессе оформления заказа
                if (isCheckoutProcess(chatId, botIdentifier)) {
                    handleCheckoutInput(botIdentifier, chatId, text);
                } else {
                    sendOpenAIResponse(botIdentifier, chatId, text);
                }
            }
        }
    }

    private void handleCallbackQuery(String botIdentifier, JsonNode callbackQuery) {
        long chatId = callbackQuery.get("message").get("chat").get("id").asLong();
        String callbackId = callbackQuery.get("id").asText();
        String data = callbackQuery.get("data").asText();
        
        log.info("Received callback for bot {}: {}", botIdentifier, data);

        Optional<Bot> botOptional = botRepository.findByBotIdentifier(botIdentifier);
        if (botOptional.isEmpty()) {
            answerCallbackQuery(callbackId, "Бот не найден", true, defaultBotToken);
            return;
        }
        
        Bot bot = botOptional.get();
        User clientUser = getOrCreateClientUser(chatId);

        try {
            if (data.equals("main_menu")) {
                sendMainMenu(chatId, bot);
                answerCallbackQuery(callbackId, null, false, bot.getAccessToken());
                
            } else if (data.equals("catalog")) {
                sendCatalogWithButtons(chatId, bot);
                answerCallbackQuery(callbackId, null, false, bot.getAccessToken());
                
            } else if (data.equals("cart")) {
                sendCartContent(chatId, bot, clientUser);
                answerCallbackQuery(callbackId, null, false, bot.getAccessToken());
                
            } else if (data.equals("contact")) {
                sendContactDetails(chatId, bot);
                answerCallbackQuery(callbackId, null, false, bot.getAccessToken());
                
            } else if (data.startsWith("catalog_")) {
                String catalog = data.substring("catalog_".length());
                sendSubcategoriesWithButtons(chatId, bot, catalog);
                answerCallbackQuery(callbackId, null, false, bot.getAccessToken());
                
            } else if (data.startsWith("subcat_")) {
                String subcategory = data.substring("subcat_".length());
                sendSubcategoryProductsWithButtons(chatId, bot, subcategory);
                answerCallbackQuery(callbackId, null, false, bot.getAccessToken());
                
            } else if (data.startsWith("add_")) {
                Long productId = Long.parseLong(data.substring("add_".length()));
                addToCartAndNotify(chatId, bot, clientUser, productId);
                answerCallbackQuery(callbackId, "✅ Добавлено в корзину!", false, bot.getAccessToken());
                
            } else if (data.startsWith("remove_")) {
                Long productId = Long.parseLong(data.substring("remove_".length()));
                cartService.removeProductFromCart(clientUser.getId(), bot.getId(), productId);
                sendCartContent(chatId, bot, clientUser);
                answerCallbackQuery(callbackId, "✅ Товар удалён", false, bot.getAccessToken());
                
            } else if (data.equals("clear_cart")) {
                cartService.clearCart(clientUser.getId(), bot.getId());
                answerCallbackQuery(callbackId, "✅ Корзина очищена", false, bot.getAccessToken());
                sendMessage(chatId, "Корзина пуста. Хотите посмотреть каталог?", bot.getAccessToken(), createMainMenuKeyboard());
                
            } else if (data.equals("checkout")) {
                sendCheckoutInstructions(chatId, bot);
                answerCallbackQuery(callbackId, null, false, bot.getAccessToken());
                
            } else {
                answerCallbackQuery(callbackId, "Неизвестная команда", true, bot.getAccessToken());
            }
            
        } catch (Exception e) {
            log.error("Ошибка при обработке callback {}: {}", data, e.getMessage());
            answerCallbackQuery(callbackId, "Произошла ошибка: " + e.getMessage(), true, bot.getAccessToken());
        }
    }

    private void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert, String botToken) {
        String url = String.format("/bot%s/answerCallbackQuery", botToken);
        
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("callback_query_id", callbackQueryId);
        if (text != null) {
            requestBody.put("text", text);
        }
        requestBody.put("show_alert", showAlert);

        telegramWebClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(e -> log.error("Ошибка при ответе на callback query: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    private User getOrCreateClientUser(long telegramChatId) {
        Optional<User> existingUser = userRepository.findByEmail("telegram_" + telegramChatId + "@bot.local");
        
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        User newUser = new User();
        newUser.setEmail("telegram_" + telegramChatId + "@bot.local");
        newUser.setFirstName("Telegram User");
        newUser.setLastName(String.valueOf(telegramChatId));
        newUser.setActive(true);
        newUser.setEnabled(true);
        
        User savedUser = userRepository.save(newUser);
        log.info("Создан новый пользователь для Telegram Chat ID: {}", telegramChatId);
        
        return savedUser;
    }

    private void handleCommand(String botIdentifier, long chatId, String command) {
        log.info("Processing command for bot {}: {}", botIdentifier, command);

        Optional<Bot> botOptional = botRepository.findByBotIdentifier(botIdentifier);
        if (botOptional.isEmpty()) {
            sendMessage(chatId, "Бот с таким идентификатором не найден.", defaultBotToken, null);
            return;
        }
        Bot bot = botOptional.get();

        if (command.startsWith("/start")) {
            sendMainMenu(chatId, bot);
        } else {
            sendMessage(chatId, "Используйте кнопки для навигации или просто напишите что вы ищете! 😊", 
                       bot.getAccessToken(), createMainMenuKeyboard());
        }
    }

    private void sendMainMenu(long chatId, Bot bot) {
        String welcomeMessage = String.format(
            "🎉 *Добро пожаловать в %s!*\n\n" +
            "Я помогу вам найти и заказать всё что нужно.\n" +
            "Выберите действие ниже или просто напишите что ищете! 👇",
            bot.getShopName()
        );
        
        sendMessage(chatId, welcomeMessage, bot.getAccessToken(), createMainMenuKeyboard());
    }

    private ObjectNode createMainMenuKeyboard() {
        ArrayNode keyboard = objectMapper.createArrayNode();
        
        ArrayNode row1 = objectMapper.createArrayNode();
        row1.add(createInlineButton("🛍️ Каталог", "catalog"));
        row1.add(createInlineButton("🛒 Корзина", "cart"));
        keyboard.add(row1);
        
        ArrayNode row2 = objectMapper.createArrayNode();
        row2.add(createInlineButton("📞 Контакты", "contact"));
        keyboard.add(row2);
        
        ObjectNode markup = objectMapper.createObjectNode();
        markup.set("inline_keyboard", keyboard);
        
        return markup;
    }

    private ObjectNode createInlineButton(String text, String callbackData) {
        ObjectNode button = objectMapper.createObjectNode();
        button.put("text", text);
        button.put("callback_data", callbackData);
        return button;
    }

    private void sendCatalogWithButtons(long chatId, Bot bot) {
        List<String> catalogs = productRepository.findByBot(bot)
                .stream()
                .map(Product::getCatalog)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (catalogs.isEmpty()) {
            sendMessage(chatId, "В магазине нет доступных каталогов.", bot.getAccessToken(), createMainMenuKeyboard());
            return;
        }

        ArrayNode keyboard = objectMapper.createArrayNode();
        for (String catalog : catalogs) {
            ArrayNode row = objectMapper.createArrayNode();
            row.add(createInlineButton("📂 " + catalog, "catalog_" + catalog));
            keyboard.add(row);
        }
        
        ArrayNode backRow = objectMapper.createArrayNode();
        backRow.add(createInlineButton("« Назад", "main_menu"));
        keyboard.add(backRow);
        
        ObjectNode markup = objectMapper.createObjectNode();
        markup.set("inline_keyboard", keyboard);

        String message = "📋 *Выберите категорию:*";
        sendMessage(chatId, message, bot.getAccessToken(), markup);
    }

    private void sendSubcategoriesWithButtons(long chatId, Bot bot, String catalog) {
        List<String> subcategories = productRepository.findByBotAndCatalog(bot, catalog)
                .stream()
                .map(Product::getSubcategory)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (subcategories.isEmpty()) {
            sendMessage(chatId, "В категории \"" + catalog + "\" нет подкатегорий.", 
                       bot.getAccessToken(), createMainMenuKeyboard());
            return;
        }

        ArrayNode keyboard = objectMapper.createArrayNode();
        for (String subcat : subcategories) {
            ArrayNode row = objectMapper.createArrayNode();
            row.add(createInlineButton("📁 " + subcat, "subcat_" + subcat));
            keyboard.add(row);
        }
        
        ArrayNode navRow = objectMapper.createArrayNode();
        navRow.add(createInlineButton("« К категориям", "catalog"));
        navRow.add(createInlineButton("« Главное меню", "main_menu"));
        keyboard.add(navRow);
        
        ObjectNode markup = objectMapper.createObjectNode();
        markup.set("inline_keyboard", keyboard);

        String message = String.format("📂 *%s*\n\nВыберите подкатегорию:", catalog);
        sendMessage(chatId, message, bot.getAccessToken(), markup);
    }

    private void sendSubcategoryProductsWithButtons(long chatId, Bot bot, String subcategory) {
        List<Product> products = productRepository.findByBotAndSubcategory(bot, subcategory);
        
        if (products.isEmpty()) {
            sendMessage(chatId, "В подкатегории \"" + subcategory + "\" нет товаров.", 
                       bot.getAccessToken(), createMainMenuKeyboard());
            return;
        }

        String headerMessage = String.format("🏷️ *%s*\n\nВсего товаров: %d", subcategory, products.size());
        sendMessage(chatId, headerMessage, bot.getAccessToken(), null);

        for (Product product : products) {
            sendProductCard(chatId, product, bot.getAccessToken());
        }
        
        ArrayNode keyboard = objectMapper.createArrayNode();
        ArrayNode navRow = objectMapper.createArrayNode();
        navRow.add(createInlineButton("« К категориям", "catalog"));
        navRow.add(createInlineButton("🛒 Корзина", "cart"));
        keyboard.add(navRow);
        
        ObjectNode markup = objectMapper.createObjectNode();
        markup.set("inline_keyboard", keyboard);
        
        sendMessage(chatId, "Что добавляем в корзину? 😊", bot.getAccessToken(), markup);
    }

    private void sendProductCard(long chatId, Product product, String botToken) {
        String caption = String.format(
            "🔸 *%s*\n\n💰 Цена: *%s тг*\n\n📝 %s",
            product.getName(),
            product.getPrice(),
            product.getDescription() != null ? product.getDescription() : "Описание отсутствует"
        );
        
        ArrayNode keyboard = objectMapper.createArrayNode();
        ArrayNode row = objectMapper.createArrayNode();
        row.add(createInlineButton("➕ Добавить в корзину", "add_" + product.getId()));
        keyboard.add(row);
        
        ObjectNode markup = objectMapper.createObjectNode();
        markup.set("inline_keyboard", keyboard);
        
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            sendPhoto(chatId, product.getImageUrl(), caption, botToken, markup);
        } else {
            sendMessage(chatId, caption, botToken, markup);
        }
    }

    private void addToCartAndNotify(long chatId, Bot bot, User clientUser, Long productId) {
        CartItemResponse item = cartService.addProductToCart(clientUser.getId(), bot.getId(), productId, 1);
        
        String message = String.format(
            "✅ *%s* добавлен в корзину!\n\n" +
            "Количество: %d шт.\n" +
            "Сумма: %s тг",
            item.getProductName(),
            item.getQuantity(),
            item.getSubtotal()
        );
        
        ArrayNode keyboard = objectMapper.createArrayNode();
        
        ArrayNode row1 = objectMapper.createArrayNode();
        row1.add(createInlineButton("🛒 Перейти в корзину", "cart"));
        row1.add(createInlineButton("🛍️ Продолжить покупки", "catalog"));
        keyboard.add(row1);
        
        ObjectNode markup = objectMapper.createObjectNode();
        markup.set("inline_keyboard", keyboard);
        
        sendMessage(chatId, message, bot.getAccessToken(), markup);
    }

    private void sendCartContent(long chatId, Bot bot, User clientUser) {
        List<CartItemResponse> items = cartService.getCartItems(clientUser.getId(), bot.getId());
        
        if (items.isEmpty()) {
            String message = "🛒 *Ваша корзина пуста*\n\nДавайте найдём что-нибудь интересное!";
            sendMessage(chatId, message, bot.getAccessToken(), createMainMenuKeyboard());
            return;
        }
        
        StringBuilder sb = new StringBuilder("🛒 *Ваша корзина:*\n\n");
        BigDecimal total = BigDecimal.ZERO;
        
        for (int i = 0; i < items.size(); i++) {
            CartItemResponse item = items.get(i);
            sb.append(String.format(
                "%d. *%s*\n   %d шт. × %s тг = *%s тг*\n\n",
                i + 1,
                item.getProductName(),
                item.getQuantity(),
                item.getPrice(),
                item.getSubtotal()
            ));
            total = total.add(item.getSubtotal());
        }
        
        sb.append(String.format("💳 *ИТОГО: %s тг*", total));
        
        ArrayNode keyboard = objectMapper.createArrayNode();
        
        for (CartItemResponse item : items) {
            ArrayNode row = objectMapper.createArrayNode();
            row.add(createInlineButton("❌ " + item.getProductName(), "remove_" + item.getProductId()));
            keyboard.add(row);
        }
        
        ArrayNode actionRow1 = objectMapper.createArrayNode();
        actionRow1.add(createInlineButton("✅ Оформить заказ", "checkout"));
        actionRow1.add(createInlineButton("🗑️ Очистить", "clear_cart"));
        keyboard.add(actionRow1);
        
        ArrayNode actionRow2 = objectMapper.createArrayNode();
        actionRow2.add(createInlineButton("🛍️ Продолжить покупки", "catalog"));
        actionRow2.add(createInlineButton("« Главное меню", "main_menu"));
        keyboard.add(actionRow2);
        
        ObjectNode markup = objectMapper.createObjectNode();
        markup.set("inline_keyboard", keyboard);
        
        sendMessage(chatId, sb.toString(), bot.getAccessToken(), markup);
    }

       private void sendCheckoutInstructions(long chatId, Bot bot) {
        // Устанавливаем флаг, что пользователь в процессе оформления
        setCheckoutProcess(chatId, bot.getBotIdentifier(), true);

        String message = "📦 *Оформление заказа*\n\n" +
                        "Отправьте ваши контактные данные в формате:\n\n" +
                        "*Адрес:* ваш адрес доставки\n" +
                        "*Телефон:* ваш номер\n" +
                        "*Комментарий:* (если есть)\n\n" +
                        "Пример:\n" +
                        "_Адрес: ул. Абая 10, кв 5\n" +
                        "Телефон: +77001234567\n" +
                        "Комментарий: Позвоните за 10 минут_";
        sendMessage(chatId, message, bot.getAccessToken(), createMainMenuKeyboard());
    }

    // Вспомогательные методы для отслеживания процесса оформления
    private Map<String, Boolean> checkoutProcessMap = new HashMap<>();

    private String getCheckoutKey(long chatId, String botIdentifier) {
        return botIdentifier + "_" + chatId;
    }

    private boolean isCheckoutProcess(long chatId, String botIdentifier) {
        return checkoutProcessMap.getOrDefault(getCheckoutKey(chatId, botIdentifier), false);
    }

    private void setCheckoutProcess(long chatId, String botIdentifier, boolean inProcess) {
        checkoutProcessMap.put(getCheckoutKey(chatId, botIdentifier), inProcess);
    }

    private void handleCheckoutInput(String botIdentifier, long chatId, String text) {
        Bot bot = botRepository.findByBotIdentifier(botIdentifier)
                .orElseThrow(() -> new RuntimeException("Бот не найден"));
        User clientUser = getOrCreateClientUser(chatId);

        // 1. Парсинг данных
        String address = extractValue(text, "Адрес");
        String phone = extractValue(text, "Телефон");
        String comment = extractValue(text, "Комментарий");

        if (address == null || phone == null) {
            sendMessage(chatId, "Не удалось распознать адрес или телефон. Пожалуйста, убедитесь, что вы отправили данные в правильном формате (Адрес: ..., Телефон: ..., Комментарий: ...).",
                    bot.getAccessToken(), createMainMenuKeyboard());
            return;
        }

        try {
            // 2. Создание заказа
            CreateOrderRequest orderRequest = new CreateOrderRequest();
            orderRequest.setBotId(bot.getId());
            orderRequest.setClientDeliveryAddress(address);
            orderRequest.setClientContactPhone(phone);
            orderRequest.setClientComment(comment);

            OrderResponse orderResponse = orderService.createOrderFromCart(clientUser.getId(), orderRequest);

            // 3. Создание OrderConfirmation (если нужна оплата)
            // В текущей логике OrderService.createOrderFromCart создает заказ и отправляет уведомление владельцу.
            // Если нужна оплата, то после создания заказа нужно предложить оплату.
            // Предположим, что оплата происходит после создания заказа, и мы должны предложить ее.

            // 4. Отправка инструкций по оплате (если настроено)
            // Здесь мы должны вызвать логику, которая предложит оплату и, возможно, создаст OrderConfirmation
            // Однако, по задаче, уведомление владельцу должно прийти сразу после оформления (т.е. после createOrderFromCart).
            // В OrderService.createOrderFromCart уже есть вызов notificationService.sendNewOrderNotification(savedOrder);
            // Это соответствует требованию "Отправить уведомление владельцу".

            // 5. Отправка подтверждения клиенту
            String confirmationMessage = String.format(
                    "🎉 *Ваш заказ №%d оформлен!* 🎉\n\n" +
                    "Мы получили ваши данные:\n" +
                    "Адрес: %s\n" +
                    "Телефон: %s\n" +
                    "Комментарий: %s\n\n" +
	                    "Общая сумма: *%s тг*\n\n" +
	                    "Владелец бота уже получил уведомление и скоро свяжется с вами для подтверждения и оплаты.",
	                    orderResponse.getId(),
	                    address,
	                    phone,
	                    comment != null ? comment : "Нет",
	                    orderResponse.getTotalAmount()
	            );
	            
	            // 6. Отправка QR-кода, если он настроен
	            if (bot.getPaymentQrCodeUrl() != null && !bot.getPaymentQrCodeUrl().isEmpty()) {
	                sendPhoto(chatId, bot.getPaymentQrCodeUrl(), 
	                          "Для оплаты используйте этот QR-код. После оплаты отправьте скриншот в чат.", 
	                          bot.getAccessToken(), createMainMenuKeyboard());
	            } else {
	                sendMessage(chatId, confirmationMessage, bot.getAccessToken(), createMainMenuKeyboard());
	            }

        } catch (Exception e) {
            log.error("Ошибка при создании заказа для чата {}: {}", chatId, e.getMessage());
            sendMessage(chatId, "Произошла ошибка при оформлении заказа. Попробуйте еще раз или свяжитесь с нами.",
                    bot.getAccessToken(), createMainMenuKeyboard());
        } finally {
            // Сбрасываем флаг процесса оформления
            setCheckoutProcess(chatId, botIdentifier, false);
        }
    }

    private String extractValue(String text, String key) {
        Pattern pattern = Pattern.compile(key + ":\\s*(.*?)(?:\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
    

    private void sendContactDetails(long chatId, Bot bot) {
        DeliveryDetailsResponse details = deliveryDetailsService.getDeliveryDetailsByBotId(bot.getId());
        
        if (details == null || details.getContactPhone() == null) {
            sendMessage(chatId, "К сожалению, контактные данные пока не указаны. " +
                       "Попробуйте связаться с нами через чат.", 
                       bot.getAccessToken(), createMainMenuKeyboard());
            return;
        }
        
        StringBuilder sb = new StringBuilder("📞 *Контактная информация*\n\n");
        
        if (details.getContactPhone() != null) {
            sb.append("☎️ Телефон: ").append(details.getContactPhone()).append("\n\n");
        }
        if (details.getWhatsappLink() != null) {
            sb.append("💬 WhatsApp: ").append(details.getWhatsappLink()).append("\n\n");
        }
        if (details.getOtherSocialMediaLink() != null) {
            sb.append("📱 Соцсети: ").append(details.getOtherSocialMediaLink()).append("\n\n");
        }
        if (details.getPickupAddress() != null) {
            sb.append("📍 Адрес самовывоза:\n").append(details.getPickupAddress()).append("\n\n");
        }
        if (details.getAdditionalInfo() != null) {
            sb.append("ℹ️ Дополнительно:\n").append(details.getAdditionalInfo());
        }
        
        sendMessage(chatId, sb.toString(), bot.getAccessToken(), createMainMenuKeyboard());
    }

    private void sendOpenAIResponse(String botIdentifier, long chatId, String userMessage) {
        Optional<Bot> botOptional = botRepository.findByBotIdentifier(botIdentifier);
        if (botOptional.isEmpty()) {
            sendMessage(chatId, "Бот с таким идентификатором не найден.", defaultBotToken, null);
            return;
        }
        Bot bot = botOptional.get();

        List<ChatMessage> history = chatMessageRepository.findTop30ByChatIdAndBotIdentifierOrderByIdDesc(chatId, botIdentifier);

        List<String[]> chatHistory = history.stream()
                .map(m -> new String[]{m.getRole(), m.getContent()})
                .collect(Collectors.toList());

        chatHistory.add(new String[]{"user", userMessage});

        String productCatalogInfo = buildProductCatalogInfo(bot);

        String aiResponse = openAIService.getBotResponseWithImageSupport(
            chatHistory, productCatalogInfo, bot.getShopName(), botIdentifier, chatId
        );

        ChatMessage userMsg = ChatMessage.builder()
                .chatId(chatId)
                .botIdentifier(botIdentifier)
                .role("user")
                .content(userMessage)
                .build();

        ChatMessage aiMsg = ChatMessage.builder()
                .chatId(chatId)
                .botIdentifier(botIdentifier)
                .role("assistant")
                .content(aiResponse)
                .build();

        chatMessageRepository.save(userMsg);
        chatMessageRepository.save(aiMsg);

        sendMessage(chatId, aiResponse, bot.getAccessToken(), createMainMenuKeyboard());

        List<Product> mentionedProducts = extractMentionedProducts(aiResponse, bot);
        if (!mentionedProducts.isEmpty()) {
            for (Product product : mentionedProducts) {
                sendProductCard(chatId, product, bot.getAccessToken());
            }
        }
    }

    private String buildProductCatalogInfo(Bot bot) {
        return productRepository.findByBot(bot).stream()
                .collect(Collectors.groupingBy(Product::getCatalog))
                .entrySet().stream()
                .map(entry -> {
                    String catalog = entry.getKey();
                    return "Каталог: " + catalog + "\n" +
                            entry.getValue().stream()
                                    .collect(Collectors.groupingBy(Product::getSubcategory))
                                    .entrySet().stream()
                                    .map(subEntry -> {
                                        String subcategory = subEntry.getKey();
                                        String products = subEntry.getValue().stream()
                                                .map(p -> String.format(
                                                    "- %s (%s тг): %s%s",
                                                    p.getName(),
                                                    p.getPrice(),
                                                    p.getDescription() != null ? p.getDescription() : "",
                                                    p.getImageUrl() != null ? " [ФОТО]" : ""
                                                ))
                                                .collect(Collectors.joining("\n"));
                                        return "  Подкаталог: " + subcategory + "\n" + products;
                                    }).collect(Collectors.joining("\n"));
                }).collect(Collectors.joining("\n\n"));
    }

    private List<Product> extractMentionedProducts(String aiResponse, Bot bot) {
        List<Product> allProducts = productRepository.findByBot(bot);
        List<Product> mentioned = new ArrayList<>();
        
        for (Product product : allProducts) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(product.getName()) + "\\b", 
                                            Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(aiResponse);
            
            if (matcher.find()) {
                mentioned.add(product);
            }
        }
        
        return mentioned.stream().limit(3).collect(Collectors.toList());
    }

    public void sendMessage(long chatId, String text, String botToken, ObjectNode replyMarkup) {
        String url = String.format("/bot%s/sendMessage", botToken);
        
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);
        requestBody.put("parse_mode", "Markdown");
        
        if (replyMarkup != null) {
            requestBody.set("reply_markup", replyMarkup);
        }

        telegramWebClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(WebClientResponseException.class, e -> 
                    log.error("Ошибка при отправке сообщения в Telegram: {} - {}", 
                             e.getStatusCode(), e.getResponseBodyAsString()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    public void sendPhoto(long chatId, String photoUrl, String caption, String botToken, ObjectNode replyMarkup) {
        String url = String.format("/bot%s/sendPhoto", botToken);

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("chat_id", chatId);
        requestBody.put("photo", photoUrl);
        requestBody.put("caption", caption);
        requestBody.put("parse_mode", "Markdown");
        
        if (replyMarkup != null) {
            requestBody.set("reply_markup", replyMarkup);
        }

        telegramWebClient.post()
                .uri(url)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .doOnError(WebClientResponseException.class, e -> 
                    log.error("Ошибка при отправке фото в Telegram: {} - {}", 
                             e.getStatusCode(), e.getResponseBodyAsString()))
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }
}