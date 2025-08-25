// Bot.java (обновлён)
package com.chatalyst.backend.model;

import com.chatalyst.backend.Entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "bots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String botIdentifier;

    @Column(nullable = false)
    private String platform;

    @Column(nullable = false, length = 255)
    private String accessToken;

    @Column(nullable = false, unique = true)
    private Long telegramBotApiId;

    @Column(nullable = true)
    private String shopName;

    @JsonIgnore // Добавлена эта аннотация для игнорирования поля при сериализации
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 1000)
    private String description;

    public Bot(String name, String botIdentifier, String platform, String accessToken,
               Long telegramBotApiId, String shopName, User owner, String description) {
        this.name = name;
        this.botIdentifier = botIdentifier;
        this.platform = platform;
        this.accessToken = accessToken;
        this.telegramBotApiId = telegramBotApiId;
        this.shopName = shopName;
        this.owner = owner;
        this.description = description;
    }
}