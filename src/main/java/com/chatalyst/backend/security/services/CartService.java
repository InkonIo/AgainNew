package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.CartItemRepository;
import com.chatalyst.backend.Repository.ProductRepository;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.dto.CartItemResponse;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.CartItem;
import com.chatalyst.backend.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final BotService botService; // Предполагаем, что BotService существует

    /**
     * Добавляет товар в корзину или увеличивает его количество.
     * @param userId ID пользователя (клиента Telegram).
     * @param botId ID бота.
     * @param productId ID товара.
     * @param quantity Количество для добавления.
     * @return Обновленный элемент корзины.
     */
    @Transactional
    public CartItemResponse addProductToCart(Long userId, Long botId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден."));
        Bot bot = botService.getBotById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден.")); // Используем метод из BotService для получения бота
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден."));
        
        if (!product.getBot().getId().equals(botId)) {
            throw new RuntimeException("Товар не принадлежит этому боту.");
        }

        CartItem cartItem = cartItemRepository.findByUserAndBotAndProduct(user, bot, product)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUser(user);
                    newItem.setBot(bot);
                    newItem.setProduct(product);
                    newItem.setPriceAtTime(product.getPrice());
                    newItem.setQuantity(0);
                    return newItem;
                });
        
        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        CartItem savedItem = cartItemRepository.save(cartItem);
        
        return convertToResponse(savedItem);
    }

    /**
     * Удаляет товар из корзины.
     * @param userId ID пользователя.
     * @param botId ID бота.
     * @param productId ID товара для удаления.
     */
    @Transactional
    public void removeProductFromCart(Long userId, Long botId, Long productId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден."));
        Bot bot = botService.getBotById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден."));
        
        CartItem cartItem = cartItemRepository.findByUserAndBotAndProduct(user, bot, product)
                .orElseThrow(() -> new RuntimeException("Товар не найден в корзине."));
        
        cartItemRepository.delete(cartItem);
    }

    /**
     * Получает содержимое корзины пользователя.
     * @param userId ID пользователя.
     * @param botId ID бота.
     * @return Список элементов корзины.
     */
    public List<CartItemResponse> getCartItems(Long userId, Long botId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден."));
        Bot bot = botService.getBotById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден."));
        
        return cartItemRepository.findByUserAndBot(user, bot).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Очищает корзину пользователя.
     * @param userId ID пользователя.
     * @param botId ID бота.
     */
    @Transactional
    public void clearCart(Long userId, Long botId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден."));
        Bot bot = botService.getBotById(botId)
                .orElseThrow(() -> new RuntimeException("Бот не найден."));
        
        cartItemRepository.deleteByUserAndBot(user, bot);
    }

    /**
     * Вспомогательный метод для преобразования CartItem в CartItemResponse.
     */
    private CartItemResponse convertToResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setPrice(item.getPriceAtTime());
        response.setQuantity(item.getQuantity());
        response.setSubtotal(item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())));
        return response;
    }
}
