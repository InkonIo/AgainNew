package com.chatalyst.backend.StripePayment.service;

import com.chatalyst.backend.StripePayment.exception.QuotaExceededException;
import com.chatalyst.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuotaService {
    
    private final UserRepository userRepository;

    /**
     * Пытается списать указанное количество сообщений из лимита пользователя.
     * Операция атомарная на уровне БД.
     * 
     * @param userId ID пользователя
     * @param units количество сообщений для списания
     * @throws QuotaExceededException если лимит исчерпан или тариф не активен
     */
    @Transactional
    public void consumeOrThrow(Long userId, int units) {
        log.debug("Попытка списать {} сообщений для пользователя {}", units, userId);
        
        int updated = userRepository.tryConsumeMessages(userId, units);
        
        if (updated != 1) {
            log.warn("Не удалось списать {} сообщений для пользователя {}. Лимит исчерпан или тариф не активен.", units, userId);
            throw new QuotaExceededException("Лимит сообщений исчерпан или тариф не активен.");
        }
        
        log.debug("Успешно списано {} сообщений для пользователя {}", units, userId);
    }

    /**
     * Проверяет, может ли пользователь создать бота
     * 
     * @param userId ID пользователя
     * @return true если можно создать бота
     */
    public boolean canCreateBot(Long userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        long currentBots = userRepository.countBotsByOwner(userId);
        return currentBots < user.getBotsAllowed();
    }

    /**
     * Получает информацию о текущем использовании лимитов пользователя
     * 
     * @param userId ID пользователя
     * @return информация о лимитах
     */
    public UserQuotaInfo getQuotaInfo(Long userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        return UserQuotaInfo.builder()
            .monthlyMessagesLimit(user.getMonthlyMessagesLimit())
            .monthlyMessagesUsed(user.getMonthlyMessagesUsed())
            .botsAllowed(user.getBotsAllowed())
            .botsCreated(userRepository.countBotsByOwner(userId))
            .supportLevel(user.getSupportLevel())
            .subscriptionStart(user.getSubscriptionStart())
            .subscriptionEnd(user.getSubscriptionEnd())
            .build();
    }

    public static class UserQuotaInfo {
        private final Integer monthlyMessagesLimit;
        private final Integer monthlyMessagesUsed;
        private final Integer botsAllowed;
        private final Long botsCreated;
        private final String supportLevel;
        private final java.time.LocalDateTime subscriptionStart;
        private final java.time.LocalDateTime subscriptionEnd;

        private UserQuotaInfo(Builder builder) {
            this.monthlyMessagesLimit = builder.monthlyMessagesLimit;
            this.monthlyMessagesUsed = builder.monthlyMessagesUsed;
            this.botsAllowed = builder.botsAllowed;
            this.botsCreated = builder.botsCreated;
            this.supportLevel = builder.supportLevel;
            this.subscriptionStart = builder.subscriptionStart;
            this.subscriptionEnd = builder.subscriptionEnd;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Integer monthlyMessagesLimit;
            private Integer monthlyMessagesUsed;
            private Integer botsAllowed;
            private Long botsCreated;
            private String supportLevel;
            private java.time.LocalDateTime subscriptionStart;
            private java.time.LocalDateTime subscriptionEnd;

            public Builder monthlyMessagesLimit(Integer monthlyMessagesLimit) {
                this.monthlyMessagesLimit = monthlyMessagesLimit;
                return this;
            }

            public Builder monthlyMessagesUsed(Integer monthlyMessagesUsed) {
                this.monthlyMessagesUsed = monthlyMessagesUsed;
                return this;
            }

            public Builder botsAllowed(Integer botsAllowed) {
                this.botsAllowed = botsAllowed;
                return this;
            }

            public Builder botsCreated(Long botsCreated) {
                this.botsCreated = botsCreated;
                return this;
            }

            public Builder supportLevel(String supportLevel) {
                this.supportLevel = supportLevel;
                return this;
            }

            public Builder subscriptionStart(java.time.LocalDateTime subscriptionStart) {
                this.subscriptionStart = subscriptionStart;
                return this;
            }

            public Builder subscriptionEnd(java.time.LocalDateTime subscriptionEnd) {
                this.subscriptionEnd = subscriptionEnd;
                return this;
            }

            public UserQuotaInfo build() {
                return new UserQuotaInfo(this);
            }
        }

        // Getters
        public Integer getMonthlyMessagesLimit() { return monthlyMessagesLimit; }
        public Integer getMonthlyMessagesUsed() { return monthlyMessagesUsed; }
        public Integer getBotsAllowed() { return botsAllowed; }
        public Long getBotsCreated() { return botsCreated; }
        public String getSupportLevel() { return supportLevel; }
        public java.time.LocalDateTime getSubscriptionStart() { return subscriptionStart; }
        public java.time.LocalDateTime getSubscriptionEnd() { return subscriptionEnd; }
    }
}
