package com.chatalyst.backend.StripePayment.service;

import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Сервис для автопродления тарифов
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalService {

    private final UserRepository userRepository;

    /**
     * Плановое задание для обновления тарифов каждый день в 00:00 (Asia/Almaty)
     * Для MVP: сбрасываем счетчик сообщений и продлеваем тариф на месяц
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Almaty")
    @Transactional
    public void renewSubscriptions() {
        log.info("Запуск планового обновления тарифов");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Находим пользователей с истекшими тарифами
        List<User> expiredUsers = userRepository.findAll().stream()
            .filter(user -> user.getSubscriptionEnd() != null && 
                           user.getSubscriptionEnd().isBefore(now))
            .toList();
        
        log.info("Найдено {} пользователей с истекшими тарифами", expiredUsers.size());
        
        for (User user : expiredUsers) {
            try {
                // Для MVP: просто продлеваем тариф на месяц и сбрасываем счетчик
                if ("BASIC".equals(user.getSupportLevel())) {
                    user.setMonthlyMessagesUsed(0);
                    user.setSubscriptionStart(now);
                    user.setSubscriptionEnd(now.plusMonths(1));
                    userRepository.save(user);
                    log.info("Тариф BASIC продлен для пользователя {}", user.getEmail());
                } else if ("PREMIUM".equals(user.getSupportLevel())) {
                    user.setMonthlyMessagesUsed(0);
                    user.setSubscriptionStart(now);
                    user.setSubscriptionEnd(now.plusMonths(1));
                    userRepository.save(user);
                    log.info("Тариф PREMIUM продлен для пользователя {}", user.getEmail());
                }
                // Пользователи с NONE тарифом не продлеваются
            } catch (Exception e) {
                log.error("Ошибка при продлении тарифа для пользователя {}: {}", user.getEmail(), e.getMessage());
            }
        }
        
        log.info("Плановое обновление тарифов завершено");
    }

    /**
     * Ручное обновление тарифа для тестирования
     * 
     * @param userId ID пользователя
     * @return true если тариф обновлен
     */
    @Transactional
    public boolean manuallyRenewSubscription(Long userId) {
        log.info("Ручное обновление тарифа для пользователя {}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        
        if ("NONE".equals(user.getSupportLevel())) {
            log.warn("Пользователь {} не имеет активного тарифа для продления", userId);
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        user.setMonthlyMessagesUsed(0);
        user.setSubscriptionStart(now);
        user.setSubscriptionEnd(now.plusMonths(1));
        userRepository.save(user);
        
        log.info("Тариф {} успешно продлен для пользователя {}", user.getSupportLevel(), userId);
        return true;
    }
}
