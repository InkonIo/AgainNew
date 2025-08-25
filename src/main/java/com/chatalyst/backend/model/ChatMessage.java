// src/main/java/com/chatalyst/backend/model/ChatMessage.java
package com.chatalyst.backend.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Модель для хранения сообщений чата в базе данных.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Роль отправителя: "user" или "assistant"
    private String role;

    // Содержимое сообщения. Используем TEXT для больших сообщений.
    @Column(columnDefinition = "TEXT")
    private String content;

    // Идентификатор чата Telegram.
    private Long chatId;

    // Идентификатор бота, которому принадлежит сообщение.
    private String botIdentifier;
}