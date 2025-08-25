package com.chatalyst.backend.Support.dto;

import com.chatalyst.backend.Support.Entity.SupportMessage;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDetailResponse {
    
    private SupportMessageResponse message;
    private List<SupportMessageReplyResponse> replies;
    
    public static MessageDetailResponse fromEntity(SupportMessage message) {
        MessageDetailResponse response = new MessageDetailResponse();
        response.setMessage(SupportMessageResponse.fromEntity(message));
        
        List<SupportMessageReplyResponse> replyResponses = message.getReplies()
            .stream()
            .map(SupportMessageReplyResponse::fromEntity)
            .collect(Collectors.toList());
        response.setReplies(replyResponses);
        
        return response;
    }
}
