package com.chatalyst.backend.KaspiPayment.Service;

import com.chatalyst.backend.Entity.Role;
import com.chatalyst.backend.Entity.RoleName;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.KaspiPayment.dto.SubscriptionDetailsDTO;
import com.chatalyst.backend.Repository.RoleRepository;
import com.chatalyst.backend.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Определяет соответствующее имя роли на основе типа подписки и ее продолжительности.
     * @param type Тип подписки (STANDARD или PREMIUM)
     * @param durationInMonths Продолжительность в месяцах (1, 3, 6, 12)
     * @return Соответствующее имя роли из RoleName
     */
    private RoleName getRoleNameForSubscription(String type, int durationInMonths) {
        String roleNameStr = "ROLE_" + type.toUpperCase() + "_" + durationInMonths + "M";
        try {
            return RoleName.valueOf(roleNameStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Неверная комбинация типа подписки и продолжительности: " + type + ", " + durationInMonths + " месяцев");
        }
    }

    /**
     * Удаляет все роли подписки (STANDARD_XM, PREMIUM_XM, AFTERMONTH_DISCOUNT) у пользователя.
     * @param user Пользователь, чьи роли нужно очистить
     */
    private void clearSubscriptionRoles(User user) {
        Set<Role> nonSubscriptionRoles = user.getRoles().stream()
                .filter(role -> {
                    String name = role.getName().name();
                    return !name.startsWith("ROLE_STANDARD_") &&
                           !name.startsWith("ROLE_PREMIUM_") &&
                           role.getName() != RoleName.ROLE_AFTERMONTH_DISCOUNT;
                })
                .collect(Collectors.toSet());
        user.setRoles(nonSubscriptionRoles);
    }

    @Transactional
    public User assignSubscription(Long userId, String subscriptionType, int durationInMonths) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с ID: " + userId));

        // ВАЖНО: Запретить изменение подписки для админов
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        
        if (isAdmin) {
            throw new RuntimeException("Нельзя изменять подписку для администраторов");
        }

        // 1. Очистка существующих ролей подписки
        clearSubscriptionRoles(user);
        Set<Role> updatedRoles = new HashSet<>(user.getRoles());

        // 2. Назначение новой подписки
        if ("STANDARD".equalsIgnoreCase(subscriptionType) || "PREMIUM".equalsIgnoreCase(subscriptionType)) {
            
            RoleName newRoleName = getRoleNameForSubscription(subscriptionType, durationInMonths);
            Role newSubscriptionRole = roleRepository.findByName(newRoleName)
                    .orElseThrow(() -> new RuntimeException("Роль " + newRoleName.name() + " не найдена"));
            
            updatedRoles.add(newSubscriptionRole);

            // Обновление лимитов (упрощенно, в реальном проекте лучше брать из сущности Subscription)
            int botsAllowed = "PREMIUM".equalsIgnoreCase(subscriptionType) ? 3 : 1;
            int messagesLimit = "PREMIUM".equalsIgnoreCase(subscriptionType) ? 15000 : 5000;
            
            user.setBotsAllowed(botsAllowed);
            user.setMonthlyMessagesLimit(messagesLimit);
            user.setSubscriptionStart(LocalDateTime.now());
            user.setSubscriptionEnd(LocalDateTime.now().plusMonths(durationInMonths));
            user.setSupportLevel(subscriptionType.toUpperCase());

            // 3. Логика для роли скидки после пробного месяца
            // Если пользователь покупает подписку на 1 месяц, и это его первая подписка,
            // или если его предыдущая подписка закончилась, и он не имеет роли скидки,
            // мы можем считать, что он завершил "пробный" период (если он был) и отметить это.
            // Однако, по заданию, "Скидка на подписку пользователям после пробного-1 месяца".
            // Будем считать, что если пользователь покупает любую подписку, он завершает "пробный" период.
            // Для простоты, роль AFTERMONTH_DISCOUNT назначается, если пользователь уже имел подписку.
            
            // Если пользователь уже имел подписку (т.е. это не первая покупка)
            // В реальном проекте, здесь должна быть более сложная логика проверки,
            // но для выполнения требования "Скидка на подписку пользователям после пробного-1 месяца"
            // мы будем использовать флаг isTrialCompleted.
            
            if (user.getIsTrialCompleted() != null && user.getIsTrialCompleted()) {
                Role afterMonthRole = roleRepository.findByName(RoleName.ROLE_AFTERMONTH_DISCOUNT)
                        .orElseThrow(() -> new RuntimeException("Роль ROLE_AFTERMONTH_DISCOUNT не найдена"));
                updatedRoles.add(afterMonthRole);
            }
            
            // Устанавливаем флаг, что пользователь завершил пробный период (т.е. купил подписку)
            user.setIsTrialCompleted(true);

        } else if ("USER".equalsIgnoreCase(subscriptionType)) {
            // No subscription - just basic user
            user.setBotsAllowed(0);
            user.setMonthlyMessagesLimit(0);
            user.setSubscriptionStart(null);
            user.setSubscriptionEnd(null);
            user.setSupportLevel("USER");
            
            // Сохраняем флаг isTrialCompleted и роль ROLE_AFTERMONTH_DISCOUNT, если они были
            if (user.getIsTrialCompleted() != null && user.getIsTrialCompleted()) {
                Role afterMonthRole = roleRepository.findByName(RoleName.ROLE_AFTERMONTH_DISCOUNT)
                        .orElseThrow(() -> new RuntimeException("Роль ROLE_AFTERMONTH_DISCOUNT не найдена"));
                updatedRoles.add(afterMonthRole);
            }
            
        } else {
            throw new RuntimeException("Неверный тип подписки: " + subscriptionType);
        }

        user.setRoles(updatedRoles);
        userRepository.save(user);
        return user;
    }

    // Вспомогательный метод для определения текущей роли подписки пользователя
    private RoleName getCurrentSubscriptionRoleName(User user) {
        return user.getRoles().stream()
                .map(Role::getName)
                .filter(name -> name.name().startsWith("ROLE_STANDARD_") || name.name().startsWith("ROLE_PREMIUM_"))
                .findFirst()
                .orElse(RoleName.ROLE_USER);
    }

    public SubscriptionDetailsDTO getSubscriptionDetailsForUser(User user) {
        int botsAllowed = 0;
        String supportLevel = "USER";
        
        // 1. ADMIN - высший приоритет
        if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
            botsAllowed = 50;
            supportLevel = "ADMIN";
        } else {
            // 2. Роли подписки
            RoleName currentRole = getCurrentSubscriptionRoleName(user);
            
            if (currentRole.name().startsWith("ROLE_PREMIUM_")) {
                botsAllowed = 3;
                supportLevel = "PREMIUM";
            } else if (currentRole.name().startsWith("ROLE_STANDARD_")) {
                botsAllowed = 1;
                supportLevel = "STANDARD";
            }
            
            // 3. Проверка на роль скидки
            boolean hasDiscountRole = user.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.ROLE_AFTERMONTH_DISCOUNT);
            
            // В реальном проекте, здесь можно добавить логику, которая будет влиять на отображение
            // информации о скидке в DTO, но поскольку DTO не содержит поля для скидки,
            // мы просто убедимся, что логика назначения роли скидки работает в assignSubscription.
        }
        
        // Переопределяем из базы, если явно установлено
        if (user.getBotsAllowed() != null && user.getBotsAllowed() > 0) {
            botsAllowed = user.getBotsAllowed();
        }
        
        if (user.getSupportLevel() != null && !user.getSupportLevel().isEmpty()) {
            supportLevel = user.getSupportLevel();
        }

        return new SubscriptionDetailsDTO(botsAllowed, supportLevel);
    }
}
