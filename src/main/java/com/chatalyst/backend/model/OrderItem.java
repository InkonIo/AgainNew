package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ссылка на Заказ
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Ссылка на Товар (если товар удалят, информация останется)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true) // nullable=true, если товар может быть удален
    private Product product;

    // Сохраняем данные о товаре на момент заказа для исторической точности
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal; // price * quantity

    // Конструктор для удобства
    public OrderItem(Order order, Product product, Integer quantity, BigDecimal price) {
        this.order = order;
        this.product = product;
        this.productName = product.getName();
        this.price = price;
        this.quantity = quantity;
        this.subtotal = price.multiply(BigDecimal.valueOf(quantity));
    }
}
