package com.chatalyst.backend.Support.dto;

import com.chatalyst.backend.Support.Entity.MessageStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMessageStatusRequest {
    
    @NotNull(message = "Status is required")
    private MessageStatus status;
}

