package com.chatalyst.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenUsageStatsDTO {
    private String botIdentifier;
    private Long totalRequests;
    private Long totalPromptTokens;
    private Long totalCompletionTokens;
    private Double totalUsdCost;
    private Double totalKztCost;
}