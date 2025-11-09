package com.chatalyst.backend.forbusinessman.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewConfirmationRequest {
    
    @NotBlank(message = "Статус обязателен")
    @Pattern(regexp = "APPROVED|REJECTED", message = "Статус должен быть APPROVED или REJECTED")
    private String status;
    
    private String ownerResponse;
}