package com.chatalyst.backend.forbusinessman.controller;

import com.chatalyst.backend.security.jwt.JwtUtils;
import com.chatalyst.backend.security.services.BotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BotManagementController {

    private final BotService botService;
    private final JwtUtils jwtUtils;

    @PostMapping(value = "/{botId}/upload-qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadBotPaymentQr(
            @PathVariable Long botId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        String qrUrl = botService.uploadPaymentQr(botId, userId, file);
        return ResponseEntity.ok(Map.of("qrUrl", qrUrl));
    }
}
