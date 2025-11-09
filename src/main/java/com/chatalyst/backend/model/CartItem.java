package com.chatalyst.backend.model;

import com.chatalyst.backend.Entity.User; // Assuming User is in Entity package based on Bot.java import
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"}) // Один товар в корзине пользователя
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Пользователь (клиент Telegram), которому принадлежит корзина
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Товар, который добавлен в корзину
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Количество товара
    @Column(nullable = false)
    private Integer quantity;

    // Цена товара на момент добавления в корзину (для истории, хотя можно брать из Product)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtTime;
    
    // Бот, через которого происходит продажа (для удобства)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;
}
