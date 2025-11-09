package com.chatalyst.backend.Repository;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.model.CartItem;
import com.chatalyst.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // Найти все элементы корзины для конкретного пользователя и бота
    List<CartItem> findByUserAndBot(User user, Bot bot);

    // Найти конкретный элемент корзины по пользователю, боту и товару
    Optional<CartItem> findByUserAndBotAndProduct(User user, Bot bot, Product product);

    // Удалить все элементы корзины для конкретного пользователя и бота
    void deleteByUserAndBot(User user, Bot bot);
}
