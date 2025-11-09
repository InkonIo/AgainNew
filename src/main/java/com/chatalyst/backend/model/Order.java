package com.chatalyst.backend.model;

import com.chatalyst.backend.Entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    public enum OrderStatus {
        NEW,          // Новый заказ, ожидает обработки
        PENDING,      // В процессе (например, ожидание оплаты или подтверждения)
        CONFIRMED,    // Подтвержден владельцем
        SHIPPED,      // Отправлен
        COMPLETED,    // Выполнен (доставлен/выдан)
        CANCELLED     // Отменен
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Пользователь (клиент Telegram), сделавший заказ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Бот, через который сделан заказ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    // Список элементов заказа (позиций)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Детали доставки, предоставленные клиентом (адрес, контакты)
    @Column(length = 500)
    private String clientDeliveryAddress;
    
    @Column(length = 50)
    private String clientContactPhone;
    
    @Column(length = 255)
    private String clientComment;
}
