package com.chatalyst.backend.KaspiPayment.controller;

import com.chatalyst.backend.KaspiPayment.dto.SubscriptionRequestDTO;
import com.chatalyst.backend.KaspiPayment.dto.SubscriptionDetailsDTO;
import com.chatalyst.backend.KaspiPayment.Service.SubscriptionService;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Entity.Role;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.dto.UserDTO;
import com.chatalyst.backend.security.services.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    // Только админ может вызывать
    @PostMapping("/admin/subscription/assign")
    public ResponseEntity<UserDTO> assignSubscription(@RequestBody SubscriptionRequestDTO request) {
        User updatedUser = subscriptionService.assignSubscription(request.getUserId(), request.getSubscriptionType(), request.getDurationInMonths());
        
        // Конвертируем User в UserDTO
        UserDTO userDTO = convertToDTO(updatedUser);
        
        return ResponseEntity.ok(userDTO);
    }

    // Получение подписки текущего пользователя
    @GetMapping("/user/subscription-details")
    public ResponseEntity<SubscriptionDetailsDTO> getUserSubscriptionDetails(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionDetailsDTO details = subscriptionService.getSubscriptionDetailsForUser(user);
        return ResponseEntity.ok(details);
    }
    
    // Метод конвертации User в UserDTO
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setActive(user.getActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        // Subscription fields
        dto.setSupportLevel(user.getSupportLevel() != null ? user.getSupportLevel() : "NONE");
        dto.setBotsAllowed(user.getBotsAllowed() != null ? user.getBotsAllowed() : 0);
        dto.setMonthlyMessagesLimit(user.getMonthlyMessagesLimit() != null ? user.getMonthlyMessagesLimit() : 0);
        dto.setMonthlyMessagesUsed(user.getMonthlyMessagesUsed() != null ? user.getMonthlyMessagesUsed() : 0);
        dto.setSubscriptionStart(user.getSubscriptionStart());
        dto.setSubscriptionEnd(user.getSubscriptionEnd());
        
        // Determine primary role
        String primaryRoleName = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .max(Comparator.comparing((String roleName) -> {
                    if (roleName.contains("ADMIN")) return 4;
                    if (roleName.contains("PREMIUM")) return 3;
                    if (roleName.contains("STANDARD")) return 2;
                    return 1; // ROLE_USER
                }))
                .orElse("ROLE_USER");

        dto.setRole(primaryRoleName.replace("ROLE_", ""));
        
        return dto;
    }
}