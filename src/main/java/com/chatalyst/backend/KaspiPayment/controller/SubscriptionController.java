package com.chatalyst.backend.KaspiPayment.controller;

import com.chatalyst.backend.KaspiPayment.dto.SubscriptionRequestDTO;
import com.chatalyst.backend.KaspiPayment.dto.SubscriptionDetailsDTO;
import com.chatalyst.backend.KaspiPayment.Service.SubscriptionService;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.security.services.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    // Только админ может вызывать
    @PostMapping("/admin/subscription/assign")
    public ResponseEntity<User> assignSubscription(@RequestBody SubscriptionRequestDTO request) {
        User updatedUser = subscriptionService.assignSubscription(request.getUserId(), request.getSubscriptionType());
        return ResponseEntity.ok(updatedUser);
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
}
