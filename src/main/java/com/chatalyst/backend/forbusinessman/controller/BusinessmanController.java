package com.chatalyst.backend.forbusinessman.controller;

import com.chatalyst.backend.forbusinessman.dto.*;
import com.chatalyst.backend.forbusinessman.service.BusinessmanService;
import com.chatalyst.backend.security.jwt.JwtUtils; // ← ИЗМЕНЕНО!
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/businessman")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BusinessmanController {

    private final BusinessmanService businessmanService;
    private final JwtUtils jwtUtils; // ← ИЗМЕНЕНО с JwtService на JwtUtils!

    // ==================== PaymentInfo Endpoints ====================

    @PostMapping("/payment-info")
    public ResponseEntity<PaymentInfoResponse> createOrUpdatePaymentInfo(
            @Valid @RequestBody PaymentInfoRequest request,
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        PaymentInfoResponse response = businessmanService.createOrUpdatePaymentInfo(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/payment-info/{botId}/upload-qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPaymentQr(
            @PathVariable Long botId,
            @RequestParam("paymentSystem") String paymentSystem,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        String qrUrl = businessmanService.uploadPaymentQr(botId, paymentSystem, file, userId);
        return ResponseEntity.ok(Map.of("qrUrl", qrUrl));
    }

    @GetMapping("/payment-info/{botId}")
    public ResponseEntity<PaymentInfoResponse> getPaymentInfo(
            @PathVariable Long botId,
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        PaymentInfoResponse response = businessmanService.getPaymentInfo(botId, userId);
        
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }

    // ==================== OrderConfirmation Endpoints ====================

    @GetMapping("/confirmations/{botId}")
    public ResponseEntity<Page<OrderConfirmationResponse>> getConfirmations(
            @PathVariable Long botId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        Page<OrderConfirmationResponse> confirmations = businessmanService.getConfirmations(
                botId, userId, page, size
        );
        return ResponseEntity.ok(confirmations);
    }

    @GetMapping("/confirmations/pending")
    public ResponseEntity<List<OrderConfirmationResponse>> getPendingConfirmations(
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        List<OrderConfirmationResponse> confirmations = businessmanService.getPendingConfirmations(userId);
        return ResponseEntity.ok(confirmations);
    }

    @GetMapping("/confirmations/pending/count")
    public ResponseEntity<Map<String, Long>> countPendingConfirmations(
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        long count = businessmanService.countPendingConfirmations(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/confirmations/{confirmationId}/review")
    public ResponseEntity<OrderConfirmationResponse> reviewConfirmation(
            @PathVariable Long confirmationId,
            @Valid @RequestBody ReviewConfirmationRequest request,
            Authentication authentication
    ) {
        Long userId = jwtUtils.extractUserIdFromAuth(authentication);
        OrderConfirmationResponse response = businessmanService.reviewConfirmation(
                confirmationId, request, userId
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Exception Handling ====================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        log.error("Ошибка в BusinessmanController: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}