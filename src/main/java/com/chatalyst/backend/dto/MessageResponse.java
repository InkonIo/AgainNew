package com.chatalyst.backend.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "API response message")
public class MessageResponse {
    
    @Schema(description = "Response message")
    private String message;
    
    public MessageResponse(String message) {
        this.message = message;
    }
}