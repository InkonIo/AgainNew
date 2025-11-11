package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.*;
import com.chatalyst.backend.dto.CreateOrderRequest;
import com.chatalyst.backend.dto.OrderResponse;
import com.chatalyst.backend.dto.OrderItemResponse;
import com.chatalyst.backend.model.*;
import com.chatalyst.backend.Support.service.SupportMessageService; // Импортируем новый сервис
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepository;
    private final NotificationService notificationService; // Для отправки уведомлений
    private final SupportMessageService supportMessageService; // Для создания сообщения поддержки

    /**
     * Создает новый заказ из содержимого корзины пользователя.
     * @param userId ID пользователя (клиента Telegram).
     * @param request DTO с деталями доставки от клиента.
     * @return Созданный объект OrderResponse.
     */
    @Transactional
    public OrderResponse createOrderFromCart(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден."));
        Bot bot = botRepository.findById(request.getBotId())
                .orElseThrow(() -> new RuntimeException("Бот не найден."));

        // 1. Получаем элементы корзины
        List<CartItem> cartItems = cartItemRepository.findByUserAndBot(user, bot);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Корзина пуста. Невозможно создать заказ.");
        }

        // 2. Создаем новый заказ
        Order order = new Order();
        order.setUser(user);
        order.setBot(bot);
        order.setClientDeliveryAddress(request.getClientDeliveryAddress());
        order.setClientContactPhone(request.getClientContactPhone());
        order.setClientComment(request.getClientComment());
        
        // 3. Переносим элементы из корзины в элементы заказа
        List<OrderItem> orderItems = cartItems.stream().map(cartItem -> 
            new OrderItem(
                order, 
                cartItem.getProduct(), 
                cartItem.getQuantity(), 
                cartItem.getPriceAtTime()
            )
        ).collect(Collectors.toList());

        // Считаем общую сумму заказа
        BigDecimal totalAmount = orderItems.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);
        
        // 4. Сохраняем заказ и его элементы
        Order savedOrder = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems); // Сохранение элементов заказа

        // 5. Очищаем корзину
        cartItemRepository.deleteByUserAndBot(user, bot);

        // 6. Отправляем уведомление владельцу бота
        notificationService.sendNewOrderNotification(savedOrder);

        // 7. Создаем сообщение поддержки для владельца бота
        supportMessageService.createOrderMessage(savedOrder);

        return convertToResponse(savedOrder);
    }

    /**
     * Получает список заказов для владельца бота.
     * @param botId ID бота.
     * @param userId ID пользователя (владельца бота).
     * @param page Номер страницы.
     * @param size Размер страницы.
     * @return Страница с OrderResponse.
     */
    public Page<OrderResponse> getOrdersByBot(Long botId, Long userId, int page, int size) {
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден."));
        
        // Проверка прав
        if (!bot.getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для просмотра заказов этого бота.");
        }

        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByBotOrderByCreatedAtDesc(bot, pageable)
                .map(this::convertToResponse);
    }

    /**
     * Обновляет статус заказа.
     * @param orderId ID заказа.
     * @param newStatus Новый статус.
     * @param userId ID пользователя (владельца бота).
     * @return Обновленный OrderResponse.
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, Order.OrderStatus newStatus, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден."));

        // Проверка прав
        if (!order.getBot().getOwner().getId().equals(userId)) {
            throw new RuntimeException("У вас нет прав для изменения этого заказа.");
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        
        // Опционально: отправить уведомление клиенту об изменении статуса
        // notificationService.sendOrderStatusUpdate(updatedOrder);

        return convertToResponse(updatedOrder);
    }

    // ========================================================================
    // Вспомогательные методы
    // ========================================================================

    private OrderResponse convertToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setBotId(order.getBot().getId());
        response.setUserId(order.getUser().getId());
        response.setUserEmail(order.getUser().getEmail()); // Предполагаем, что у User есть email
        response.setClientDeliveryAddress(order.getClientDeliveryAddress());
        response.setClientContactPhone(order.getClientContactPhone());
        response.setClientComment(order.getClientComment());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        
        List<OrderItemResponse> itemResponses = order.getItems().stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
        response.setItems(itemResponses);
        
        return response;
    }

    private OrderItemResponse convertToResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        response.setProductName(item.getProductName());
        response.setPrice(item.getPrice());
        response.setQuantity(item.getQuantity());
        response.setSubtotal(item.getSubtotal());
        return response;
    }
}
