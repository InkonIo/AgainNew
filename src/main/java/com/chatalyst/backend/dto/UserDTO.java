package com.chatalyst.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for admin view
    private Long totalMessages;
    private LocalDateTime lastLoginAt;
    private String profilePicture;
    
    // Subscription fields - ДОБАВЛЕНО
    private String supportLevel;
    private Integer botsAllowed;
    private Integer monthlyMessagesLimit;
    private Integer monthlyMessagesUsed;
    private LocalDateTime subscriptionStart;
    private LocalDateTime subscriptionEnd;
}