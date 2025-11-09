package com.chatalyst.backend.forbusinessman.service;

import com.chatalyst.backend.Entity.Notification;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.NotificationRepository;
import com.chatalyst.backend.Repository.OrderRepository;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.forbusinessman.dto.*;
import com.chatalyst.backend.forbusinessman.model.OrderConfirmation;
import com.chatalyst.backend.forbusinessman.model.PaymentInfo;
import com.chatalyst.backend.forbusinessman.repository.*;

import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.Order;
import com.chatalyst.backend.Repository.BotRepository;
import com.chatalyst.backend.security.services.PsObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessmanService {

    private final PaymentInfoRepository paymentInfoRepository;
    private final OrderConfirmationRepository orderConfirmationRepository;
    private final BotRepository botRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final PsObjectStorageService psObjectStorageService;

    // ==================== PaymentInfo Methods ====================

    /**
     * Создать или обновить информацию об оплате для бота
     */
    @Transactional
    public PaymentInfoResponse createOrUpdatePaymentInfo(PaymentInfoRequest request, Long userId) {
        Bot bot = botRepository.findById(request.getBotId())
                .orElseThrow(() -> new RuntimeException("Бот не найден"));

        // Проверка прав
        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для изменения настроек этого бота");
        }

        PaymentInfo paymentInfo = paymentInfoRepository.findByBot(bot)
                .orElseGet(() -> {
                    PaymentInfo newInfo = new PaymentInfo();
                    newInfo.setBot(bot);
                    return newInfo;
                });

        // Обновляем поля
        paymentInfo.setKaspiQrUrl(request.getKaspiQrUrl());
        paymentInfo.setHalykQrUrl(request.getHalykQrUrl());
        paymentInfo.setOtherQrUrl(request.getOtherQrUrl());
        paymentInfo.setBankAccount(request.getBankAccount());
        paymentInfo.setCardNumber(request.getCardNumber());
        paymentInfo.setOwnerTelegramUsername(request.getOwnerTelegramUsername());
        paymentInfo.setOwnerTelegramChatId(request.getOwnerTelegramChatId());
        paymentInfo.setOwnerPhone(request.getOwnerPhone());
        paymentInfo.setOwnerEmail(request.getOwnerEmail());
        paymentInfo.setPaymentInstructions(request.getPaymentInstructions());
        paymentInfo.setPaymentEnabled(request.getPaymentEnabled());

        PaymentInfo saved = paymentInfoRepository.save(paymentInfo);
        log.info("PaymentInfo сохранена для бота ID: {}", bot.getId());

        return convertToPaymentInfoResponse(saved);
    }

    /**
     * Загрузить QR-код для оплаты
     */
    @Transactional
    public String uploadPaymentQr(Long botId, String paymentSystem, MultipartFile file, Long userId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден"));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для изменения настроек этого бота");
        }

        // Загружаем изображение
        String qrUrl = psObjectStorageService.uploadImage(file, "payment_qr_" + paymentSystem);

        // Обновляем PaymentInfo
        PaymentInfo paymentInfo = paymentInfoRepository.findByBot(bot)
                .orElseGet(() -> {
                    PaymentInfo newInfo = new PaymentInfo();
                    newInfo.setBot(bot);
                    return newInfo;
                });

        switch (paymentSystem.toLowerCase()) {
            case "kaspi" -> paymentInfo.setKaspiQrUrl(qrUrl);
            case "halyk" -> paymentInfo.setHalykQrUrl(qrUrl);
            default -> paymentInfo.setOtherQrUrl(qrUrl);
        }

        paymentInfoRepository.save(paymentInfo);
        log.info("QR-код загружен для {} бота ID: {}", paymentSystem, botId);

        return qrUrl;
    }

    /**
     * Получить информацию об оплате для бота (с проверкой прав)
     */
    public PaymentInfoResponse getPaymentInfo(Long botId, Long userId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден"));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для просмотра настроек этого бота");
        }

        PaymentInfo paymentInfo = paymentInfoRepository.findByBot(bot)
                .orElse(null);

        return paymentInfo != null ? convertToPaymentInfoResponse(paymentInfo) : null;
    }

    /**
     * Получить информацию об оплате для бота (публичный метод для Telegram бота)
     */
    public PaymentInfoResponse getPaymentInfoPublic(Long botId) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден"));

        PaymentInfo paymentInfo = paymentInfoRepository.findByBot(bot)
                .orElseThrow(() -> new RuntimeException("Информация об оплате не настроена"));

        if (!Boolean.TRUE.equals(paymentInfo.getPaymentEnabled())) {
            throw new RuntimeException("Оплата отключена для этого бота");
        }

        return convertToPaymentInfoResponse(paymentInfo);
    }

    // ==================== OrderConfirmation Methods ====================

    /**
     * Создать подтверждение оплаты заказа (вызывается из Telegram бота)
     */
    @Transactional
    public OrderConfirmationResponse createOrderConfirmation(
            Long orderId,
            Long clientUserId,
            Long clientTelegramChatId,
            String clientTelegramUsername,
            String clientMessage,
            MultipartFile paymentScreenshot
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        User clientUser = userRepository.findById(clientUserId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем, нет ли уже подтверждения для этого заказа
        if (orderConfirmationRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException("Подтверждение для этого заказа уже существует");
        }

        // Загружаем скриншот оплаты
        String screenshotUrl = null;
        if (paymentScreenshot != null && !paymentScreenshot.isEmpty()) {
            screenshotUrl = psObjectStorageService.uploadImage(
                    paymentScreenshot,
                    "payment_confirmation_" + orderId
            );
        }

        // Создаем подтверждение
        OrderConfirmation confirmation = OrderConfirmation.builder()
                .order(order)
                .bot(order.getBot())
                .clientUser(clientUser)
                .clientTelegramChatId(clientTelegramChatId)
                .clientTelegramUsername(clientTelegramUsername)
                .paymentScreenshotUrl(screenshotUrl)
                .clientMessage(clientMessage)
                .status(OrderConfirmation.ConfirmationStatus.PENDING)
                .build();

        OrderConfirmation saved = orderConfirmationRepository.save(confirmation);
        log.info("Создано подтверждение оплаты для заказа ID: {}", orderId);

        // Отправляем уведомление владельцу бота
        sendOrderNotificationToOwner(saved);

        return convertToConfirmationResponse(saved);
    }

    /**
     * Получить все подтверждения для владельца бота
     */
    public Page<OrderConfirmationResponse> getConfirmations(Long botId, Long userId, int page, int size) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден"));

        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для просмотра подтверждений этого бота");
        }

        Pageable pageable = PageRequest.of(page, size);
        return orderConfirmationRepository.findByBotOrderByCreatedAtDesc(bot, pageable)
                .map(this::convertToConfirmationResponse);
    }

    /**
     * Получить ожидающие подтверждения для владельца
     */
    public List<OrderConfirmationResponse> getPendingConfirmations(Long userId) {
        return orderConfirmationRepository.findPendingConfirmationsByOwner(userId)
                .stream()
                .map(this::convertToConfirmationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Подсчитать количество ожидающих подтверждений
     */
    public long countPendingConfirmations(Long userId) {
        return orderConfirmationRepository.countPendingByOwner(userId);
    }

    /**
     * Одобрить или отклонить подтверждение
     */
    @Transactional
    public OrderConfirmationResponse reviewConfirmation(
            Long confirmationId,
            ReviewConfirmationRequest request,
            Long userId
    ) {
        OrderConfirmation confirmation = orderConfirmationRepository.findById(confirmationId)
                .orElseThrow(() -> new RuntimeException("Подтверждение не найдено"));

        // Проверка прав
        if (!confirmation.getBot().getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для проверки этого подтверждения");
        }

        // Обновляем статус
        OrderConfirmation.ConfirmationStatus newStatus = OrderConfirmation.ConfirmationStatus.valueOf(
                request.getStatus().toUpperCase()
        );
        confirmation.setStatus(newStatus);
        confirmation.setOwnerResponse(request.getOwnerResponse());
        confirmation.setReviewedAt(LocalDateTime.now());

        // Обновляем статус заказа
        Order order = confirmation.getOrder();
        if (newStatus == OrderConfirmation.ConfirmationStatus.APPROVED) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
        } else if (newStatus == OrderConfirmation.ConfirmationStatus.REJECTED) {
            order.setStatus(Order.OrderStatus.CANCELLED);
        }
        orderRepository.save(order);

        OrderConfirmation saved = orderConfirmationRepository.save(confirmation);
        log.info("Подтверждение ID {} обработано со статусом: {}", confirmationId, newStatus);

        // TODO: Отправить уведомление клиенту в Telegram

        return convertToConfirmationResponse(saved);
    }

    // ==================== Helper Methods ====================

    private void sendOrderNotificationToOwner(OrderConfirmation confirmation) {
        User owner = confirmation.getBot().getOwner();
        Order order = confirmation.getOrder();

        String orderItemsText = order.getItems().stream()
                .map(item -> String.format("%s × %d = %s тг",
                        item.getProductName(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .collect(Collectors.joining("\n"));

        String message = String.format("""
                🆕 Новый заказ требует подтверждения!
                
                🏪 Магазин: %s
                📦 Заказ #%d
                
                👤 Клиент:
                Telegram: @%s (ID: %d)
                Телефон: %s
                
                📍 Адрес доставки:
                %s
                
                🛒️ Товары:
                %s
                
                💰 Итого: %s тг
                
                💬 Комментарий клиента:
                %s
                
                📸 Скриншот оплаты: %s
                """,
                confirmation.getBot().getShopName(),
                order.getId(),
                confirmation.getClientTelegramUsername(),
                confirmation.getClientTelegramChatId(),
                order.getClientContactPhone(),
                order.getClientDeliveryAddress(),
                orderItemsText,
                order.getTotalAmount(),
                confirmation.getClientMessage() != null ? confirmation.getClientMessage() : "—",
                confirmation.getPaymentScreenshotUrl() != null ? "Прикреплен" : "Не прикреплен"
        );

        Notification notification = Notification.builder()
                .type("order_confirmation")
                .title("Новый заказ ожидает подтверждения")
                .message(message)
                .user(owner)
                .priority("high")
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Уведомление отправлено владельцу ID: {}", owner.getId());
    }

    private PaymentInfoResponse convertToPaymentInfoResponse(PaymentInfo info) {
        return PaymentInfoResponse.builder()
                .id(info.getId())
                .botId(info.getBot().getId())
                .kaspiQrUrl(info.getKaspiQrUrl())
                .halykQrUrl(info.getHalykQrUrl())
                .otherQrUrl(info.getOtherQrUrl())
                .bankAccount(info.getBankAccount())
                .cardNumber(info.getCardNumber())
                .ownerTelegramUsername(info.getOwnerTelegramUsername())
                .ownerTelegramChatId(info.getOwnerTelegramChatId())
                .ownerPhone(info.getOwnerPhone())
                .ownerEmail(info.getOwnerEmail())
                .paymentInstructions(info.getPaymentInstructions())
                .paymentEnabled(info.getPaymentEnabled())
                .createdAt(info.getCreatedAt())
                .updatedAt(info.getUpdatedAt())
                .build();
    }

    private OrderConfirmationResponse convertToConfirmationResponse(OrderConfirmation confirmation) {
        Order order = confirmation.getOrder();

        String orderItemsText = order.getItems().stream()
                .map(item -> String.format("%s × %d = %s тг",
                        item.getProductName(),
                        item.getQuantity(),
                        item.getSubtotal()))
                .collect(Collectors.joining(", "));

        return OrderConfirmationResponse.builder()
                .id(confirmation.getId())
                .orderId(order.getId())
                .botId(confirmation.getBot().getId())
                .botName(confirmation.getBot().getName())
                .clientUserId(confirmation.getClientUser().getId())
                .clientTelegramChatId(confirmation.getClientTelegramChatId())
                .clientTelegramUsername(confirmation.getClientTelegramUsername())
                .deliveryAddress(order.getClientDeliveryAddress())
                .contactPhone(order.getClientContactPhone())
                .orderComment(order.getClientComment())
                .totalAmount(order.getTotalAmount().toString())
                .orderItems(orderItemsText)
                .paymentScreenshotUrl(confirmation.getPaymentScreenshotUrl())
                .clientMessage(confirmation.getClientMessage())
                .status(confirmation.getStatus().name())
                .ownerResponse(confirmation.getOwnerResponse())
                .createdAt(confirmation.getCreatedAt())
                .reviewedAt(confirmation.getReviewedAt())
                .build();
    }
}