package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Repository.OpenAITokenUsageRepository;
import com.chatalyst.backend.dto.TokenUsageStatsDTO;
import com.chatalyst.backend.model.OpenAITokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenUsageService {

    private final OpenAITokenUsageRepository tokenUsageRepository;

    public TokenUsageStatsDTO getBotTokenUsageStats(String botIdentifier) {
        List<OpenAITokenUsage> usages = tokenUsageRepository.findByBotIdentifier(botIdentifier);

        long totalRequests = usages.size();
        long totalPromptTokens = usages.stream().mapToLong(OpenAITokenUsage::getPromptTokens).sum();
        long totalCompletionTokens = usages.stream().mapToLong(OpenAITokenUsage::getCompletionTokens).sum();
        double totalUsdCost = usages.stream().mapToDouble(OpenAITokenUsage::getUsdCost).sum();
        double totalKztCost = usages.stream().mapToDouble(OpenAITokenUsage::getKztCost).sum();

        return new TokenUsageStatsDTO(botIdentifier, totalRequests, totalPromptTokens, totalCompletionTokens, totalUsdCost, totalKztCost);
    }
}