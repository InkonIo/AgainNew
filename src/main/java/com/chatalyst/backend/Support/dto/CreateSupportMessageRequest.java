package com.chatalyst.backend.Support.dto;

import com.chatalyst.backend.Support.Entity.MessagePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSupportMessageRequest {
    
    @NotBlank(message = "Subject cannot be empty")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;
    
    @NotBlank(message = "Message cannot be empty")
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    private String message;
    
    private MessagePriority priority = MessagePriority.MEDIUM;
}
