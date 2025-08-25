package com.chatalyst.backend.Support.dto;

import com.chatalyst.backend.Support.Entity.SupportMessage;
import com.chatalyst.backend.Support.Entity.MessageStatus;
import com.chatalyst.backend.Support.Entity.MessagePriority;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageResponse {
    
    private Long id;
    private String subject;
    private String message;
    private MessageStatus status;
    private MessagePriority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User info
    private Long userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    
    // Admin info (if assigned)
    private Long adminId;
    private String adminEmail;
    private String adminFirstName;
    private String adminLastName;
    
    // Reply count
    private int replyCount;
    
    public static SupportMessageResponse fromEntity(SupportMessage message) {
        SupportMessageResponse response = new SupportMessageResponse();
        response.setId(message.getId());
        response.setSubject(message.getSubject());
        response.setMessage(message.getMessage());
        response.setStatus(message.getStatus());
        response.setPriority(message.getPriority());
        response.setCreatedAt(message.getCreatedAt());
        response.setUpdatedAt(message.getUpdatedAt());
        
        // User info
        if (message.getUser() != null) {
            response.setUserId(message.getUser().getId());
            response.setUserEmail(message.getUser().getEmail());
            response.setUserFirstName(message.getUser().getFirstName());
            response.setUserLastName(message.getUser().getLastName());
        }
        
        // Admin info
        if (message.getAdmin() != null) {
            response.setAdminId(message.getAdmin().getId());
            response.setAdminEmail(message.getAdmin().getEmail());
            response.setAdminFirstName(message.getAdmin().getFirstName());
            response.setAdminLastName(message.getAdmin().getLastName());
        }
        
        // Reply count
        response.setReplyCount(message.getReplies().size());
        
        return response;
    }
}
