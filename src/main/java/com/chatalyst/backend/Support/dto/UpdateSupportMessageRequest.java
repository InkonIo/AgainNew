package com.chatalyst.backend.Support.dto;

import com.chatalyst.backend.Support.Entity.MessagePriority;
import com.chatalyst.backend.Support.Entity.MessageStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSupportMessageRequest {
    
    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;
    
    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message must not exceed 5000 characters")
    private String message;
    
    @NotNull(message = "Priority is required")
    private MessagePriority priority;

    private MessageStatus status;
}

