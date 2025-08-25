package com.chatalyst.backend.Repository;

// src/main/java/com/chatalyst/backend/repository/ChatMessageRepository.java

import com.chatalyst.backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    /**
     * Retrieves the top 25 chat messages for a specific chat and bot, ordered by ID in descending order.
     * This is useful for fetching the most recent messages.
     * @param chatId The ID of the chat.
     * @param botIdentifier The identifier of the bot.
     * @return A list of the top 25 ChatMessage objects.
     */
    List<ChatMessage> findTop30ByChatIdAndBotIdentifierOrderByIdDesc(Long chatId, String botIdentifier);

    /**
     * Counts the total number of messages for a specific bot.
     * @param botIdentifier The identifier of the bot.
     * @return The total number of messages.
     */
    long countByBotIdentifier(String botIdentifier);

    /**
     * Counts the number of unique chats (dialogues) for a specific bot.
     * @param botIdentifier The identifier of the bot.
     * @return The number of unique chats.
     */
    @Query("SELECT COUNT(DISTINCT m.chatId) FROM ChatMessage m WHERE m.botIdentifier = :botIdentifier")
    long countDistinctChatIdsByBotIdentifier(@Param("botIdentifier") String botIdentifier);
}