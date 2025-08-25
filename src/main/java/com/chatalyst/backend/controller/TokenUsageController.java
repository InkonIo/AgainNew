package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.TokenUsageStatsDTO;
import com.chatalyst.backend.security.services.TokenUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/token-usage")
@RequiredArgsConstructor
public class TokenUsageController {

    private final TokenUsageService tokenUsageService;

    @GetMapping("/stats/{botIdentifier}")
    public ResponseEntity<TokenUsageStatsDTO> getBotStats(@PathVariable String botIdentifier) {
        TokenUsageStatsDTO stats = tokenUsageService.getBotTokenUsageStats(botIdentifier);
        return ResponseEntity.ok(stats);
    }
}
