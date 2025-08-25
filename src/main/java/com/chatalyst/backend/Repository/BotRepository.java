package com.chatalyst.backend.Repository;

import com.chatalyst.backend.model.Bot;
import com.chatalyst.backend.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {
    // Найти бота по его уникальному идентификатору (username в Telegram)
    Optional<Bot> findByBotIdentifier(String botIdentifier);

    // Найти всех ботов, принадлежащих определенному пользователю
    List<Bot> findByOwner(User owner);

    // Найти бота по токену доступа (для вебхуков, чтобы определить, какому боту пришло сообщение)
    Optional<Bot> findByAccessToken(String accessToken);

    // Новый метод: Найти бота по его Telegram API ID
    Optional<Bot> findByTelegramBotApiId(Long telegramBotApiId);
}
