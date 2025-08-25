package com.chatalyst.backend.Support.dto;

import com.chatalyst.backend.Support.Entity.SupportMessageReply;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportMessageReplyResponse {
    
    private Long id;
    private String replyText;
    private Boolean isAdminReply;
    private LocalDateTime createdAt;
    
    // Sender info
    private Long senderId;
    private String senderEmail;
    private String senderFirstName;
    private String senderLastName;
    
    public static SupportMessageReplyResponse fromEntity(SupportMessageReply reply) {
        SupportMessageReplyResponse response = new SupportMessageReplyResponse();
        response.setId(reply.getId());
        response.setReplyText(reply.getReplyText());
        response.setIsAdminReply(reply.getIsAdminReply());
        response.setCreatedAt(reply.getCreatedAt());
        
        // Sender info
        if (reply.getSender() != null) {
            response.setSenderId(reply.getSender().getId());
            response.setSenderEmail(reply.getSender().getEmail());
            response.setSenderFirstName(reply.getSender().getFirstName());
            response.setSenderLastName(reply.getSender().getLastName());
        }
        
        return response;
    }
}

