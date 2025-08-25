// src/main/java/com/chatalyst/backend/model/Product.java
package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal; // Для точного хранения цен

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2) // Цена с двумя знаками после запятой
    private BigDecimal price;

    @Column(length = 1000) // Увеличиваем длину для описания
    private String description;

    @Column(name = "catalog")
    private String catalog;

    @Column(name = "subcategory")
    private String subcategory;


    @Column
    private String imageUrl; // URL к изображению товара

    // Исправлено: добавлена колонка со значением по умолчанию 'true'
    // Это предотвратит ошибку, когда DDL пытается добавить NOT NULL колонку
    // в таблицу с уже существующими данными.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean inStock = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot; // Бот, которому принадлежит этот товар
}
