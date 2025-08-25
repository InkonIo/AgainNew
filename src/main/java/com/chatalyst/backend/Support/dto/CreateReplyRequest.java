package com.chatalyst.backend.Support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReplyRequest {
    
    @NotBlank(message = "Reply text cannot be empty")
    @Size(max = 5000, message = "Reply must not exceed 5000 characters")
    private String replyText;
}
