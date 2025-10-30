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

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public User assignSubscription(Long userId, String subscriptionType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден с ID: " + userId));

        // ВАЖНО: Запретить изменение подписки для админов
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        
        if (isAdmin) {
            throw new RuntimeException("Нельзя изменять подписку для администраторов");
        }

        // Preserve existing roles that are not subscription-related (e.g., ADMIN)
        Set<Role> updatedRoles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Роль USER не найдена"));
        updatedRoles.add(userRole);

        Role newSubscriptionRole = null;

        if ("STANDARD".equalsIgnoreCase(subscriptionType)) {
            newSubscriptionRole = roleRepository.findByName(RoleName.ROLE_STANDARD)
                    .orElseThrow(() -> new RuntimeException("Роль STANDARD не найдена"));
            user.setBotsAllowed(1);
            user.setMonthlyMessagesLimit(5000);
            user.setSubscriptionStart(LocalDateTime.now());
            user.setSubscriptionEnd(LocalDateTime.now().plusMonths(1));
        } else if ("PREMIUM".equalsIgnoreCase(subscriptionType)) {
            newSubscriptionRole = roleRepository.findByName(RoleName.ROLE_PREMIUM)
                    .orElseThrow(() -> new RuntimeException("Роль PREMIUM не найдена"));
            user.setBotsAllowed(3);
            user.setMonthlyMessagesLimit(15000);
            user.setSubscriptionStart(LocalDateTime.now());
            user.setSubscriptionEnd(LocalDateTime.now().plusMonths(2));
        } else if ("NONE".equalsIgnoreCase(subscriptionType) || "USER".equalsIgnoreCase(subscriptionType)) {
            // No subscription - just basic user
            user.setBotsAllowed(0);
            user.setMonthlyMessagesLimit(0);
            user.setSubscriptionStart(null);
            user.setSubscriptionEnd(null);
        } else {
            throw new RuntimeException("Неверный тип подписки: " + subscriptionType);
        }

        if (newSubscriptionRole != null) {
            updatedRoles.add(newSubscriptionRole);
        }

        user.setRoles(updatedRoles);

        // Set supportLevel - используем "USER" вместо "NONE"
        if ("NONE".equalsIgnoreCase(subscriptionType)) {
            user.setSupportLevel("USER");
        } else {
            user.setSupportLevel(subscriptionType);
        }

        userRepository.save(user);
        return user;
    }

    public SubscriptionDetailsDTO getSubscriptionDetailsForUser(User user) {
        int botsAllowed = 0;
        String supportLevel = "USER";

        // 1. ADMIN - высший приоритет, 50 ботов
        if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
            botsAllowed = 50;
            supportLevel = "ADMIN";
        }
        // 2. PREMIUM
        else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_PREMIUM)) {
            botsAllowed = 3;
            supportLevel = "PREMIUM";
        }
        // 3. STANDARD
        else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_STANDARD)) {
            botsAllowed = 1;
            supportLevel = "STANDARD";
        }
        // 4. USER (базовый уровень)
        else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_USER)) {
            supportLevel = "USER";
            botsAllowed = 0;
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