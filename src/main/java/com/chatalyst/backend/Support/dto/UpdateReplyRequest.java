package com.chatalyst.backend.Support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateReplyRequest {
    
    @NotBlank(message = "Reply text is required")
    @Size(max = 5000, message = "Reply text must not exceed 5000 characters")
    private String replyText;
}

