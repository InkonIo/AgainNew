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

        // Preserve existing roles that are not subscription-related (e.g., ADMIN)
        Set<Role> updatedRoles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Роль USER не найдена"));
        updatedRoles.add(userRole); // Ensure ROLE_USER is always present by default

        // Add ADMIN role if user already has it
        user.getRoles().stream()
                .filter(r -> r.getName() == RoleName.ROLE_ADMIN)
                .forEach(updatedRoles::add);

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
        } else if ("NONE".equalsIgnoreCase(subscriptionType)) {
            // No specific subscription role to add, just keep ROLE_USER and ADMIN if present
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

        // Set supportLevel based on subscriptionType
        user.setSupportLevel(subscriptionType);

        userRepository.save(user);
        return user;
    }

    public SubscriptionDetailsDTO getSubscriptionDetailsForUser(User user) {
        int botsAllowed = 0;
        String supportLevel = "NONE";

        // 1. Check for highest subscription role (PREMIUM)
        if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_PREMIUM)) {
            botsAllowed = 3;
            supportLevel = "PREMIUM";
        // 2. Check for STANDARD subscription role
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_STANDARD)) {
            botsAllowed = 1;
            supportLevel = "STANDARD";
        // 3. Check for ADMIN role (should not be considered a subscription level, but kept for logic consistency)
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
            botsAllowed = 999; // Example: unlimited bots for admin
            supportLevel = "ADMIN";
        } else if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_USER)) {
            // 4. Default to standard user if no subscription/admin role is found
            supportLevel = "USER";
            botsAllowed = 0; // Default for ROLE_USER without subscription
        }
        
        // Overwrite with user-specific settings if available and greater than 0 (for botsAllowed)
        if (user.getBotsAllowed() != null && user.getBotsAllowed() > 0) {
            botsAllowed = user.getBotsAllowed();
        }
        // Overwrite supportLevel if explicitly set on User entity (e.g., from assignSubscription)
        if (user.getSupportLevel() != null && !user.getSupportLevel().isEmpty()) {
            supportLevel = user.getSupportLevel();
        }

        return new SubscriptionDetailsDTO(botsAllowed, supportLevel);
    }
}

